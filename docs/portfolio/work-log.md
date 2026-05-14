# Work Log

## 작업 기록 템플릿

앞으로 기능 구현, 리팩토링, 성능 개선, 테스트 추가, 문서 갱신을 할 때마다 아래 형식으로 기록한다.

```md
## 작업명

### 1. 문제 상황

### 2. 금융 관점에서 중요한 이유

### 3. 판단 기준

### 4. 기술 선택

### 5. 구현 액션

### 6. 검증 방법

### 7. 결과

### 8. 포트폴리오 문장

### 9. 자소서 문장

### 10. 면접 답변

### 11. 예상 질문
```

---

## BASIC_LOOP 기준선 구현

### 1. 문제 상황

성능 개선 프로젝트에서 개선 효과를 설명하려면 먼저 비교 가능한 기준선이 필요했다. 처음부터 최적화된 방식만 구현하면 어떤 병목을 줄였는지 설명하기 어렵다.

### 2. 금융 관점에서 중요한 이유

정산 배치는 금액이 직접 반영되는 업무다. 처리 시간이 빠르더라도 계산 기준이 불명확하면 결과를 신뢰하기 어렵다. 따라서 단순하지만 검증 가능한 기준선을 먼저 만들고, 이후 개선 전략이 같은 결과를 내는지 비교해야 한다.

### 3. 판단 기준

BASIC_LOOP는 실무 최적화 방식이 아니라 기준선 역할로 둔다. 특정 정산일자의 Payment를 조회하고 Java 반복문으로 가맹점별 결제금액, 취소금액, 수수료, 최종 정산금액을 계산한다.

### 4. 기술 선택

- Spring Boot 서비스 계층에서 정산 흐름 관리
- JPA Entity 조회와 Java 반복문 집계
- BigDecimal 기반 금액 계산
- Settlement 저장
- BatchJobHistory 실행 시간 기록

### 5. 구현 액션

- Merchant, Payment, Settlement, BatchJobHistory 도메인 구성
- BASIC_LOOP 정산 processor 구현
- 정산 실행 API 추가
- 정산 결과 조회 API 추가
- 배치 실행 이력 조회 API 추가

### 6. 검증 방법

- 자동 테스트로 정산 계산 결과 확인
- API 실행 후 Settlement 저장 여부 확인
- BatchJobHistory에 처리 건수와 실행 시간 기록 여부 확인

### 7. 결과

BASIC_LOOP는 100,000건 기준 반복 측정에서 728~1043ms 범위로 측정되었다. 이 값은 로컬 단일 DB 기준이며 DB/OS 캐시와 JVM warm-up에 따라 변동될 수 있다.

### 8. 포트폴리오 문장

BASIC_LOOP는 실무 최적화 방식이 아니라 성능 개선 전 기준선으로 설계했습니다. 정산일자의 Payment 전체를 조회해 Java 반복문으로 가맹점별 금액을 계산하고, 이후 GROUP_BY_QUERY가 같은 결과를 내는지 검증하는 비교 기준으로 사용했습니다.

### 9. 자소서 문장

정산 배치 성능 개선을 설명하기 위해 먼저 비교 가능한 기준선을 구현했습니다. BASIC_LOOP 방식은 Payment 전체를 조회해 Java에서 집계하는 단순한 구조지만, 이후 개선 전략의 정확성과 성능 차이를 확인하기 위한 기준이 되었습니다. 금융 정산 업무에서는 빠른 처리보다 결과 신뢰가 우선이라고 판단해 BigDecimal 계산과 결과 검증을 함께 고려했습니다.

### 10. 면접 답변

BASIC_LOOP는 최종 목표가 아니라 기준선입니다. 정산일자의 Payment를 모두 조회하고 Java 반복문으로 가맹점별 금액을 계산하도록 구현했습니다. 이후 GROUP_BY_QUERY를 적용했을 때 처리 시간만 줄어든 것이 아니라 총 결제금액, 총 취소금액, 수수료, 최종 정산금액이 동일한지 검증할 수 있게 했습니다.

### 11. 예상 질문

**Q. 왜 비효율적인 BASIC_LOOP를 먼저 만들었나요?**

