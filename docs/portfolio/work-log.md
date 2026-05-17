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

---

## 정산 배치 성능 실험 규모 확장 기획

### 1. 문제 상황

10만 건 / Merchant 100개 기준 실험에서 BASIC_LOOP 882ms, GROUP_BY_QUERY 133ms, GROUP_BY_BULK_SAVE 110ms를 확인했다. 이 결과로 Payment 전체 조회 방식의 비효율성과 DB GROUP BY 집계 개선 효과는 확인했다.

하지만 Merchant 수가 100개라 Settlement 저장 결과도 100건 수준이었다. 따라서 saveAll 기반 저장 최적화 효과, Hibernate batch 설정 필요성, 인덱스 필요성을 판단하기에는 실험 규모가 작았다.

### 2. 금융 관점에서 중요한 이유

정산 배치는 데이터가 커질수록 조회 병목, 저장 병목, 메모리 부담, 실패 복구 리스크가 함께 커진다. 작은 데이터에서 빠르게 동작했다는 이유만으로 대용량 거래 정산에 적합하다고 설명하면 신뢰도가 낮다.

### 3. 판단 기준

최적화 설정을 바로 추가하기보다 데이터 규모와 Merchant 수를 늘려 병목 위치를 먼저 확인하기로 했다. 인덱스, Hibernate batch_size, PostgreSQL `reWriteBatchedInserts`, Spring Batch는 측정 결과에 따라 필요성을 판단한다.

### 4. 기획 액션

| 단계 | Payment 수 | Merchant 수 | 목적 | 측정 전략 |
|---:|---:|---:|---|---|
| 1 | 100,000 | 100 | 기준 실험 완료 | BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |
| 2 | 1,000,000 | 5,000 | 중간 확장 및 안정성 검증 | BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |
| 3 | 10,000,000 | 5,000~10,000 | 최종 대용량 검증 | GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |

100만 건은 1000만 건으로 가기 전 데이터 생성, 정합성, 실행 시간, 메모리 부담을 확인하는 중간 검증 단계로 둔다. 1000만 건에서는 전체 Payment를 애플리케이션으로 로딩하는 BASIC_LOOP를 필수로 보지 않고, DB GROUP BY 기반 전략 중심으로 비교한다.

### 5. 검증 기준

- 전략별 Settlement 수 동일
- 전략별 총 결제금액, 총 취소금액, 최종 정산금액 동일
- BatchJobHistory elapsedMs 기록
- 중복 실행 방지 유지
- 실패 시 Settlement 롤백과 BatchJobHistory FAILED 기록 유지

### 6. 결과

이번 작업에서는 데이터를 생성하거나 인덱스, batch 설정, Spring Batch를 적용하지 않았다. 기존 문서의 로드맵을 100만/1000만 건 실험 이후 병목 기반으로 최적화를 판단하는 방향으로 수정했다.

### 7. 포트폴리오 문장

10만 건 기준에서는 GROUP BY 집계만으로 충분한 성능 개선을 확인했습니다. 다만 당시 Merchant 수가 100개라 Settlement 저장 결과가 100건 수준이었고, saveAll 기반 저장 최적화의 효과를 충분히 보기 어렵다고 판단했습니다. 그래서 다음 실험에서는 Payment를 100만 건, Merchant를 5,000개로 늘려 조회 병목과 저장 병목을 함께 확인할 수 있도록 조건을 재설계했습니다.

---

## benchmark-medium 데이터셋 구현 및 100만 건 측정

### 1. 문제 상황

기존 10만 건 데이터에 90만 건을 단순 추가하면 Merchant 분포와 Settlement 저장 건수가 섞여 성능 비교 조건이 불명확해진다. 100만 건 실험은 1000만 건으로 가기 전 데이터 생성, 실행 시간, 정합성, 메모리 부담을 점검하는 중간 단계가 필요했다.

### 2. 금융 관점에서 중요한 이유

정산 성능 개선은 단순히 빠른 실행 시간만 확인하면 안 된다. 데이터 규모가 커져도 전략별 정산 결과 건수와 금액이 동일해야 하고, 과거 실행 이력은 추적 가능해야 한다.

### 3. 판단 기준

