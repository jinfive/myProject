# 정산 배치 성능 개선 프로젝트

## 1. 프로젝트 개요

이 프로젝트는 대용량 결제 데이터를 일별로 정산하는 배치 처리 과정에서 병목을 확인하고, 정산 전략을 단계적으로 개선한 금융 거래 정산 배치 성능 개선 프로젝트입니다.

핵심 도메인은 다음과 같습니다.

| 도메인 | 역할 |
|---|---|
| `Payment` | 결제/취소 원천 거래 데이터 |
| `Merchant` | 가맹점과 수수료율 정보 |
| `Settlement` | 가맹점별 일자별 정산 결과 |
| `BatchJobHistory` | 배치 실행 상태, 처리 건수, 실행 시간, 실패 원인 기록 |

## 2. 해결하려 한 문제

대용량 결제 데이터를 정산할 때 다음 문제가 발생할 수 있다고 보고 개선을 진행했습니다.

- 전체 Payment 데이터를 애플리케이션으로 조회하는 비용
- 성능 개선 후 정산 금액이 달라질 수 있는 정합성 리스크
- 같은 날짜와 같은 전략의 중복 정산 위험
- 실패 시 원인과 실행 상태를 추적할 필요성
- Settlement 저장 건수가 늘어날 때 발생하는 저장 성능 병목

## 3. 정산 전략

| 전략 | 목적 | 처리 방식 | 의미 |
|---|---|---|---|
| `BASIC_LOOP` | 성능 비교 기준선 | Payment 전체 조회 후 Java 반복문 집계 | 가장 단순한 기준선으로 개선 효과 비교에 사용 |
| `GROUP_BY_QUERY` | 조회 병목 제거 | DB `GROUP BY`로 가맹점별 금액 집계 | Payment 전체 Entity 조회를 제거 |
| `GROUP_BY_BULK_SAVE` | 저장 방식 개선 실험 | `GROUP_BY_QUERY` 집계 결과 재사용 후 Settlement 저장 방식 변경 | 조회 개선 후 남는 저장 병목을 분리해 확인 |

## 4. 안정성 설계

### 트랜잭션 롤백

- 정산 중 오류가 발생하면 Settlement 일부만 저장되는 상황을 막습니다.
- 정산 결과 저장은 실패 시 롤백되도록 처리했습니다.

### 중복 정산 방지

- 중복 기준은 `merchant_id + settlement_date + processing_strategy`입니다.
- 같은 날짜라도 다른 전략의 결과는 저장할 수 있습니다.
- 같은 날짜 + 같은 전략 재실행은 차단합니다.

### BatchJobHistory

- `RUNNING`, `SUCCESS`, `FAILED` 상태를 기록합니다.
- 실패 시 `errorMessage`를 기록합니다.
- `BatchJobHistory` 저장은 `REQUIRES_NEW`로 분리해 정산 실패 후에도 실패 이력을 보존합니다.

## 5. 성능 개선 흐름

1. 10만 건 기준선 실험
   - `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`를 같은 데이터 조건에서 비교했습니다.

2. 100만 건 확장 실험
   - Merchant 수를 5,000개로 늘려 조회 병목과 저장 결과 증가를 함께 확인했습니다.

3. 1000만 건 단일 날짜 실험
   - 단일 날짜에 1000만 건이 몰린 조건에서 DB GROUP BY 기반 전략을 측정했습니다.

4. `work_mem` 실험
   - `HashAggregate` temp spill을 확인하고 세션 단위 `work_mem` 변경 효과를 비교했습니다.

5. 날짜 분산 데이터셋 재설계
   - 1000만 건을 2026년 5월 날짜 범위에 분산해 날짜 조건 선택도를 다시 검증했습니다.

6. 인덱스 실험
   - 단순 날짜 인덱스와 covering index를 비교하며 실행계획을 확인했습니다.

7. JPQL `count(p)` 문제 분석
   - 수동 SQL과 API SQL의 실행계획 차이를 Hibernate SQL 로그로 확인했습니다.

8. native query + `count(*)` + interface projection 적용
   - API 경로에서도 `Index Only Scan`이 사용되도록 집계 쿼리를 분리했습니다.

9. Hibernate `batch_size` 실험
   - `saveAll`만으로 실제 bulk insert가 되는지 SQL 로그와 구간별 시간으로 확인했습니다.

10. IDENTITY -> SEQUENCE 변경
    - `Settlement.id`를 SEQUENCE 전략으로 변경해 저장 구간과 API 전체 시간을 비교했습니다.

### GROUP_BY_QUERY 쿼리 구조 개선

초기 `GROUP_BY_QUERY`는 `payments`와 `merchants`를 먼저 조인한 뒤 `m.id`, `m.name`, `m.fee_rate` 기준으로 GROUP BY를 수행했습니다. 1000만 건 조건에서는 조인 이후 중간 데이터가 커지고, GROUP BY 과정에서 temp spill이 크게 발생했습니다.

정산에 필요한 핵심 집계는 `payments` 테이블의 `merchant_id` 기준 결제/취소 금액 합계라고 판단했습니다. 그래서 `payments`를 먼저 `merchant_id` 기준으로 집계하고, 줄어든 집계 결과만 `merchants`와 조인하는 구조로 변경했습니다.

| 구분 | 기존 방식 | 개선 방식 |
|---|---|---|
| 처리 순서 | `payments + merchants` 조인 후 GROUP BY | `payments` 선집계 후 `merchants` 조인 |
| 집계 기준 | `m.id`, `m.name`, `m.fee_rate` | `p.merchant_id` |
| 처리 건수 계산 | `count(p.id)` | `count(*)` |
| 의도 | merchant 정보 포함 상태로 집계 | 중간 데이터 크기와 temp spill 감소 |

