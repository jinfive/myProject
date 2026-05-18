# Interview Notes

## 왜 이 프로젝트를 했나요?

### 30초 답변

금융 IT에서 중요한 정합성, 안정성, 성능 개선을 한 흐름으로 보여주기 위해 정산 배치 프로젝트를 선택했습니다. 결제와 취소 데이터를 일자별로 정산하고, 중복 정산 방지, 실패 이력 추적, GROUP BY 기반 성능 개선을 구현했습니다. 단순 CRUD보다 금융권에서 자주 다루는 배치와 금액 데이터 리스크를 설명하기 적합하다고 판단했습니다.

### 1분 답변

금융 IT에서는 금액 데이터가 정확히 계산되고, 실패했을 때 원인을 추적할 수 있으며, 대용량 데이터도 마감 시간 안에 처리되어야 합니다. 이 프로젝트는 그런 요구를 보여주기 위해 대용량 결제/취소 데이터를 일별로 정산하는 배치 시스템으로 설계했습니다. BASIC_LOOP 기준선을 먼저 구현하고, GROUP_BY_QUERY로 Payment 전체 Entity 로딩을 줄였습니다. 동시에 중복 정산 방지, 실패 시 Settlement 롤백과 BatchJobHistory 보존, 전략별 결과 동일성 검증을 넣어 성능뿐 아니라 정합성과 안정성도 함께 다뤘습니다.

---

## 왜 BASIC_LOOP를 먼저 구현했나요?

### 30초 답변

성능 개선 효과를 설명하려면 기준선이 필요했기 때문입니다. BASIC_LOOP는 Payment 전체를 조회해 Java 반복문으로 집계하는 단순한 방식입니다. 실무 최적화 방식은 아니지만, GROUP_BY_QUERY가 어떤 병목을 줄였는지 비교하기 위한 출발점으로 사용했습니다.

### 1분 답변

처음부터 최적화된 쿼리만 만들면 개선 전후를 설명하기 어렵습니다. 그래서 BASIC_LOOP를 기준선으로 구현했습니다. 이 방식은 특정 정산일자의 Payment 전체를 조회하고 Java에서 가맹점별로 결제금액, 취소금액, 수수료, 최종 정산금액을 계산합니다. 이후 GROUP_BY_QUERY를 적용했을 때 처리 시간 차이와 결과 동일성을 비교할 수 있었습니다. 즉 BASIC_LOOP는 성능 개선 전 문제 상황을 의도적으로 드러내는 역할입니다.

---

## 왜 GROUP_BY_QUERY를 적용했나요?

### 30초 답변

BASIC_LOOP는 Payment 전체 Entity를 애플리케이션으로 가져와 집계하기 때문에 데이터가 커질수록 병목이 될 수 있습니다. DB가 잘하는 집계는 DB에서 처리하는 것이 적절하다고 판단해 GROUP BY로 가맹점별 금액을 먼저 집계했습니다. Java는 집계 결과만 받아 정산금액을 계산하도록 역할을 줄였습니다.

### 1분 답변

정산 배치는 특정 날짜의 대량 거래를 가맹점별로 집계하는 작업입니다. BASIC_LOOP는 모든 Payment Entity를 애플리케이션으로 가져오므로 메모리 사용과 반복 집계 비용이 커질 수 있습니다. GROUP_BY_QUERY에서는 DB에서 transaction_date, status, type, merchant 기준으로 결제금액과 취소금액을 집계하고, Java에서는 수수료와 최종 정산금액만 계산했습니다. 벌크 저장과 인덱스는 아직 섞지 않았고, GROUP BY 자체의 효과를 분리해 보기 위해 다음 단계로 남겼습니다.

---

## 중복 정산은 어떻게 막았나요?

### 30초 답변

중복 기준을 `merchant_id + settlement_date + processing_strategy`로 잡았습니다. 같은 날짜라도 BASIC_LOOP와 GROUP_BY_QUERY는 비교를 위해 각각 저장할 수 있어야 하지만, 같은 전략이 반복 실행되면 중복 정산이 됩니다. 그래서 서비스 검사와 DB unique constraint로 막았습니다.

### 1분 답변

처음에는 정산일자만 기준으로 중복을 막을 수 있다고 생각했지만, 성능 비교를 하려면 같은 날짜에 여러 처리 전략의 결과를 저장해야 했습니다. 그래서 중복 기준을 가맹점, 정산일자, 처리 전략으로 정했습니다. 서비스 계층에서는 이미 같은 날짜와 전략의 Settlement가 있으면 실행을 거부하고, DB에는 unique constraint를 둬서 최종적으로 중복 저장을 막습니다. 이 구조는 성능 비교 가능성과 금액 중복 반영 방지를 함께 만족합니다.

---

## 실패하면 어떻게 처리되나요?

### 30초 답변

정산 중 실패하면 Settlement 결과는 롤백되고, BatchJobHistory에는 FAILED 상태와 실패 원인이 남습니다. 결과 데이터가 일부만 저장되는 것은 막고, 운영자가 실패 원인은 추적할 수 있게 했습니다.

### 1분 답변