medium 데이터셋은 기존 데이터에 추가하지 않고 재생성하도록 했다. 재생성 대상은 `settlements`, `payments`, `merchants`로 제한하고, `batch_job_histories`는 실행 이력이므로 보존했다. 운영 기능처럼 오용되지 않도록 기본값은 비활성화하고 `benchmark-medium` 프로파일과 `benchmark.reset-enabled=true` 조건에서만 실행되도록 했다.

### 4. 구현 액션

- `benchmark-medium` 프로파일 추가
- benchmark reset 설정 추가
- Merchant 5,000개, Payment 1,000,000건 생성
- 5,000개 Merchant에 Payment를 고르게 분산
- 오늘 날짜 기준으로 정산 가능한 Payment 생성
- 재생성 시 `batch_job_histories` 보존

### 5. 검증 방법

- `./gradlew test`
- `./gradlew bootRun --args='--spring.profiles.active=benchmark-medium'`
- API 실행: BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE
- DB 건수 확인
- 전략별 Settlement 합계 비교
- 같은 날짜 + 같은 전략 중복 실행 409 확인

### 6. 결과

| 항목 | 결과 |
|---|---:|
| Merchant 수 | 5,000 |
| Payment 수 | 1,000,000 |
| 정산일자 Payment 수 | 1,000,000 |
| 데이터 재생성 시간 | 96,314ms |
| 전략별 Settlement 수 | 5,000 |

| 데이터 건수 | Merchant 수 | 전략 | 실행 시간 | 결과 동일성 |
|---:|---:|---|---:|---|
| 1,000,000 | 5,000 | BASIC_LOOP | 8,253ms | 기준 |
| 1,000,000 | 5,000 | GROUP_BY_QUERY | 899ms | BASIC_LOOP와 동일 |
| 1,000,000 | 5,000 | GROUP_BY_BULK_SAVE | 798ms | BASIC_LOOP와 동일 |

### 7. 포트폴리오 문장

10만 건 실험에서 조회 병목을 확인한 뒤, 100만 건 / Merchant 5,000개 조건으로 데이터셋을 재생성해 중간 확장 실험을 진행했습니다. 기존 데이터에 단순 추가하지 않고 benchmark-medium 조건으로 재생성해 Merchant 분포를 통제했고, 세 전략의 Settlement 수와 총 정산금액이 동일한지 검증했습니다.

---

## benchmark-large 데이터셋 구현 및 1000만 건 측정

### 1. 문제 상황

100만 건 실험으로 중간 확장 조건은 확인했지만, 최종 대용량 정산 배치 설명을 위해서는 1000만 건 조건에서 DB GROUP BY 기반 전략이 정상 동작하는지 확인할 필요가 있었다.

### 2. 판단 기준

1000만 건에서는 전체 Payment를 애플리케이션으로 로딩하는 BASIC_LOOP를 강제 실행하지 않고, GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE 중심으로 측정했다. 기존 benchmark 데이터에 추가하지 않고 benchmark-large 조건으로 재생성해 Merchant 분포를 통제했다.

### 3. 구현 액션

- `benchmark-large` 프로파일 추가
- Merchant 10,000개, Payment 10,000,000건 생성
- 10,000개 Merchant에 Payment를 고르게 분산
- 오늘 날짜 기준으로 정산 가능한 Payment 생성
- 재생성 시 `settlements`, `payments`, `merchants` 초기화
- `batch_job_histories` 보존

### 4. 검증 방법

- `./gradlew test`
- `./gradlew bootRun --args='--spring.profiles.active=benchmark-large'`
- API 실행: GROUP_BY_QUERY, GROUP_BY_BULK_SAVE
- DB 건수 확인
- 전략별 Settlement 합계 비교
- 같은 날짜 + 같은 전략 중복 실행 409 확인

### 5. 결과

| 항목 | 결과 |
|---|---:|
| Merchant 수 | 10,000 |
| Payment 수 | 10,000,000 |
| 정산일자 Payment 수 | 10,000,000 |
| 데이터 재생성 시간 | 978,764ms |
| 전략별 Settlement 수 | 10,000 |