| 항목 | 개선 전 | 개선 후 |
|---|---:|---:|
| EXPLAIN 실행 시간 | 3,855ms | 3,274ms |
| API `GROUP_BY_QUERY` | 5,026ms | 4,796ms |
| API `GROUP_BY_BULK_SAVE` | 4,596ms | 4,149ms |
| temp written | 92,954 | 47,200 |

두 전략의 정산 합계 동일성을 확인했고, 중복 실행 시 `409 Conflict` 동작과 `./gradlew test` 통과 기록도 유지했습니다.

## 6. 최종 성능 결과

### 100만 건 확장 실험

| 데이터 건수 | Merchant 수 | 전략 | 실행 시간 | 결과 동일성 |
|---:|---:|---|---:|---|
| 1,000,000 | 5,000 | `BASIC_LOOP` | 8,253ms | 기준 |
| 1,000,000 | 5,000 | `GROUP_BY_QUERY` | 899ms | BASIC_LOOP와 동일 |
| 1,000,000 | 5,000 | `GROUP_BY_BULK_SAVE` | 798ms | BASIC_LOOP와 동일 |

100만 건 조건에서는 Payment 전체 조회 방식보다 DB GROUP BY 집계 방식이 더 짧은 실행 시간을 보였습니다. 세 전략의 Settlement 수와 금액 합계는 동일했습니다.

### 날짜 분산 1000만 건 최종 개선 결과

| 항목 | 기본 조회 | covering index + native `count(*)` | 개선 효과 |
|---|---:|---:|---:|
| EXPLAIN | 2,242.501ms | 74.988ms | 2,167.513ms 감소 |
| API `GROUP_BY_QUERY` | 3,021.333ms | 1,627.667ms | 1,393.666ms 감소 |
| API `GROUP_BY_BULK_SAVE` | 2,478.333ms | 2,058.000ms | 420.333ms 감소 |

수동 EXPLAIN에서만 빨라진 것이 아니라, API 실제 SQL을 `count(*)`로 맞춘 뒤 API 경로에서도 개선 효과를 확인했습니다.

### SEQUENCE 변경 후 저장 성능

| 전략 | ID 전략 | DB 집계조회 평균 | 저장 평균 | API 전체 평균 |
|---|---|---:|---:|---:|
| `GROUP_BY_QUERY` | IDENTITY | 297.837ms | 1,105.225ms | 1,490.271ms |
| `GROUP_BY_BULK_SAVE` | IDENTITY | 276.100ms | 975.471ms | 1,289.364ms |
| `GROUP_BY_QUERY` | SEQUENCE | 258.032ms | 101.945ms | 755.662ms |
| `GROUP_BY_BULK_SAVE` | SEQUENCE | 281.536ms | 40.449ms | 696.352ms |

IDENTITY 전략은 insert 직후 generated id 조회가 필요해 batch insert에 불리했습니다. SEQUENCE 변경 후 저장 구간과 API 전체 시간이 줄었습니다.

## 7. 정합성 검증

성능 개선 후에도 정산 결과가 동일한지 다음 기준으로 확인했습니다.

- `processedCount=322,581` 확인
- Settlement 10,000건 확인
- 결제금액, 취소금액, 수수료, 최종 정산금액 동일성 확인
- `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE` 결과 동일성 검증
- 실패 시 Settlement 롤백 확인
- 실패 시 BatchJobHistory `FAILED`와 `errorMessage` 기록 확인

날짜 분산 1000만 건 최종 검증 기준 합계는 다음과 같습니다.

| 항목 | 값 |
|---|---:|
| 결제금액 | 27,066,846,805.00 |
| 취소금액 | 3,387,078,413.00 |
| 수수료 | 447,339,420.65 |
| 최종 정산금액 | 23,232,428,971.35 |

## 8. 트러블슈팅

### 1. JPQL `count(p)`로 Index Only Scan이 깨진 문제

| 구분 | 내용 |
|---|---|
| 문제 | 수동 SQL은 `count(*)`라 `Index Only Scan`이 되었지만, API에서는 JPQL `count(p)`가 SQL `count(p.id)`로 실행됐습니다. |
| 원인 | covering index에 `id`가 없어 DB가 heap 접근을 수행했습니다. |
| 해결 | native query에서 `count(*)`를 명시하고 interface projection으로 결과를 받도록 변경했습니다. |
| 결과 | API 경로에서도 `Index Only Scan`, `Heap Fetches=0`을 확인했습니다. |

### 2. `saveAll`이 실제 bulk insert가 아니었던 문제

| 구분 | 내용 |
|---|---|
| 문제 | `saveAll` 사용 후에도 `insert into settlements (...) values (...)`가 개별 반복으로 실행됐습니다. |
| 원인 | Hibernate batch 설정이 없었고, `Settlement.id`의 IDENTITY 전략도 batch insert에 불리했습니다. |
| 해결 | Hibernate `jdbc.batch_size=1000`, `order_inserts=true`, `order_updates=true`를 실험하고, ID 전략을 IDENTITY에서 SEQUENCE로 변경했습니다. |
| 결과 | SEQUENCE 변경 후 `GROUP_BY_QUERY` API 전체 평균은 755.662ms, `GROUP_BY_BULK_SAVE` API 전체 평균은 696.352ms로 측정됐습니다. |