정산 배치 실패 시 가장 위험한 것은 일부 가맹점 결과만 저장되는 상황입니다. 그래서 Settlement 저장은 정산 처리 트랜잭션 안에서 처리하고, 예외가 발생하면 전체 롤백되도록 했습니다. 반면 실패 이력은 사라지면 안 되기 때문에 BatchJobHistoryService를 별도 트랜잭션으로 분리했습니다. 시작 시 RUNNING을 남기고, 성공하면 SUCCESS와 처리 건수, 실행 시간을 기록하며, 실패하면 FAILED와 errorMessage를 남깁니다.

---

## 트랜잭션은 어떻게 잡았나요?

### 30초 답변

정산 결과 저장과 배치 이력 저장을 분리했습니다. Settlement는 실패 시 롤백되어야 하므로 정산 트랜잭션에 두고, BatchJobHistory는 실패해도 보존되어야 하므로 `REQUIRES_NEW`로 별도 트랜잭션을 사용했습니다.

### 1분 답변

트랜잭션은 데이터 정합성과 실패 추적이라는 두 책임으로 나눴습니다. 정산 결과인 Settlement는 중간에 실패하면 일부 저장 상태가 남으면 안 되므로 하나의 정산 트랜잭션에서 처리했습니다. 반대로 BatchJobHistory는 실패 원인을 추적하기 위한 운영 데이터이기 때문에 정산 트랜잭션과 같이 롤백되면 안 됩니다. 그래서 BatchJobHistory 저장과 상태 변경은 `REQUIRES_NEW`로 분리했습니다.

---

## 성능 개선은 어떻게 검증했나요?

### 30초 답변

같은 Payment 100,000건과 같은 정산일자를 기준으로 BASIC_LOOP와 GROUP_BY_QUERY를 실행했습니다. 실행 시간은 BatchJobHistory의 elapsedMs로 기록했고, 정산 결과는 총 결제금액, 총 취소금액, 수수료, 최종 정산금액 합계가 같은지 확인했습니다.

### 1분 답변

성능 비교는 같은 데이터 건수와 같은 정산일자를 유지하는 것을 기준으로 했습니다. 로컬 벤치마크에서는 Payment 100,000건을 같은 날짜로 맞추고, 전략별로 정산을 실행했습니다. 실행 시간은 BatchJobHistory.elapsedMs를 기준으로 기록했습니다. 다만 로컬 DB에서는 캐시와 JVM warm-up 영향으로 수치가 흔들릴 수 있어 단일값이 아니라 반복 측정 범위로 남겼습니다. 또한 성능 개선 후에도 금액 결과가 동일한지 자동 테스트와 DB 합계로 검증했습니다.

---

## 실무라면 어떤 방식이 더 적절한가요?

### 30초 답변

실무라면 BASIC_LOOP보다 DB GROUP BY 집계, 벌크 저장, 적절한 인덱스, 실행 계획 확인이 더 적절합니다. 다만 인덱스를 무조건 추가하기보다 EXPLAIN ANALYZE로 실제 쿼리 계획과 쓰기 비용을 함께 확인해야 합니다.

### 1분 답변

실무 정산 배치에서는 전체 Entity를 애플리케이션으로 가져오는 방식보다 DB에서 필요한 집계만 수행하는 것이 일반적으로 더 적절합니다. 다음 단계로는 GROUP BY 결과를 Settlement에 개별 저장하는 병목을 줄이기 위해 bulk save나 batch insert를 검토할 수 있습니다. 이후 transaction_date, status, type, merchant_id 기준 인덱스를 검토하되, 인덱스는 쓰기 비용도 있으므로 EXPLAIN ANALYZE와 실제 실행 시간 비교가 필요합니다. 마지막으로 원천 Payment와 Settlement를 대사하는 기능까지 있어야 금융 정합성 근거가 완성됩니다.

---

## 다음 개선은 무엇인가요?

### 30초 답변

다음은 Hibernate batch_size와 PostgreSQL reWriteBatchedInserts 적용 검토입니다. GROUP_BY_BULK_SAVE 1차에서 saveAll만 적용했기 때문에, 실제 DB batch insert 효과는 다음 단계에서 분리해 측정할 계획입니다.

### 1분 답변

현재 GROUP_BY_BULK_SAVE 1차에서는 DB GROUP BY 집계 결과를 유지하면서 Settlement 저장을 saveAll 기반으로 바꿨습니다. 다만 saveAll만으로 실제 JDBC batch insert 효과를 단정할 수 없기 때문에 다음 단계에서는 Hibernate batch_size, order_inserts, PostgreSQL reWriteBatchedInserts 옵션을 분리 적용해 비교할 계획입니다. 이후 GROUP_BY_BULK_INDEX에서 조회 조건 기준 인덱스를 적용하고, EXPLAIN ANALYZE로 실제 실행 계획을 확인할 계획입니다. 성능 개선과 별도로 원천 Payment와 Settlement 결과 대사 기능도 추가해야 합니다.

---

## saveAll을 적용하면 바로 벌크 저장이라고 볼 수 있나요?

### 30초 답변