| 데이터 건수 | Merchant 수 | 전략 | 실행 시간 | 결과 동일성 |
|---:|---:|---|---:|---|
| 10,000,000 | 10,000 | GROUP_BY_QUERY | 4,796ms | 기준 |
| 10,000,000 | 10,000 | GROUP_BY_BULK_SAVE | 4,149ms | GROUP_BY_QUERY와 동일 |

GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE는 같은 payments 선집계 쿼리를 사용한다. API 기준 두 전략의 차이는 약 647ms로, saveAll 기반 저장 구조가 개별 저장보다 유리한 지점은 확인됐다. 다만 실행계획 기준 GROUP_BY_QUERY 집계 쿼리만 약 2,979ms가 걸렸고, `payments` 1000만 건 Parallel Seq Scan과 HashAggregate temp spill이 확인됐기 때문에 현재 병목은 저장보다 조회/집계 쪽이 더 크다고 판단했다.

### 6. 포트폴리오 문장

1000만 건 / Merchant 10,000개 조건에서는 전체 Payment를 애플리케이션으로 로딩하는 BASIC_LOOP를 강제 실행하지 않고, DB GROUP BY 기반 전략 중심으로 측정했습니다. GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE 모두 10,000건의 정산 결과를 생성했고 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액이 동일함을 확인했습니다.

---

## GROUP_BY_QUERY 실행계획 기반 쿼리 구조 개선

### 1. 문제 상황

1000만 건 데이터에서 GROUP_BY_QUERY 실행계획을 확인했을 때, 기존 쿼리는 payments와 merchants를 먼저 조인한 뒤 집계했다. 이 구조에서는 조인 이후의 중간 데이터가 커지고, GROUP BY 과정에서 temp spill이 크게 발생했다.

### 2. 기존 쿼리

```sql
select
    m.id as merchant_id,
    m.name,
    m.fee_rate,
    coalesce(sum(case when p.type = 'PAYMENT' then p.amount else 0 end), 0) as total_payment_amount,
    coalesce(sum(case when p.type = 'CANCEL' then p.amount else 0 end), 0) as total_cancel_amount,
    count(p.id) as processed_count
from payments p
join merchants m on p.merchant_id = m.id
where p.transaction_date = date '2026-05-15'
  and p.status = 'COMPLETED'
group by m.id, m.name, m.fee_rate
order by m.id;
```

### 3. 판단

정산에 필요한 핵심 집계는 payments 테이블의 merchant_id 기준 결제/취소 금액 합계다. 따라서 merchants와 먼저 조인하기보다, payments를 먼저 merchant_id 기준으로 집계한 뒤 집계 결과만 merchants와 조인하는 편이 중간 데이터 크기를 줄일 수 있다고 판단했다.

### 4. 개선 쿼리

```sql
select
    agg.merchant_id,
    m.name,
    m.fee_rate,
    agg.payment_amount,
    agg.cancel_amount,
    agg.processed_count
from (
    select
        p.merchant_id,
        coalesce(sum(case when p.type = 'PAYMENT' then p.amount else 0 end), 0) as payment_amount,
        coalesce(sum(case when p.type = 'CANCEL' then p.amount else 0 end), 0) as cancel_amount,
        count(*) as processed_count
    from payments p
    where p.transaction_date = date '2026-05-15'
      and p.status = 'COMPLETED'
    group by p.merchant_id
) agg
join merchants m on m.id = agg.merchant_id
order by agg.merchant_id;
```

### 5. 결과

| 항목 | 개선 전 | 개선 후 |
|---|---:|---:|
| EXPLAIN 실행 시간 | 3,855ms | 3,274ms |
| API GROUP_BY_QUERY | 5,026ms | 4,796ms |
| API GROUP_BY_BULK_SAVE | 4,596ms | 4,149ms |
| temp written | 92,954 | 47,200 |

### 6. 검증

- 두 전략의 정산 합계 동일 확인
- 중복 실행 시 409 Conflict 유지
- `./gradlew test` 통과

### 7. 포트폴리오 설명

