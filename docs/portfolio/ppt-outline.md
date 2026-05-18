# PPT Outline

## 1. 프로젝트 개요

핵심 메시지:

대용량 결제/취소 데이터를 일자별로 정산하고, 정합성, 안정성, 성능 개선을 함께 검증한 금융 IT 포트폴리오 프로젝트다.

포함 내용:

- 정산 배치 성능 개선 프로젝트
- Payment 100,000건, Merchant 100개
- BASIC_LOOP 기준선
- GROUP_BY_QUERY 개선
- GROUP_BY_BULK_SAVE 1차 저장 개선
- BatchJobHistory 기반 실행 이력

---

## 2. 금융 IT 관점의 문제 정의

핵심 메시지:

금융 정산에서는 빠른 처리만큼 중복 방지, 실패 추적, 결과 정합성이 중요하다.

포함 내용:

- 중복 정산 시 금액 이중 반영 위험
- 실패 시 일부 결과 저장 위험
- 성능 개선 후 금액 불일치 위험
- 성능 측정 조건 불일치 위험

---

## 3. BASIC_LOOP 기준선 구현 이유

핵심 메시지:

BASIC_LOOP는 최적화 방식이 아니라 성능 개선 전 병목을 설명하기 위한 기준선이다.

포함 내용:

- Payment 전체 조회
- Java 반복문 집계
- BigDecimal 계산
- Settlement 개별 저장
- BatchJobHistory elapsedMs 기록

---

## 4. 중복 정산 방지 설계

핵심 메시지:

정산 결과 중복 저장을 막으면서도 전략별 성능 비교가 가능하도록 중복 기준을 조정했다.

포함 내용:

- 기존 문제: 정산일자 단위 차단만으로는 전략 비교 어려움
- 결정: `merchant_id + settlement_date + processing_strategy`
- 같은 날짜 + 다른 전략 저장 허용
- 같은 날짜 + 같은 전략 재실행 차단

---

## 5. 실패 이력 추적 구조

핵심 메시지:

정산 결과는 실패 시 롤백하고, 실패 이력은 별도 트랜잭션으로 보존했다.

포함 내용:

- Settlement 트랜잭션 롤백
- BatchJobHistory `REQUIRES_NEW`
- RUNNING, SUCCESS, FAILED
- errorMessage, elapsedMs, processedCount 기록

---

## 6. GROUP_BY_QUERY 개선

핵심 메시지:

DB가 잘하는 집계는 DB에서 처리하고, 애플리케이션은 집계 결과만 받아 정산 계산을 수행하도록 바꿨다.

포함 내용:

- BASIC_LOOP 병목: Payment 전체 Entity 로딩
- GROUP_BY_QUERY: DB GROUP BY 집계
- Java: 수수료와 최종 정산금액 계산
- 벌크 저장과 인덱스는 다음 단계로 분리

---

## 7. 성능 비교 결과

핵심 메시지:

같은 데이터 조건에서 GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE가 더 짧은 실행 시간을 보였지만, 로컬 측정값은 캐시 영향을 고려해 조건과 한계를 함께 기록했다.

포함 내용:

| 전략 | 데이터 건수 | 실행 시간 |
|---|---:|---:|
| BASIC_LOOP | 100,000 | 728~1043ms |
| GROUP_BY_QUERY | 100,000 | 104~265ms |
| GROUP_BY_BULK_SAVE | 100,000 | 110ms |

주의:

- 로컬 단일 DB 기준
- DB/OS 캐시와 JVM warm-up 영향 가능
- 단일값으로 과장하지 않음
- 대표 측정값은 BASIC_LOOP 882ms, GROUP_BY_QUERY 133ms, GROUP_BY_BULK_SAVE 110ms
- GROUP_BY_BULK_SAVE 110ms는 saveAll 1차 적용 후 동일 조건 1회 측정값
- Merchant 100개 조건이라 Settlement 저장 결과는 100건 수준
- saveAll 기반 저장 최적화 효과는 제한적으로만 확인
- benchmark-medium 100만 건 / Merchant 5,000개 측정값은 BASIC_LOOP 8,253ms, GROUP_BY_QUERY 899ms, GROUP_BY_BULK_SAVE 798ms
- benchmark-medium에서도 전략별 Settlement 5,000건과 합계 금액 동일

---

## 7-1. GROUP_BY_BULK_SAVE 1차 개선

핵심 메시지:

조회 병목을 줄인 뒤 저장 단계의 반복 save를 다음 개선 대상으로 보고, saveAll만 먼저 적용해 정합성을 검증했다.

포함 내용:

- GROUP_BY_QUERY의 DB GROUP BY 집계 재사용
- Settlement 개별 save 반복 제거
- `saveAll` 기반 저장
- 집계 결과 건수 = 생성 Settlement 수 = 저장 Settlement 수
- BASIC_LOOP/GROUP_BY_QUERY/GROUP_BY_BULK_SAVE 금액 동일성 검증
- Hibernate batch_size와 PostgreSQL reWriteBatchedInserts는 다음 단계로 분리

---

## 8. 정합성 검증

핵심 메시지:

성능 개선 후에도 정산 결과 금액이 BASIC_LOOP와 동일한지 확인했다.

포함 내용:

- totalPaymentAmount 동일
- totalCancelAmount 동일
- feeAmount 동일
- finalSettlementAmount 동일
- 저장 건수 동일
- 자동 테스트와 DB 합계 검증

---

## 9. 남은 개선 방향

핵심 메시지:

10만 건 기준에서는 GROUP BY 집계 개선 효과를 확인했고, 100만 건 benchmark-medium에서는 Merchant 수를 5,000개로 늘려 조회 병목과 저장 결과 증가를 함께 확인했다. 1000만 건에서는 covering index를 실험했지만 데이터 분포와 조건 선택도 때문에 기대한 Index Only Scan 효과는 확인하지 못했다.

포함 내용:

- 100만 건 / Merchant 5,000개 중간 확장 실험 완료
- 1000만 건 / Merchant 5,000~10,000개 최종 대용량 실험
- 저장 병목 확인 후 Hibernate batch_size 적용 검토
- batch 설정 후에도 병목이 남을 때 PostgreSQL reWriteBatchedInserts 적용 검토
- 1000만 건 조회 병목 확인 후 GROUP_BY_BULK_INDEX와 EXPLAIN ANALYZE 검토
- covering index 실험 결과: 단일 날짜 1000만 건, 전체 COMPLETED 조건에서는 Parallel Seq Scan 유지
- 인덱스는 무조건 적용하지 않고 데이터 분포와 선택도를 기준으로 판단
- 다음 방향: 날짜 분산 데이터셋 또는 일자·가맹점 단위 사전 집계 테이블 검토
- Payment와 Settlement 대사 기능
- RUNNING 상태 기준 동시 실행 방지
- DB 설정 정리

발표 문장:

10만 건 기준에서는 GROUP BY 집계만으로 충분한 성능 개선을 확인했습니다. 다만 당시 Merchant 수가 100개라 Settlement 저장 결과가 100건 수준이었고, saveAll 기반 저장 최적화의 효과를 충분히 보기 어렵다고 판단했습니다. 그래서 Payment를 100만 건, Merchant를 5,000개로 늘려 조회 병목과 저장 병목을 함께 확인할 수 있도록 조건을 재설계했고, 세 전략의 결과 금액이 동일한지 다시 검증했습니다.

covering index 실험에서는 감으로 인덱스를 추가하지 않고, EXPLAIN에서 확인한 heap read와 전체 스캔 병목을 줄일 수 있는지 가설을 세웠습니다. 하지만 실제 실행계획에서는 단일 날짜와 COMPLETED 상태에 데이터가 몰려 조건 선택도가 낮았고, PostgreSQL은 인덱스보다 Parallel Seq Scan을 선택했습니다. 이 결과를 바탕으로 다음 개선 방향을 날짜 분산 데이터셋 또는 사전 집계 테이블 검토로 수정했습니다.

### Troubleshooting: JPQL count(p)와 Index Only Scan 문제

핵심 메시지:

수동 EXPLAIN에서는 covering index가 `Index Only Scan`으로 동작했지만, API 경로에서는 DB 집계 조회가 3~4초대로 느렸다. Hibernate SQL 로그와 바인딩 파라미터를 확인해 JPQL `count(p)`가 실제 SQL에서 `count(p.id)`로 변환되는 차이를 발견했고, native `count(*)`로 분리해 API 경로에서도 `Index Only Scan`을 확인했다.

포함 내용:

- 수동 EXPLAIN: 약 75~119ms, `Index Only Scan`, `Heap Fetches=0`
- 수동 SQL의 `count(*)`는 `id` 컬럼을 읽지 않아 `Index Only Scan` 가능
- API SQL의 `count(p.id)`는 covering index에 없는 `id`를 요구해 heap 접근 가능
- JPQL constructor projection 쿼리를 native query로 분리
- `count(*)` 명시, interface projection 추가
- API 실제 SQL이 `count(*)`로 실행되는지 확인
- API 경로에서도 `Index Only Scan`, `Heap Fetches=0` 확인
- 정합성: `processedCount=322,581`, Settlement 10,000건, 총액 동일

발표 문장:

처음에는 같은 GROUP BY 집계라고 생각했지만, 수동 EXPLAIN과 API 실행 시간이 크게 달랐습니다. 그래서 API 내부 구간을 나누고 Hibernate가 실제로 만든 SQL을 확인했습니다. 그 결과 JPQL의 `count(p)`가 SQL에서는 `count(p.id)`로 바뀌었고, 제가 만든 covering index에 `id`가 없어서 API 경로에서는 heap 접근이 발생했습니다. native query에서 `count(*)`를 명시하고 interface projection으로 받도록 바꾸자 API 경로에서도 `Index Only Scan`과 `Heap Fetches=0`을 확인했습니다. 이 경험을 통해 ORM을 쓸 때도 최종 성능 판단은 Repository 코드가 아니라 실제 SQL과 EXPLAIN으로 해야 한다는 점을 배웠습니다.

측정/검증:

- EXPLAIN: 2,242.501ms → 74.988ms
- API GROUP_BY_QUERY: 3,021.333ms → 1,627.667ms
- API GROUP_BY_BULK_SAVE: 2,478.333ms → 2,058.000ms
- DB집계조회 GROUP_BY_QUERY: 280.836ms
- DB집계조회 GROUP_BY_BULK_SAVE: 308.998ms

### Troubleshooting: saveAll과 IDENTITY 전략의 batch insert 한계

핵심 메시지:

`saveAll`을 사용했지만 SQL 로그에서는 Settlement 10,000건 insert가 개별 반복으로 실행됐다. `dummy-data.batch-size`와 `benchmark.batch-size`는 Hibernate batch 설정이 아니었고, `Settlement.id`의 IDENTITY 전략도 batch insert에 불리했다. Hibernate batch 설정과 SEQUENCE 전략을 분리 실험해 저장 구간과 API 전체 시간을 개선했다.

포함 내용:

- `saveAll` 호출만으로 DB bulk insert가 보장되지 않음
- SQL 로그: `insert into settlements (...) values (...)` 개별 반복
- multi-row insert 또는 명확한 JDBC batch insert 효과는 확인되지 않음
- Hibernate `jdbc.batch_size=1000`, `order_inserts=true`, `order_updates=true` 실험
- `Settlement.id`: IDENTITY → SEQUENCE
- `settlement_seq`, `allocationSize=1000` 적용
- 기존 row와 sequence 충돌 방지를 위해 `max(id)+1000` 이후로 `setval` 보정
- 정합성: `processedCount=322,581`, Settlement 10,000건, 총액 동일

측정/검증:

| 전략 | ID 전략 | API 전체 평균 |
|---|---|---:|
| GROUP_BY_QUERY | IDENTITY | 1,490.271ms |
| GROUP_BY_QUERY | SEQUENCE | 755.662ms |
| GROUP_BY_BULK_SAVE | IDENTITY | 1,289.364ms |
| GROUP_BY_BULK_SAVE | SEQUENCE | 696.352ms |

발표 문장:

`GROUP_BY_BULK_SAVE`라는 이름만 보면 저장이 bulk로 처리된다고 생각할 수 있지만, SQL 로그를 보니 insert가 10,000건 개별 반복되고 있었습니다. 설정을 확인해 보니 `dummy-data.batch-size`와 `benchmark.batch-size`는 데이터 생성 chunk 설정일 뿐 Hibernate batch insert 설정이 아니었습니다. 또한 Settlement id가 IDENTITY 전략이라 insert 직후 DB generated id를 받아야 했고, 이 구조는 Hibernate batch insert에 불리했습니다. 그래서 Hibernate batch 설정을 먼저 분리 실험한 뒤, SEQUENCE와 `allocationSize=1000`을 적용해 id를 미리 확보할 수 있도록 바꿨습니다. 변경 후 API 전체 시간은 GROUP_BY_QUERY 약 49%, GROUP_BY_BULK_SAVE 약 46% 줄었고, 처리 건수와 총액 동일성도 유지했습니다.

### 오늘의 학습 포인트

핵심 메시지:

성능 개선은 구현 의도나 메서드 이름이 아니라 실제 SQL, 실행계획, 구간별 측정, 정합성 검증으로 판단해야 한다.

포함 내용:

- 기능 구현 후 실제 실행계획과 SQL 로그 확인
- 예상과 다른 성능 결과가 나오면 구간별 시간으로 원인 분리
- ORM이 생성한 SQL과 수동 SQL의 차이 확인
- `saveAll`과 Hibernate batch insert의 차이 확인
- 금융 IT 관점에서 성능과 정합성, 추적 가능성을 함께 검증

### 면접 설명용 요약

인덱스 적용 후 수동 SQL은 빨랐지만 API는 느렸고, 실제 SQL을 확인해 JPQL `count(p)`가 `count(p.id)`로 변환되는 문제를 찾았습니다. native `count(*)`와 interface projection으로 수정해 API에서도 `Index Only Scan`이 사용되도록 했습니다. 이후 처리 건수와 Settlement 건수, 총액 동일성을 확인해 성능 개선 후 정합성이 유지되는지도 검증했습니다. `saveAll`도 실제 bulk insert가 아니라는 점을 SQL 로그로 확인했습니다. IDENTITY 전략이 batch insert에 불리하다는 점을 확인하고 SEQUENCE 전략으로 실험해 저장 시간과 API 전체 시간을 개선했습니다. 이 과정에서 성능 개선은 감이 아니라 실행계획, SQL 로그, 구간별 측정으로 판단해야 한다는 점을 배웠습니다.

---

## 10. 자소서/면접 설명 포인트

핵심 메시지:

이 프로젝트는 기술 사용 경험보다 금융 데이터 처리 리스크를 인식하고 검증한 경험으로 설명한다.

포함 내용:

- 중복 정산 방지
- 실패 이력 추적
- 트랜잭션 롤백
- 성능 개선과 정합성 검증 병행
- 수치 과장 없이 측정 조건 기록
- 인덱스 실험이 기대대로 동작하지 않았을 때 실행계획과 데이터 분포를 근거로 원인 분석