A. 성능 개선 효과를 설명하려면 비교 기준이 필요했기 때문입니다. 기준선이 있어야 GROUP BY, 벌크 저장, 인덱스 적용이 각각 어떤 병목을 줄였는지 분리해서 설명할 수 있습니다.

---

## 중복 정산 방지

### 1. 문제 상황

같은 정산일자와 같은 처리 전략이 반복 실행되면 같은 가맹점의 정산 결과가 중복 저장될 수 있다.

### 2. 금융 관점에서 중요한 이유

정산 결과가 중복 저장되면 금액이 이중 반영될 수 있다. 이는 지급 금액 오류, 대사 불일치, 고객사 신뢰 하락으로 이어질 수 있는 금융 IT의 핵심 리스크다.

### 3. 판단 기준

성능 비교를 위해 같은 날짜에 BASIC_LOOP와 GROUP_BY_QUERY 결과는 각각 저장할 수 있어야 한다. 따라서 중복 기준은 단순 정산일자가 아니라 `merchant_id + settlement_date + processing_strategy`가 되어야 한다.

### 4. 기술 선택

- Settlement DB unique constraint
- 서비스 계층의 사전 존재 여부 검사
- 처리 전략 enum 분리

### 5. 구현 액션

- Settlement에 processingStrategy 필드 추가
- `merchant_id + settlement_date + processing_strategy` unique 제약 적용
- 같은 날짜 + 같은 전략 재실행 차단
- 같은 날짜 + 다른 전략 실행 허용

### 6. 검증 방법

- 같은 날짜 + 같은 전략 중복 실행 실패 테스트
- 같은 날짜 + 다른 전략 저장 허용 테스트
- DB unique constraint 위반 테스트

### 7. 결과

같은 날짜의 BASIC_LOOP와 GROUP_BY_QUERY는 각각 저장할 수 있고, 같은 전략을 다시 실행하면 실패하도록 확인했다.

### 8. 포트폴리오 문장

정산 결과가 중복 저장되면 금액이 이중 반영될 수 있다고 판단해 중복 기준을 가맹점, 정산일자, 처리 전략 단위로 설계했습니다. 이를 통해 성능 비교를 위한 전략별 저장은 허용하면서 같은 전략의 중복 정산은 차단했습니다.

### 9. 자소서 문장

정산 배치에서 가장 중요한 리스크 중 하나는 중복 실행으로 인한 금액 중복 반영이라고 판단했습니다. 처음에는 정산일자 단위 차단만 고려했지만, 성능 비교를 위해 같은 날짜에 여러 전략 결과를 저장해야 했습니다. 그래서 중복 기준을 가맹점, 정산일자, 처리 전략 단위로 조정하고 테스트로 검증했습니다.

### 10. 면접 답변

중복 정산은 DB 제약과 서비스 검사를 함께 사용했습니다. 단순히 날짜 하나로 막으면 BASIC_LOOP와 GROUP_BY_QUERY 비교가 불가능하기 때문에, 가맹점과 정산일자와 처리 전략을 unique 기준으로 잡았습니다. 이렇게 하면 같은 전략의 재실행은 막고, 다른 전략의 결과는 비교용으로 저장할 수 있습니다.

### 11. 예상 질문

**Q. 서비스에서만 중복을 막으면 안 되나요?**

A. 서비스 검사는 빠른 예외 응답에는 도움이 되지만 동시 요청에서는 DB 제약이 최종 방어선이 되어야 합니다. 그래서 서비스 검사와 unique constraint를 함께 두는 방향이 적절하다고 봤습니다.

---

## 실패 이력 추적과 트랜잭션 분리

### 1. 문제 상황

정산 중 오류가 발생하면 일부 Settlement만 저장되는 상황을 막아야 한다. 동시에 실패 원인은 사라지지 않고 추적 가능해야 한다.

### 2. 금융 관점에서 중요한 이유

금융 배치는 실패 여부와 원인을 나중에 추적할 수 있어야 한다. 결과 데이터는 불완전하면 안 되지만, 실패 이력까지 롤백되면 운영자가 장애 원인을 확인하기 어렵다.

### 3. 판단 기준