코드상으로는 개별 save 반복을 줄이고 일괄 저장 구조를 만든 것입니다. 다만 JPA/Hibernate에서 실제 DB batch insert 효과를 보려면 `hibernate.jdbc.batch_size` 같은 설정이 추가로 필요할 수 있습니다. 그래서 이번 단계에서는 saveAll만 적용하고, batch 설정과 JDBC 옵션은 다음 단계로 분리했습니다.

### 1분 답변

`saveAll`은 여러 Entity를 한 번에 repository에 전달한다는 점에서 코드 구조상 일괄 저장에 가깝습니다. 하지만 Hibernate가 실제로 JDBC batch insert를 수행하려면 `hibernate.jdbc.batch_size`, `order_inserts` 같은 설정과 DB 드라이버 옵션이 필요할 수 있습니다. 그래서 이번 작업에서는 saveAll 적용 자체와 정합성 검증에 집중했습니다. 이후 Hibernate batch_size와 PostgreSQL `reWriteBatchedInserts=true`를 별도 단계로 적용해 성능 차이를 분리 측정할 계획입니다.

---

## saveAll 적용 후 어떤 정합성 검증을 했나요?

### 30초 답변

집계 결과 건수, 생성한 Settlement 수, 실제 저장된 Settlement 수가 일치하는지 확인했습니다. 그리고 BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE의 가맹점별 결제금액, 취소금액, 수수료, 최종 정산금액이 같은지 테스트했습니다.

### 1분 답변

저장 방식을 바꾸면 일부 결과가 누락되거나 금액이 달라지는 리스크가 있다고 봤습니다. 그래서 먼저 DB GROUP BY 집계 결과 건수와 생성 Settlement 수, 저장된 Settlement 수를 비교했습니다. 그 다음 BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE를 같은 날짜와 같은 Payment 데이터로 실행하고, 가맹점별 총 결제금액, 총 취소금액, 순매출, 수수료, 최종 정산금액이 동일한지 검증했습니다. 실패 케이스에서는 Settlement가 롤백되고 BatchJobHistory에 FAILED와 errorMessage가 남는지도 확인했습니다.

---

## 저장 건수가 누락되지 않았는지는 어떻게 확인했나요?

### 30초 답변

DB GROUP BY 집계 결과의 가맹점 수와 saveAll 후 실제 저장된 Settlement 수를 비교했습니다. 테스트에서는 두 값이 같아야 성공하도록 했고, 100,000건 API 검증에서도 100건이 저장되는 것을 확인했습니다.

### 1분 답변

GROUP_BY_BULK_SAVE는 Payment 전체 Entity가 아니라 가맹점별 집계 결과를 기준으로 Settlement를 생성합니다. 따라서 집계 결과 건수와 생성 Settlement 수, 저장 Settlement 수가 같아야 합니다. 이 값을 자동 테스트로 검증했고, 수동 API 검증에서도 Payment 100,000건 기준으로 GROUP_BY_BULK_SAVE가 정산 결과 100건을 생성하는 것을 BatchJobHistory와 API 응답으로 확인했습니다.

---

## Hibernate batch_size는 왜 다음 단계로 분리했나요?

### 30초 답변

saveAll, Hibernate batch_size, PostgreSQL JDBC 옵션을 한 번에 넣으면 어떤 변경이 성능에 영향을 줬는지 설명하기 어렵기 때문입니다. 포트폴리오에서는 개선 원인을 단계별로 보여주는 것이 중요해서 saveAll만 먼저 적용했습니다.

### 1분 답변

이번 프로젝트는 단순히 빠르게 만드는 것보다 어떤 병목을 어떤 방식으로 줄였는지 설명할 수 있어야 합니다. saveAll과 Hibernate batch_size, PostgreSQL `reWriteBatchedInserts`를 한 번에 적용하면 성능 차이가 나도 원인을 분리하기 어렵습니다. 그래서 GROUP_BY_BULK_SAVE 1차에서는 saveAll만 적용하고 저장 건수와 금액 정합성을 검증했습니다. 다음 단계에서 batch_size와 JDBC 옵션을 각각 적용해 실제 효과가 있는지 별도로 측정할 계획입니다.

---

## 성능 개선 과정에서 정합성을 어떻게 유지했나요?

### 30초 답변

각 개선 전략이 같은 Payment 데이터와 같은 정산일자를 기준으로 같은 금액 결과를 내는지 확인했습니다. 중복 기준은 가맹점, 정산일자, 처리 전략으로 유지했고, 실패 시 Settlement는 롤백하고 BatchJobHistory는 FAILED로 남겼습니다.

### 1분 답변

성능 개선 단계마다 계산식은 바꾸지 않고 조회와 저장 방식만 분리해서 변경했습니다. BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE 모두 같은 결제금액, 취소금액, 수수료, 최종 정산금액을 내는지 자동 테스트와 API 검증으로 확인했습니다. 또한 같은 날짜와 같은 전략의 재실행은 중복으로 차단하고, 실패가 발생하면 Settlement는 전체 롤백되며 BatchJobHistory에는 실패 상태와 원인이 남도록 유지했습니다.