1000만 건 정산 쿼리의 실행계획을 확인한 결과, 기존에는 payments와 merchants를 먼저 조인한 뒤 집계해 중간 데이터가 커지고 temp spill이 많이 발생했습니다. 이를 줄이기 위해 payments를 먼저 merchant_id 기준으로 집계한 뒤, 집계 결과만 merchants와 조인하도록 쿼리 구조를 변경했습니다. 그 결과 temp written이 92,954에서 47,200으로 줄었고, GROUP_BY_QUERY API 실행 시간도 5,026ms에서 4,796ms로 개선되었습니다.

---

## 1000만 건 GROUP BY 병목 개선 방향

### 1. 문제 상황

개선된 GROUP_BY_QUERY는 payments를 먼저 `merchant_id` 기준으로 집계한 뒤 merchants와 조인한다. 개선 후에도 benchmark-large 데이터가 `2026-05-15` 단일 날짜에 1000만 건 몰려 있어 `payments`를 대부분 `Parallel Seq Scan`으로 읽고, `HashAggregate` 과정에서 temp disk spill이 남아 있다.

### 2. 판단

현재 데이터는 `status`가 모두 `COMPLETED`라 partial index는 선택도를 크게 줄이지 못할 가능성이 높다. 따라서 바로 인덱스를 추가하기보다, temp spill과 전체 스캔 비용을 분리해서 확인한 뒤 다음 개선을 선택한다.

### 3. 다음 개선 순서

| 순서 | 개선 후보 | 목적 | 확인 항목 |
|---:|---|---|---|
| 1 | 세션 단위 `work_mem` 실험 | HashAggregate temp spill 감소 확인 | temp read/write, HashAggregate batches, Execution Time |
| 2 | 일반 covering index 실험 | heap read 감소와 Index Only Scan 가능성 확인 | Scan 방식, Buffers read/hit, Execution Time |
| 3 | 날짜 분산 데이터셋 또는 사전 집계 테이블 | 1000만 건 전체 스캔 비용이 계속 클 때 구조 대안 검토 | 날짜 선택도, 집계 대상 row 수, 실행 시간 |

covering index 후보는 다음과 같이 검토한다.

```sql
create index idx_payments_settlement_covering
on payments (transaction_date, status, merchant_id, type)
include (amount);
```

### 4. 이번 단계에서 하지 않은 것

- `work_mem` 설정 적용
- 인덱스 생성
- 날짜 분산 데이터 생성
- 사전 집계 테이블 구현
- Spring Batch 도입

---

## work_mem 세션 단위 GROUP BY temp spill 실험

### 1. 문제 상황

1000만 건 GROUP_BY_QUERY 실행계획에서 `payments` Parallel Seq Scan은 유지됐고, `merchant_id` 기준 HashAggregate 과정에서 temp disk spill이 발생했다. 기본 `work_mem`은 4MB였다.

### 2. 판단

전역 설정을 바꾸기 전에 현재 DB 세션에서만 `SET work_mem`을 적용해 temp spill 감소 여부를 확인했다. 비교 항목은 Execution Time, HashAggregate batches, temp read/write, Buffers hit/read로 제한했다.

### 3. 결과

| work_mem | Execution Time | HashAggregate Batches | temp read | temp written | Buffers hit/read |
|---:|---:|---:|---:|---:|---:|
| 4MB | 4,295.640ms | 5 | 26,516 | 46,759 | 13,363 / 100,170 |
| 64MB | 2,874.350ms | 1 | 0 | 0 | 13,459 / 100,074 |
| 128MB | 2,645.038ms | 1 | 0 | 0 | 13,555 / 99,978 |
| 256MB | 2,645.408ms | 1 | 0 | 0 | 13,651 / 99,882 |

### 4. 판단 결과

64MB부터 HashAggregate temp spill이 제거됐다. 128MB와 256MB는 실행 시간 차이가 거의 없어, 무조건 큰 값을 적용하기보다 동시 실행 수와 DB 메모리 여유를 함께 고려해야 한다. 다음 개선은 covering index 실험으로 heap read와 1000만 건 전체 스캔 비용을 줄일 수 있는지 확인한다.

### 5. 이번 단계에서 하지 않은 것

- 전역 `work_mem` 변경
- 인덱스 생성
- 날짜 분산 데이터 생성
- 사전 집계 테이블 구현