정산 결과 저장과 배치 이력 저장의 트랜잭션 책임을 분리한다. Settlement는 실패 시 롤백하고, BatchJobHistory는 별도 트랜잭션으로 RUNNING, SUCCESS, FAILED 상태를 보존한다.

### 4. 기술 선택

- Spring `@Transactional`
- BatchJobHistoryService의 `REQUIRES_NEW`
- 실패 메시지 길이 제한
- BatchJobStatus enum

### 5. 구현 액션

- 배치 시작 시 RUNNING 이력 저장
- 성공 시 processedCount, successCount, elapsedMs 기록
- 실패 시 FAILED와 errorMessage 기록
- Settlement 저장 중 예외가 발생하면 결과는 롤백되도록 유지

### 6. 검증 방법

- 강제 실패 processor 테스트
- 실패 시 Settlement count가 0인지 확인
- 실패 시 BatchJobHistory가 FAILED로 남는지 확인

### 7. 결과

실패 상황에서 Settlement는 롤백되고 BatchJobHistory에는 FAILED, endedAt, errorMessage가 남는 것을 검증했다.

### 8. 포트폴리오 문장

정산 실패 시 일부 결과만 저장되면 데이터 정합성이 깨진다고 판단했습니다. 그래서 정산 결과는 하나의 트랜잭션으로 롤백하고, 실패 이력은 별도 트랜잭션으로 남겨 운영자가 실패 원인을 추적할 수 있도록 개선했습니다.

### 9. 자소서 문장

금융 배치에서는 실패 자체보다 실패 후 상태를 추적할 수 없는 것이 더 큰 리스크라고 생각했습니다. 정산 결과 저장과 배치 이력 저장을 분리해 결과 데이터는 롤백하고 실패 이력은 보존하도록 구현했습니다. 이를 통해 안정성과 운영 추적 가능성을 함께 확보하는 방향을 경험했습니다.

### 10. 면접 답변

트랜잭션은 두 가지 책임으로 나눴습니다. Settlement 저장은 실패하면 전체 롤백되어야 하므로 정산 처리 트랜잭션 안에 두었습니다. 반면 BatchJobHistory는 실패 원인을 남겨야 하므로 REQUIRES_NEW로 별도 저장했습니다.

### 11. 예상 질문

**Q. 실패 이력까지 같은 트랜잭션에 두면 어떤 문제가 있나요?**

A. 정산 처리 중 예외가 발생하면 전체 트랜잭션이 롤백되면서 실패 이력도 사라질 수 있습니다. 운영 관점에서는 실패 원인을 확인할 수 없기 때문에 이력 저장은 별도 트랜잭션으로 분리했습니다.

---

## GROUP_BY_QUERY 개선

### 1. 문제 상황

BASIC_LOOP는 특정 날짜의 Payment 전체 Entity를 애플리케이션으로 가져와 Java 반복문으로 집계한다. 데이터가 커질수록 Entity 로딩과 애플리케이션 집계가 병목이 될 수 있다.

### 2. 금융 관점에서 중요한 이유

정산 배치는 마감 시간 안에 안정적으로 끝나야 한다. 처리 시간이 길어지면 후속 대사, 지급, 리포트 작업이 지연될 수 있다. 다만 성능 개선 후에도 금액 정합성은 유지되어야 한다.

### 3. 판단 기준

DB가 잘하는 집계는 DB에서 수행하고, Java는 집계 결과를 받아 수수료와 최종 정산금액을 계산한다. 단, 이번 단계에서는 벌크 저장과 인덱스 튜닝을 섞지 않아 GROUP BY 자체의 효과를 분리해 본다.

### 4. 기술 선택

- JPQL constructor projection
- DB GROUP BY 집계
- PaymentSettlementAggregation record
- GroupBySettlementProcessor

### 5. 구현 액션

- `PaymentRepository.aggregateCompletedPaymentsByMerchant` 추가
- Payment 전체 Entity List 대신 가맹점별 집계 결과 조회
- GROUP_BY_QUERY processor 추가
- BASIC_LOOP와 GROUP_BY_QUERY 실행 분기 추가
- 프론트에서 GROUP_BY_QUERY 실행 가능 처리

### 6. 검증 방법

