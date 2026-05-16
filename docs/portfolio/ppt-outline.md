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