---

## work_mem 128MB 적용 전후 3회 평균 재측정

### 1. 문제 상황

psql 또는 DBeaver 세션에서 `SET work_mem`을 실행해도 백엔드 API가 사용하는 DB 커넥션에는 적용되지 않는다. 따라서 API 성능 비교는 백엔드 DB 커넥션에 work_mem이 적용되는 방식으로 다시 측정해야 했다.

### 2. 적용 방식

전역 설정이나 `postgresql.conf`는 수정하지 않았다. 기본값 측정은 일반 JDBC URL로 서버를 실행했고, 128MB 측정은 PostgreSQL JDBC URL의 세션 옵션 `options=-c work_mem=128MB`로 서버를 재실행했다. 각 항목은 3회 반복 측정 후 평균값으로 기록했다.

### 3. 결과

| 항목 | work_mem 적용 전 | work_mem 적용 후 | 개선 효과 |
|---|---:|---:|---:|
| EXPLAIN 실행 시간 | 3,159.873ms | 2,458.106ms | 701.767ms |
| API GROUP_BY_QUERY | 4,203.667ms | 3,399.333ms | 804.334ms |
| API GROUP_BY_BULK_SAVE | 3,879.333ms | 3,283.333ms | 596.000ms |
| temp written | 46,805 | 0 | 46,805 감소 |

### 4. 정합성 확인

모든 API 측정에서 `processedCount`는 10,000,000건, Settlement 수는 10,000건이었다. GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE의 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액은 동일했다.

### 5. 판단

3회 평균 기준으로 128MB 적용 시 temp spill이 제거됐고 EXPLAIN과 API 실행 시간이 모두 줄었다. 다만 전역 work_mem 변경은 보류하고, 다음 작업은 covering index로 heap read와 전체 스캔 비용을 확인하는 방향으로 둔다.

---

## covering index 성능 실험

### 1. 문제 상황

`work_mem=128MB` 적용 후 HashAggregate temp spill은 제거됐지만, GROUP_BY_QUERY는 여전히 `payments` 1000만 건을 `Parallel Seq Scan`으로 읽었다. 남은 병목이 heap read와 전체 스캔 비용인지 확인하기 위해 covering index를 실험했다.

### 2. 적용한 인덱스

```sql
create index idx_payments_settlement_covering
on payments (transaction_date, status, merchant_id, type)
include (amount);
```

### 3. 결과

| 항목 | 인덱스 적용 전 평균 | 인덱스 적용 후 평균 | 개선 효과 |
|---|---:|---:|---:|
| EXPLAIN 실행 시간 | 2,958.553ms | 2,647.294ms | 311.259ms |
| API GROUP_BY_QUERY | 3,642.333ms | 3,534.333ms | 108.000ms |
| API GROUP_BY_BULK_SAVE | 3,369.333ms | 3,302.333ms | 67.000ms |
| Buffers read | 98,568 | 99,117 | -549 |
| temp written | 0 | 0 | 0 |

### 4. 실행계획 비교

인덱스 생성 후에도 `payments`는 `Parallel Seq Scan`을 사용했다. `Index Scan` 또는 `Index Only Scan`은 발생하지 않았고, 따라서 `Heap Fetches` 비교 대상도 없었다. HashAggregate는 `Batches: 1`로 유지됐고 temp read/write는 없었다.

### 5. 판단

현재 benchmark-large 데이터는 `2026-05-15` 단일 날짜에 1000만 건이 몰려 있고 `status`가 모두 `COMPLETED`라 조건 선택도가 낮다. 이 조건에서는 covering index가 선택되지 않아 heap read 감소 효과를 확인하지 못했다. API 평균은 소폭 줄었지만 Buffers read는 줄지 않았으므로, 성능 차이는 캐시와 실행 변동 영향일 가능성이 있다. 다음 판단은 실험용 인덱스 유지 또는 drop 여부를 정한 뒤, 날짜 분산 데이터셋이나 사전 집계 테이블 중 하나를 선택하는 것이다.

### 6. 정합성 확인

모든 API 측정에서 `processedCount`는 10,000,000건, Settlement 수는 10,000건이었다. GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE의 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액은 동일했다.