- BASIC_LOOP와 GROUP_BY_QUERY의 가맹점별 금액 동일성 테스트
- 100,000건 기준 API 수동 실행
- BatchJobHistory elapsedMs 확인
- Settlement 합계 비교

### 7. 결과

100,000건 로컬 반복 측정에서 BASIC_LOOP는 728~1043ms, GROUP_BY_QUERY는 104~265ms 범위로 측정되었다. 두 전략의 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액은 동일했다. 이 수치는 로컬 단일 DB 기준이며 캐시와 JVM warm-up에 따라 변동 가능하므로 범위로 기록한다.

### 8. 포트폴리오 문장

Payment 전체를 애플리케이션으로 가져오는 BASIC_LOOP의 병목을 줄이기 위해 DB GROUP BY 집계를 적용했습니다. Java는 집계 결과만 받아 동일한 계산식으로 수수료와 정산금액을 산출했고, BASIC_LOOP와 결과 금액이 동일한지 테스트와 DB 합계로 검증했습니다.

### 9. 자소서 문장

대용량 정산 배치에서 Entity 전체 로딩이 병목이 될 수 있다고 판단해 GROUP_BY_QUERY 전략을 적용했습니다. DB에서 가맹점별 결제금액과 취소금액을 먼저 집계하고, Java에서는 집계 결과만 받아 정산금액을 계산하도록 변경했습니다. 개선 후에도 BASIC_LOOP와 금액 결과가 동일한지 테스트와 수동 검증으로 확인하며 성능보다 정합성을 우선했습니다.

### 10. 면접 답변

GROUP_BY_QUERY는 Payment 전체를 애플리케이션으로 가져오지 않고 DB에서 가맹점별로 집계하는 방식입니다. BASIC_LOOP와 비교해 가져오는 데이터량이 줄어들고, 로컬 100,000건 기준 반복 측정에서 더 짧은 실행 시간이 나왔습니다. 다만 수치는 캐시 영향을 받을 수 있어 범위로 기록했고, 금액 결과 동일성을 먼저 검증했습니다.

### 11. 예상 질문

**Q. 왜 GROUP_BY_QUERY에 벌크 저장까지 같이 넣지 않았나요?**

A. 개선 효과를 단계별로 설명하기 위해서입니다. GROUP_BY_QUERY 단계에서는 조회와 집계 병목만 줄이고, Settlement 저장 최적화는 GROUP_BY_BULK_SAVE 단계에서 별도로 비교하는 것이 더 명확하다고 판단했습니다.

---

## GROUP_BY_BULK_SAVE 1차 구현

### 1. 문제 상황

GROUP_BY_QUERY로 Payment 전체 Entity 로딩 병목은 줄였지만, Settlement 저장은 여전히 개별 `save` 반복 구조였다. 집계 결과 수가 늘어나면 저장 호출도 병목이 될 수 있다.

### 2. 금융 관점에서 중요한 이유

저장 성능을 개선하더라도 정산 결과가 일부 누락되거나 가맹점별 금액이 달라지면 금융 데이터 정합성이 깨진다. 따라서 저장 방식 변경은 실행 시간뿐 아니라 저장 건수와 금액 동일성 검증이 함께 필요하다.

### 3. 판단 기준

한 번에 Hibernate batch_size와 PostgreSQL JDBC 옵션까지 적용하면 어떤 변경이 효과를 냈는지 설명하기 어렵다. 그래서 이번 단계에서는 `saveAll`만 적용하고, 실제 batch insert 설정은 다음 단계로 분리했다.

### 4. 기술 선택

- GROUP_BY_QUERY의 DB GROUP BY 집계 재사용
- GroupByBulkSaveSettlementProcessor 추가
- `settlementRepository.saveAll(settlements)` 적용
- BatchJobHistory 실행 이력 유지

### 5. 구현 액션

- GROUP_BY_BULK_SAVE 전략 실행 분기 추가
- Payment 전체 Entity List 조회 없이 집계 결과로 Settlement 생성
- 개별 `save` 반복 대신 `saveAll` 저장
- 프론트에서 GROUP_BY_BULK_SAVE 실행 가능 처리
- GROUP_BY_BULK_INDEX는 미구현 상태 유지