### 7. 기대와 달랐던 점

정산 쿼리에 필요한 컬럼을 인덱스 키와 include 컬럼에 포함하면, PostgreSQL이 Index Only Scan을 선택하고 테이블 본문 접근을 줄일 수 있을 것이라고 가정했다. 하지만 실제 실행계획에서는 인덱스가 선택되지 않았고, `payments` 전체를 병렬로 읽는 계획이 유지됐다. Buffers read도 줄지 않아 heap read 감소 효과를 확인하지 못했다.

### 8. 원인 분석

인덱스가 선택되지 않은 핵심 원인은 데이터 분포와 조건 선택도였다. benchmark-large 데이터는 1000만 건이 모두 `2026-05-15`에 몰려 있고 `status`도 모두 `COMPLETED`였다. 이 조건에서는 `transaction_date`, `status` 조건이 대부분의 row를 통과시키므로, PostgreSQL 입장에서는 큰 인덱스를 따라 읽는 것보다 테이블을 병렬로 스캔하는 편이 더 효율적이라고 판단한 것으로 해석했다.

### 9. 배운 점

인덱스는 무조건 성능을 높이는 수단이 아니라 데이터 분포, 조건 선택도, 실행계획과 함께 판단해야 한다. 이번 실험에서는 EXPLAIN으로 병목을 보고 가설을 세웠지만, 실제 실행계획과 Buffers 지표를 통해 가설이 맞지 않음을 확인했다. 성능 개선은 설정이나 인덱스를 추가하는 작업이 아니라, 측정 결과에 따라 방향을 수정하는 과정이라는 점을 정리했다.

### 10. 다음 개선 방향

- 실험용 covering index를 유지할지 drop할지 판단한다.
- 날짜 분산 데이터셋을 만들어 `transaction_date` 조건 선택도가 높아질 때 인덱스가 선택되는지 확인한다.
- 단일 날짜 대량 정산이 계속 핵심 시나리오라면, 일자·가맹점 단위 사전 집계 테이블을 검토한다.
- 인덱스보다 사전 집계가 더 적절한 경우, 원천 Payment와 사전 집계 결과의 정합성 검증 방법을 함께 설계한다.

### 11. 자소서/면접 요약

1000만 건 정산 쿼리에서 Parallel Seq Scan과 heap read 비용이 남아 있어 covering index로 Index Only Scan을 유도할 수 있을지 가설을 세웠습니다. 정산 쿼리에 필요한 조건 컬럼과 집계 컬럼을 인덱스에 포함했지만, 실제 실행계획에서는 인덱스가 사용되지 않고 Parallel Seq Scan이 유지됐습니다. 원인을 확인해 보니 모든 데이터가 단일 날짜에 몰려 있고 상태값도 대부분 COMPLETED라 조건 선택도가 낮았습니다. 이 경험을 통해 인덱스는 무조건 성능을 높이는 수단이 아니라 데이터 분포와 실행계획을 함께 보고 판단해야 한다는 점을 확인했습니다. 이후 방향도 인덱스를 억지로 유지하는 것이 아니라 날짜 분산 데이터셋이나 일자·가맹점 단위 사전 집계 테이블 검토로 수정했습니다.

---

## benchmark-large-date-distributed 데이터셋 구현

### 1. 문제 상황

기존 benchmark-large 데이터는 10,000,000건이 `2026-05-15` 단일 날짜에 몰려 있었다. 이 조건에서는 `transaction_date` 조건이 대부분의 데이터를 통과시키므로 covering index의 선택도와 효과를 판단하기 어려웠다.

### 2. 판단

인덱스 효과를 다시 검증하려면 같은 10,000,000건 규모를 유지하되, 결제일자를 2026년 5월 한 달에 분산해 특정 정산일의 조회 대상 row 수를 줄여야 한다. 기존 benchmark 데이터에 추가하지 않고 재생성해야 Merchant와 날짜 분포를 통제할 수 있다.

### 3. 구현 액션