### 6. 검증 방법

- `./gradlew test`
- `cd frontend && npm run build`
- 저장 건수 검증: 집계 결과 건수, 생성 Settlement 수, 저장 Settlement 수 비교
- 금액 정합성 검증: BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE의 가맹점별 금액 비교
- 실패 처리 검증: GROUP_BY_BULK_SAVE 저장 후 강제 예외 시 Settlement 롤백과 BatchJobHistory FAILED 보존 확인
- API 수동 검증: 2026-05-14, Payment 100,000건 기준 세 전략 실행

### 7. 결과

테스트에서 GROUP_BY_BULK_SAVE는 Payment 전체 Entity List를 조회하지 않고 DB GROUP BY 집계 결과를 사용하며, `saveAll`을 호출하는 것을 확인했다. 100,000건 로컬 API 1회 측정에서 BASIC_LOOP는 882ms, GROUP_BY_QUERY는 133ms, GROUP_BY_BULK_SAVE는 110ms로 기록되었다. 세 전략 모두 정산 결과 100건과 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액이 동일했다.

### 8. 포트폴리오 문장

GROUP_BY_QUERY로 조회 병목을 줄인 뒤, 저장 단계에서도 반복 insert가 병목이 될 수 있다고 판단했습니다. GROUP_BY_BULK_SAVE 1차 구현에서는 집계 결과로 생성한 Settlement를 개별 저장하지 않고 saveAll 기반으로 저장하도록 변경했습니다. 저장 방식이 바뀌어도 금융 데이터의 정합성이 깨지면 안 된다고 판단해, 집계 결과 건수와 실제 저장 건수, 가맹점별 정산금액이 기존 전략들과 동일한지 검증했습니다.

### 9. 자소서 문장

대용량 정산 배치에서 조회 병목을 GROUP BY 집계로 줄인 뒤, 저장 단계에서도 반복 저장이 병목이 될 수 있다고 판단했습니다. 이에 정산 결과를 개별 저장하지 않고 일괄 저장하는 방식으로 개선하되, 저장 건수와 가맹점별 정산금액이 기존 방식과 동일한지 검증했습니다. 성능 개선 과정에서도 데이터 정합성과 중복 처리 방지를 우선해, 같은 정산일자와 처리 전략이 중복 실행되지 않도록 유지했습니다.

### 10. 면접 답변

GROUP_BY_BULK_SAVE는 GROUP_BY_QUERY의 DB GROUP BY 집계 결과를 그대로 사용하고, Settlement 저장 방식만 개별 save 반복에서 saveAll로 바꾼 단계입니다. saveAll만으로 실제 DB batch insert 효과가 보장되는 것은 아니기 때문에 이번 단계에서는 저장 호출 구조 개선과 정합성 검증에 집중했습니다. 집계 결과 건수와 저장 건수가 일치하는지, BASIC_LOOP와 GROUP_BY_QUERY와 금액이 같은지 테스트와 API로 확인했습니다.

### 11. 예상 질문

**Q. saveAll을 적용하면 바로 벌크 저장이라고 볼 수 있나요?**

A. 코드상 일괄 저장 구조를 만들었다고 볼 수 있지만, JPA/Hibernate에서 실제 DB batch insert 효과를 보려면 `hibernate.jdbc.batch_size` 같은 설정이 추가로 필요할 수 있습니다. 그래서 이번 단계에서는 saveAll만 적용하고, batch_size와 PostgreSQL `reWriteBatchedInserts=true`는 다음 단계로 분리했습니다.

**Q. saveAll 적용 후 어떤 정합성 검증을 했나요?**

A. 집계 결과 건수, 생성 Settlement 수, 실제 저장 Settlement 수가 같은지 확인했습니다. 또한 BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE의 가맹점별 결제금액, 취소금액, 순매출, 수수료, 최종 정산금액이 같은지 검증했습니다.

**Q. 성능 수치가 왜 흔들리나요?**

A. 로컬 단일 DB에서 측정했기 때문에 DB/OS 캐시, JVM warm-up, 실행 순서 영향을 받습니다. 그래서 단일값을 성과로 과장하지 않고 반복 측정 범위와 조건을 함께 기록했습니다.