- `benchmark-large-date-distributed` 프로파일 추가
- Merchant 10,000개 생성
- Payment 10,000,000건 생성
- `transaction_date`를 `2026-05-01`부터 `2026-05-31`까지 최대한 균등 분산
- 10,000개 Merchant에 Payment를 순차 분산
- `reset-enabled=true`일 때만 `settlements`, `payments`, `merchants` 순서로 초기화
- `batch_job_histories` 보존

### 4. 검증 결과

- 데이터 재생성 시간: 2,870,068ms
- `merchants = 10,000`
- `payments = 10,000,000`
- `settlements = 0`
- `batch_job_histories = 70` 보존
- 날짜 범위: `2026-05-01` ~ `2026-05-31`
- distinct transaction_date: 31일
- 날짜별 Payment 수: `2026-05-01` ~ `2026-05-20`은 각 322,581건, `2026-05-21` ~ `2026-05-31`은 각 322,580건
- `2026-05-15` Payment 수: 322,581건

## 날짜 분산 benchmark-large 인덱스 없는 기준선 측정

### 1. 문제 상황

단일 날짜 1000만 건 데이터에서는 `transaction_date` 조건 선택도가 낮아 covering index 효과를 판단하기 어려웠다. 날짜 분산 데이터셋을 만든 뒤에는 먼저 실험용 인덱스를 제거하고, 인덱스 없는 현재 쿼리 기준선을 다시 측정해야 했다.

### 2. 판단

인덱스 실험을 다시 진행하기 전에 기준선이 명확해야 한다고 판단했다. 따라서 `idx_payments_settlement_covering`만 제거하고 `ANALYZE payments`를 실행한 뒤, 2026-05-17 기준 322,581건을 대상으로 `work_mem` 기본값 4MB에서 측정했다.

### 3. 검증 방법

- `drop index if exists idx_payments_settlement_covering`
- `payments` 인덱스 목록 확인: `payments_pkey`만 유지
- `show work_mem`: 4MB
- EXPLAIN ANALYZE 3회 측정
- API GROUP_BY_QUERY 3회 측정
- API GROUP_BY_BULK_SAVE 3회 측정
- API 측정 전마다 2026-05-17 settlements만 초기화
- GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE 합계 금액 동일성 확인

### 4. 결과

| 항목 | 인덱스 없는 기준선 평균 |
|---|---:|
| EXPLAIN 실행 시간 | 2,242.501ms |
| API GROUP_BY_QUERY | 3,021.333ms |
| API GROUP_BY_BULK_SAVE | 2,478.333ms |
| temp written | 1,880 |

정산 결과는 두 전략 모두 `processedCount=322,581`, Settlement 10,000건이었다. GROUP_BY_QUERY와 GROUP_BY_BULK_SAVE의 총 결제금액, 총 취소금액, 순매출, 수수료, 최종 정산금액 합계는 동일했다.

### 5. 다음 방향

날짜 분산 후에도 인덱스 없는 기준선에서는 `Parallel Seq Scan`과 `HashAggregate` temp spill이 남아 있다. 2026-05-17 대상 322,581건을 찾기 위해 약 967만 건이 filter에서 제거되므로, 다음 단계에서는 바로 covering index로 가지 않고 단순 인덱스부터 날짜 선택도 효과를 검증한다.

1차 실험은 다음 인덱스로 날짜 조건만으로 `Parallel Seq Scan`이 줄어드는지 확인한다.

```sql
create index idx_payments_transaction_date
on payments (transaction_date);
```

2차 실험은 다음 인덱스로 where 조건과 group by 기준까지 고려했을 때 더 좋아지는지 확인한다. `status`는 현재 선택도가 낮을 수 있지만 정산 쿼리 조건에 포함되므로 실험 가치가 있다.

```sql
create index idx_payments_date_status_merchant
on payments (transaction_date, status, merchant_id);
```

판단 기준은 Execution Time, Scan 방식, `Parallel Seq Scan` 유지 여부, `Index Scan` 또는 `Bitmap Index Scan` 사용 여부, Rows Removed by Filter, Buffers hit/read, temp read/write, API `GROUP_BY_QUERY`, API `GROUP_BY_BULK_SAVE`, 정산 합계 동일성이다. 이번 작업에서는 실제 인덱스 생성, 성능 측정, `work_mem` 변경은 하지 않는다.
