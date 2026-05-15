# myProject

## 프로젝트 방향

이 프로젝트는 대용량 결제 데이터를 일별로 정산하는 배치 처리 과정에서 성능 병목을 확인하고, 처리 전략 개선을 통해 실행 시간을 줄이는 것을 목표로 합니다.

프론트 화면은 정산 담당자가 배치 실행 결과와 성능 개선 지표를 확인하기 위한 대시보드로 구현했습니다. 프로젝트의 핵심은 화면 구현보다 대용량 정산 배치 처리 성능 개선에 있습니다.

## 프론트 실행

프론트는 `frontend` 디렉터리의 React + Vite 프로젝트로 구성되어 있습니다.

```bash
cd frontend
npm install
npm run dev
```

개발 서버 기본 주소는 `http://localhost:5173`입니다.

## 로컬 개발 CORS

로컬 개발에서는 Vite 프론트엔드(`http://localhost:5173`)와 Spring Boot 백엔드(`http://localhost:8080`)가 서로 다른 origin으로 실행됩니다.

백엔드는 `/api/**` 경로에 대해 로컬 개발 origin만 허용합니다.

- `http://localhost:5173`
- `http://127.0.0.1:5173`

정산 결과 초기화 API는 브라우저에서 `DELETE /api/settlements?date=...` 요청을 보내며, DELETE 요청 전 OPTIONS preflight가 발생합니다. 따라서 로컬 CORS 설정에는 `GET`, `POST`, `PUT`, `PATCH`, `DELETE`, `OPTIONS`를 포함합니다.

운영 환경에서는 `allowedOrigins("*")`처럼 전체 origin을 허용하지 않고, 배포된 프론트엔드 주소에 맞는 별도 origin 정책을 적용해야 합니다.

## 성능 테스트용 더미 데이터 생성

애플리케이션 실행 시 성능 테스트용 더미 데이터를 자동 생성합니다. 이미 `Merchant` 또는 `Payment` 데이터가 존재하면 중복 생성을 방지하기 위해 생성 작업을 건너뜁니다.

기본 로컬 DB는 PostgreSQL의 `settlement_perf_db`를 사용합니다.

기본 생성 기준은 다음과 같습니다.

- Merchant: 100개
- Payment: 100,000건
- 성능 테스트 기준일: `2026-05-08`
- `2026-05-08` Payment: 70,000건
- 결제/취소 데이터 모두 `COMPLETED` 상태로 포함
- 각 Merchant는 서로 다른 수수료율을 가짐

이 기준은 1차 성능 비교를 위한 작은 벤치마크 조건입니다. 다음 실험에서는 Payment와 Merchant 수를 함께 늘려 조회 병목과 저장 병목을 분리해서 확인합니다.

| 단계 | Payment 수 | Merchant 수 | 목적 | 측정 전략 |
|---:|---:|---:|---|---|
| 1 | 100,000 | 100 | 기준 실험 완료, Payment 전체 조회 병목 확인 | BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |
| 2 | 1,000,000 | 5,000 | 중간 확장, 정합성·실행 시간·메모리 부담 점검 | BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |
| 3 | 10,000,000 | 5,000~10,000 | 최종 대용량 검증, 조회 병목과 저장 병목 재확인 | GROUP_BY_QUERY, GROUP_BY_BULK_SAVE |

100만 건 benchmark-medium 데이터셋은 구현 완료했습니다. `benchmark-medium` 프로파일을 사용하면 기존 benchmark 데이터를 추가하지 않고 `settlements`, `payments`, `merchants`를 초기화한 뒤 Merchant 5,000개와 Payment 1,000,000건을 오늘 날짜 기준으로 재생성합니다. `batch_job_histories`는 실행 이력이므로 삭제하지 않습니다.

1000만 건에서는 `BASIC_LOOP`를 필수 전략으로 보지 않고, DB GROUP BY 기반 전략 중심으로 정합성과 실행 시간을 비교합니다.

실행 방법:

```bash
./gradlew bootRun
```

생성 여부와 건수는 애플리케이션 로그에서 확인할 수 있습니다.

```text
Dummy data generation started at ...
Dummy data generation finished at ...
Dummy data generation elapsedMs=...
Merchant total count=100
Payment total count=100000
Payment count for 2026-05-08=70000
```

DB에서 직접 확인할 때는 다음 쿼리를 사용할 수 있습니다.

```sql
select count(*) from merchants;
select count(*) from payments;
select count(*) from payments where transaction_date = '2026-05-08';
```

## 로컬 벤치마크 날짜 동기화

대용량 성능 비교를 반복하기 위해 매번 Payment 데이터를 새로 생성하면 시간이 오래 걸리고 실험 조건도 달라질 수 있다고 판단했습니다.

그래서 로컬 벤치마크 환경에서는 기존 Payment 데이터를 재사용하되, 정산 기준일만 오늘 날짜로 동기화하도록 했습니다. 이 기능은 운영 기능이 아니라 개발용 벤치마크 데이터 재사용 기능입니다.

정책:

- Payment 데이터가 없으면 기존 더미 데이터 생성 흐름만 유지합니다.
- Payment 데이터가 이미 있고 모든 `transaction_date`가 오늘 날짜이면 아무 작업도 하지 않습니다.
- 하나라도 오늘 날짜가 아닌 Payment가 있으면 기존 `settlements`를 삭제한 뒤 `payments.transaction_date`를 오늘 날짜로 벌크 변경합니다.
- `batch_job_histories`는 실행 이력이므로 삭제하지 않고 보존합니다.

설정:

```yaml
benchmark:
  data-date-sync-enabled: true
```

운영이나 외부 공개 환경에서는 `benchmark.data-date-sync-enabled=false`로 비활성화할 수 있습니다.

100만 건과 1000만 건 실험은 데이터 규모를 코드에 고정하지 않고 설정이나 프로파일로 분리합니다.

```yaml
benchmark:
  profile: medium
  reset-enabled: true
  merchant-count: 5000
  payment-count: 1000000
  batch-size: 1000
  target-date-sync-enabled: true
```

실행 예시:

```bash
./gradlew bootRun --args='--spring.profiles.active=benchmark-medium'
```

현재 프로파일은 `benchmark-small`(기본값, 100,000 payments / 100 merchants), `benchmark-medium`(1,000,000 payments / 5,000 merchants)을 사용합니다. `benchmark-large`(10,000,000 payments / 10,000 merchants)는 아직 구현하지 않고 다음 단계로 남겼습니다. 기존 `payments`와 `merchants`는 무단 삭제하지 않고, `benchmark.reset-enabled=true`처럼 명확한 설정값을 켠 경우에만 재생성합니다. `batch_job_histories`는 실행 이력이므로 계속 보존합니다.

## 정산 배치 처리 전략

BASIC_LOOP는 실무 적용 방식이 아니라 성능 개선 전 기준선을 만들기 위해 의도적으로 구현했습니다. 이 단계에서는 GROUP BY 쿼리, 인덱스 튜닝, 벌크 저장 최적화를 적용하지 않고, 특정 정산일자의 Payment 데이터를 모두 조회한 뒤 Java 반복문으로 가맹점별 금액을 집계합니다.

실무 정산 배치라면 전체 Payment 데이터를 애플리케이션으로 가져오는 방식보다 DB GROUP BY 집계가 더 적절하다고 판단했습니다. 그래서 같은 정산일자에 대해 `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`, `GROUP_BY_BULK_INDEX`를 단계형 개선 전략으로 비교할 수 있도록 처리 전략을 정리했습니다.

`GROUP_BY_QUERY`에서는 `payments` 전체 Entity 목록을 조회하지 않고, DB GROUP BY로 가맹점별 결제금액과 취소금액, 완료 처리 건수를 먼저 집계합니다. Java에서는 집계 결과만 받아 BASIC_LOOP와 같은 반올림 정책으로 수수료와 최종 정산금액을 계산합니다. 이 단계의 개선 효과를 분리해서 보기 위해 Settlement 저장은 BASIC_LOOP와 동일하게 개별 저장으로 유지했습니다.

`GROUP_BY_BULK_SAVE` 1차 구현에서는 GROUP_BY_QUERY와 같은 DB GROUP BY 집계 결과를 재사용하면서 Settlement 저장 방식을 개별 `save` 반복에서 `saveAll` 기반 저장으로 변경했습니다. 저장 방식 변경으로 일부 결과가 누락되거나 금액이 달라질 수 있다고 보고, 집계 결과 건수, 생성 Settlement 수, 실제 저장 Settlement 수와 전략별 금액 동일성을 테스트했습니다. 이번 단계에서는 `saveAll`만 적용했고 Hibernate `batch_size`와 PostgreSQL `reWriteBatchedInserts=true`는 다음 단계에서 별도로 검토합니다.

처음에는 하루 정산 결과가 중복 저장되는 것을 막기 위해 정산일자 기준으로 재실행을 차단했습니다. 하지만 성능 비교를 위해 같은 정산일자라도 처리 전략별 결과를 각각 저장할 필요가 있다고 판단했습니다. 그래서 중복 기준을 정산일자 단위에서 가맹점 + 정산일자 + 처리 전략 단위로 변경했습니다.

또한 배치가 실패해도 원인을 추적할 수 있도록 BatchJobHistory를 별도 트랜잭션으로 관리해 `RUNNING`, `SUCCESS`, `FAILED` 상태와 실패 원인을 남기도록 개선했습니다. 정산 결과는 실패 시 전체 롤백되도록 유지해 일부 가맹점 결과만 저장되는 문제를 막았습니다.

처리 전략:

| 전략 | 의미 | 현재 실행 여부 |
|---|---|---|
| `BASIC_LOOP` | 전체 Payment 조회 후 Java 반복문 집계, 개별 저장 | 구현 |
| `GROUP_BY_QUERY` | DB GROUP BY 집계, 개별 저장 | 구현 |
| `GROUP_BY_BULK_SAVE` | DB GROUP BY 집계 + saveAll 저장 | 구현 |
| `GROUP_BY_BULK_INDEX` | DB GROUP BY 집계 + 일괄 저장 + 조회 조건 인덱스 | 예정 |

## 1차 성능 측정 결과

BASIC_LOOP는 성능 개선 전 기준선으로, 특정 정산일자의 Payment 데이터를 모두 애플리케이션으로 조회한 뒤 Java 반복문으로 가맹점별 금액을 집계했습니다.

이 방식은 구현은 단순하지만 데이터가 증가할수록 Entity 로딩과 반복 집계가 병목이 될 수 있다고 판단했습니다.

GROUP_BY_QUERY에서는 DB GROUP BY를 사용해 가맹점별 결제금액과 취소금액을 DB에서 직접 집계하도록 변경했습니다.

Java에서는 Payment 전체 목록이 아니라 집계 결과만 받아 수수료와 최종 정산금액을 계산하도록 했고, 동일한 데이터 조건에서 BASIC_LOOP와 GROUP_BY_QUERY의 정산 결과 금액이 동일한지 검증했습니다.

측정 조건:

- 데이터 건수: 100,000건
- 정산일자: 2026-05-14
- 측정 기준: `batch_job_histories.elapsed_ms`
- 결과 동일성 기준: 전략별 Settlement 합계 비교
- 측정 방식: 같은 날짜의 `settlements`만 삭제하고 `batch_job_histories`는 보존한 상태에서 API로 반복 실행
- 주의: 로컬 단일 DB에서 측정한 값이며, DB/OS 캐시와 JVM warm-up 상태에 따라 흔들릴 수 있으므로 단일 실행값이 아니라 반복 측정 범위로 해석한다.

| 데이터 건수 | 전략 | 핵심 처리 방식 | 실행 시간 | 정산 결과 |
|---:|---|---|---:|---|
| 100,000 | BASIC_LOOP | Payment 전체 조회 후 Java 반복 집계 | 728~1043ms | 기준 |
| 100,000 | GROUP_BY_QUERY | DB GROUP BY 집계 후 결과만 조회 | 104~265ms | BASIC_LOOP와 동일 |
| 100,000 | GROUP_BY_BULK_SAVE | DB GROUP BY 집계 후 saveAll 저장 | 110ms | BASIC_LOOP와 동일 |

동일 조건 대표 측정값은 BASIC_LOOP 882ms, GROUP_BY_QUERY 133ms, GROUP_BY_BULK_SAVE 110ms입니다. 이 실험에서는 Payment 전체 조회 방식이 비효율적이고, DB GROUP BY 집계가 가장 큰 개선 효과를 만든다는 점을 확인했습니다.

다만 Merchant 수가 100개라 Settlement 저장 결과도 100건 수준이었습니다. 따라서 `GROUP_BY_BULK_SAVE`의 saveAll 기반 저장 최적화 효과는 제한적으로만 확인되었고, Hibernate `batch_size`와 PostgreSQL JDBC `reWriteBatchedInserts=true` 적용 여부는 100만 건과 1000만 건 실험에서 저장 건수가 늘어난 뒤 다시 판단합니다.

반복 측정 이력:

| 회차 | 실행 순서 | BASIC_LOOP | GROUP_BY_QUERY |
|---:|---|---:|---:|
| 1 | BASIC_LOOP → GROUP_BY_QUERY | 1043ms | 127ms |
| 2 | BASIC_LOOP → GROUP_BY_QUERY | 773ms | 104ms |
| 3 | BASIC_LOOP → GROUP_BY_QUERY | 774ms | 265ms |
| 4 | GROUP_BY_QUERY → BASIC_LOOP | 728ms | 171ms |

전략별 합계 검증 결과:

| 전략 | 정산 결과 수 | 총 결제금액 | 총 취소금액 | 총 수수료 | 총 정산금액 |
|---|---:|---:|---:|---:|---:|
| BASIC_LOOP | 100 | 8,372,918,761.00 | 1,052,006,411.00 | 145,575,451.69 | 7,175,336,898.31 |
| GROUP_BY_QUERY | 100 | 8,372,918,761.00 | 1,052,006,411.00 | 145,575,451.69 | 7,175,336,898.31 |
| GROUP_BY_BULK_SAVE | 100 | 8,372,918,761.00 | 1,052,006,411.00 | 145,575,451.69 | 7,175,336,898.31 |

## benchmark-medium 측정 결과

100만 건 / Merchant 5,000개 조건에서는 기존 10만 건 데이터에 추가하지 않고 benchmark-medium 데이터셋으로 재생성했습니다. 기존 데이터에 90만 건을 추가하면 Merchant 분포가 섞여 실험 조건이 불명확해지기 때문입니다.

데이터 생성 결과:

| 항목 | 값 |
|---|---:|
| Merchant | 5,000 |
| Payment | 1,000,000 |
| 정산일자 | 2026-05-15 |
| 정산일자 Payment | 1,000,000 |
| 데이터 재생성 시간 | 96,314ms |
| BatchJobHistory | 보존 |

성능 측정 결과:

| 데이터 건수 | Merchant 수 | 전략 | 실행 시간 | 결과 동일성 |
|---:|---:|---|---:|---|
| 1,000,000 | 5,000 | BASIC_LOOP | 8,253ms | 기준 |
| 1,000,000 | 5,000 | GROUP_BY_QUERY | 899ms | BASIC_LOOP와 동일 |
| 1,000,000 | 5,000 | GROUP_BY_BULK_SAVE | 798ms | BASIC_LOOP와 동일 |

전략별 합계 검증 결과:

| 전략 | 정산 결과 수 | 총 결제금액 | 총 취소금액 | 총 수수료 | 총 정산금액 |
|---|---:|---:|---:|---:|---:|
| BASIC_LOOP | 5,000 | 83,994,777,240.00 | 10,515,004,210.00 | 1,388,102,869.22 | 72,091,670,160.78 |
| GROUP_BY_QUERY | 5,000 | 83,994,777,240.00 | 10,515,004,210.00 | 1,388,102,869.22 | 72,091,670,160.78 |
| GROUP_BY_BULK_SAVE | 5,000 | 83,994,777,240.00 | 10,515,004,210.00 | 1,388,102,869.22 | 72,091,670,160.78 |

같은 날짜와 같은 전략의 재실행은 `409 Conflict`로 차단되는 것을 확인했습니다.

API 목록:

```text
POST /api/settlements/run?date=2026-05-08&strategy=BASIC_LOOP
POST /api/settlements/run?date=2026-05-08&strategy=GROUP_BY_QUERY
POST /api/settlements/run?date=2026-05-08&strategy=GROUP_BY_BULK_SAVE
GET  /api/settlements?date=2026-05-08
GET  /api/batch-histories
```

프론트에서 확인:

```bash
./gradlew bootRun

cd frontend
npm run dev
```

`http://localhost:5173`에서 정산일자 `2026-05-08`, 처리 전략 `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`를 선택한 뒤 정산 배치 실행 버튼을 누르면 백엔드 API를 호출하고 정산 결과와 배치 실행 이력을 갱신합니다. `GROUP_BY_BULK_INDEX`는 아직 구현하지 않았으므로 프론트 선택 목록에서 비활성화했습니다.

DB에서 직접 확인:

```sql
select count(*) from settlements
where settlement_date = '2026-05-08'
  and processing_strategy = 'BASIC_LOOP';

select * from batch_job_histories order by started_at desc;
```

BASIC_LOOP와 GROUP_BY_QUERY 결과 금액 비교:

```sql
select
    merchant_id,
    processing_strategy,
    total_payment_amount,
    total_cancel_amount,
    net_sales_amount,
    fee_amount,
    final_settlement_amount
from settlements
where settlement_date = '2026-05-08'
  and processing_strategy in ('BASIC_LOOP', 'GROUP_BY_QUERY')
order by merchant_id, processing_strategy;
```

마이그레이션 도구를 아직 사용하지 않기 때문에 기존 로컬 DB가 있으면 스키마 보정이 필요합니다. 기존 `settlements`, `batch_job_histories` 데이터가 있는 상태에서 Hibernate가 `processing_strategy not null` 컬럼을 바로 추가하면 PostgreSQL에서 `contains null values` 오류가 발생할 수 있습니다.

애플리케이션 시작 시 로컬 PostgreSQL 스키마 보정 러너가 기존 데이터를 `BASIC_LOOP`로 backfill하고 `processing_strategy`의 `NOT NULL`, check constraint, `merchant_id + settlement_date + processing_strategy` unique 제약을 적용합니다. 직접 확인하거나 수동으로 처리해야 하는 경우 [docs/generated/db-schema.md](docs/generated/db-schema.md)의 수동 마이그레이션 SQL을 사용합니다. 코드에서는 `processingStrategy`를 nullable로 낮추지 않고 DB 데이터를 보정해 NOT NULL을 유지합니다.

또한 BatchJobHistory는 `RUNNING` 상태를 먼저 저장한 뒤 성공/실패 시 종료 시간을 채우므로 `batch_job_histories.ended_at`은 nullable이어야 합니다. 기존 로컬 DB에 예전 `ended_at NOT NULL` 제약이 남아 있으면 스키마 보정 러너가 이를 해제합니다.

기존 로컬 DB에 예전 `batch_job_histories.strategy` 컬럼이 남아 있고 `NOT NULL` 제약이 설정되어 있으면, 현재 코드는 `processing_strategy`만 쓰기 때문에 INSERT가 실패할 수 있습니다. 스키마 보정 러너는 이 예전 컬럼의 `NOT NULL` 제약도 해제합니다.

BatchJobHistory에 `RUNNING` 상태가 추가되면서 기존 로컬 DB의 status check constraint가 새 enum 값을 허용하지 않아 오류가 발생할 수 있습니다. Hibernate `ddl-auto=update`는 기존 check constraint를 자동으로 안전하게 수정하지 못할 수 있으므로, 스키마 보정 러너가 기존 status check constraint를 제거하고 `RUNNING`, `SUCCESS`, `FAILED`를 허용하는 새 check constraint를 추가합니다. `batch_job_histories`는 실행 이력이므로 삭제하지 않습니다.

현재 로컬 DB 제약조건 확인 SQL:

```sql
select
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
from pg_constraint
where conrelid = 'settlements'::regclass;

select
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
from pg_constraint
where conrelid = 'batch_job_histories'::regclass;
```

## 앞으로의 구현 순서

이 프로젝트는 단순 기능 추가가 아니라 정산 배치 성능 개선 과정을 포트폴리오로 설명하기 위한 프로젝트입니다. 앞으로의 작업은 다음 순서로 진행합니다.

```txt
1. 10만 건 / Merchant 100개 기준 실험 정리
2. 100만 건 / Merchant 5,000개 중간 확장 실험
3. 1000만 건 / Merchant 5,000~10,000개 최종 대용량 실험
4. 저장 병목 확인 시 Hibernate batch_size 적용 검토
5. batch 설정 후에도 저장 병목이 남으면 PostgreSQL reWriteBatchedInserts 적용 검토
6. 1000만 건에서 조회 병목이 확인되면 GROUP_BY_BULK_INDEX와 인덱스 적용 검토
7. EXPLAIN ANALYZE로 실행 계획 확인
8. 대사 기능 추가
9. RUNNING 중복 실행 방지
10. 날짜 파티셔닝은 고도화 항목으로 문서화
```

각 단계는 독립된 개선 액션으로 관리합니다. 한 단계가 끝나면 README의 진행 기록에 문제, 판단, 액션, 검증, 결과, 다음 작업을 남기고, 성능 수치가 있으면 실제 측정값만 기록합니다. 수치가 아직 없으면 `측정 예정`으로 남깁니다.

진행 순서의 기준:

- 10만 건 실험은 성능 개선 전 기준선과 DB GROUP BY 집계 효과를 확인한 완료된 기준 실험입니다.
- 100만 건 실험은 1000만 건으로 가기 전에 데이터 생성, 정합성, 실행 시간, 메모리 부담을 점검하는 중간 단계입니다.
- 1000만 건 실험은 대용량 정산 배치 개선을 설명하기 위한 최종 검증 단계입니다.
- Hibernate batch_size와 PostgreSQL reWriteBatchedInserts는 Settlement 저장 건수가 5,000건 이상으로 늘어난 뒤 saveAll만으로 저장 성능 개선이 제한적일 때 검토합니다.
- GROUP_BY_BULK_INDEX와 인덱스 적용은 1000만 건에서 GROUP_BY_QUERY가 수 초 이상 걸리거나 `EXPLAIN ANALYZE`에서 전체 스캔 비용이 크다고 확인될 때 검토합니다.
- 인덱스 후보는 실제 컬럼 기준으로 `payments(transaction_date, status, merchant_id)` 조합을 우선 검토합니다.
- EXPLAIN ANALYZE는 인덱스가 실제 실행 계획에서 사용되는지 검증하는 단계입니다.
- 대사 기능은 원천 Payment와 Settlement 결과가 일치하는지 검증하는 금융권 정합성 근거입니다.
- RUNNING 중복 실행 방지는 같은 날짜와 같은 전략의 배치가 동시에 실행되어 DB 부하와 충돌을 만드는 문제를 막는 안정성 개선입니다.
- 날짜 파티셔닝은 현재 범위에서 실제 구현하지 않고, 장기 데이터 누적에 대비한 고도화 항목으로 문서화합니다.

## 진행 기록

앞으로 각 작업이 끝날 때마다 다음 형식으로 기록합니다. 이 기록은 최종 README와 PPT를 만들 때 원천 자료로 사용합니다.

```md
### YYYY-MM-DD: 작업명

#### 문제

현재 어떤 문제가 있었는지 작성한다.

#### 판단

왜 이 작업이 필요하다고 판단했는지 작성한다.

#### 액션

실제로 어떤 구현을 했는지 작성한다.

#### 검증

어떤 테스트, 수동 검증, 성능 측정을 했는지 작성한다.

#### 결과

확인된 결과를 작성한다. 수치가 있으면 실제 측정값을 쓰고, 아직 없으면 `측정 예정`으로 남긴다.

#### 다음 작업

다음에 이어서 할 작업을 작성한다.
```

### 2026-05-13: GROUP_BY_QUERY 구현 및 결과 동일성 검증

#### 문제

BASIC_LOOP는 특정 정산일자의 Payment 전체를 애플리케이션으로 가져와 Java 반복문으로 가맹점별 금액을 집계하는 기준선입니다. 기준선으로는 적절하지만 데이터가 증가할수록 Entity 로딩과 애플리케이션 반복 집계가 병목이 될 수 있습니다.

#### 판단

실무 정산 배치에서는 전체 Payment를 애플리케이션으로 가져오는 방식보다 DB GROUP BY로 필요한 집계 결과만 조회하는 방식이 더 적절하다고 판단했습니다. 다만 개선 효과를 분리해서 보기 위해 이번 단계에서는 벌크 저장과 인덱스 적용을 제외했습니다.

#### 액션

GROUP_BY_QUERY 전략을 추가해 DB에서 가맹점별 결제금액, 취소금액, 완료 처리 건수를 집계하도록 했습니다. Java에서는 집계 결과만 받아 BASIC_LOOP와 같은 반올림 정책으로 수수료와 최종 정산금액을 계산하고, Settlement 저장은 개별 저장으로 유지했습니다.

#### 검증

`./gradlew test`로 BASIC_LOOP와 GROUP_BY_QUERY의 `totalPaymentAmount`, `totalCancelAmount`, `netSalesAmount`, `feeAmount`, `finalSettlementAmount` 동일성을 검증했습니다. 같은 날짜의 BASIC_LOOP와 GROUP_BY_QUERY는 각각 실행 가능하고, 같은 날짜의 GROUP_BY_QUERY 중복 실행은 차단되는 것도 확인했습니다.

로컬 PostgreSQL에서 100,000건 Payment를 2026-05-14로 동기화한 뒤 API로 BASIC_LOOP와 GROUP_BY_QUERY를 각각 실행했습니다. BatchJobHistory의 `elapsedMs`와 Settlement 합계를 기준으로 처리 시간과 결과 동일성을 확인했습니다.

#### 결과

GROUP_BY_QUERY 구현과 자동 테스트는 통과했습니다. 100,000건 기준 재측정에서 BASIC_LOOP는 728~1043ms, GROUP_BY_QUERY는 104~265ms 범위로 측정되었습니다. 두 전략 모두 정산 결과 100건을 생성했고 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액이 동일했습니다.

#### 다음 작업

GROUP_BY_BULK_SAVE를 구현해 GROUP BY 집계는 유지하면서 Settlement 개별 저장 호출을 일괄 저장 방식으로 줄입니다.

### 2026-05-14: GROUP_BY_BULK_SAVE 1차 구현 및 저장 정합성 검증

#### 문제

GROUP_BY_QUERY는 Payment 전체 조회 병목을 줄였지만, Settlement 저장은 여전히 개별 `save` 반복 구조였습니다. 집계 결과 수가 커지면 저장 단계도 병목이 될 수 있다고 판단했습니다.

#### 판단

이번 단계에서는 저장 최적화 효과를 과장하지 않기 위해 Hibernate batch 설정이나 PostgreSQL JDBC 옵션을 함께 넣지 않고, 먼저 `saveAll`만 적용했습니다. 저장 방식이 바뀌면 일부 Settlement 누락이나 금액 불일치가 생길 수 있으므로 저장 건수와 금액 정합성 검증을 우선했습니다.

#### 액션

`GROUP_BY_BULK_SAVE` 전략을 추가하고 GROUP_BY_QUERY와 같은 DB GROUP BY 집계 결과를 재사용했습니다. 생성한 Settlement 목록은 개별 저장하지 않고 `settlementRepository.saveAll(settlements)`로 저장하도록 분리했습니다. 프론트에서도 `GROUP_BY_BULK_SAVE`를 실행 가능하게 변경했습니다.

#### 검증

`./gradlew test`로 집계 결과 건수와 저장 Settlement 수가 일치하는지, BASIC_LOOP/GROUP_BY_QUERY/GROUP_BY_BULK_SAVE의 가맹점별 금액이 동일한지 검증했습니다. 또한 GROUP_BY_BULK_SAVE 실패 시 Settlement는 롤백되고 BatchJobHistory는 FAILED와 errorMessage를 남기는지 확인했습니다.

로컬 API 기준으로 2026-05-14 정산 결과를 초기화한 뒤 BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE를 순서대로 실행했습니다. 같은 날짜와 같은 전략의 GROUP_BY_BULK_SAVE 재실행은 중복 실행으로 실패하고 FAILED 이력이 남는 것을 확인했습니다.

#### 결과

100,000건 기준 동일 조건 1회 측정에서 BASIC_LOOP는 882ms, GROUP_BY_QUERY는 133ms, GROUP_BY_BULK_SAVE는 110ms로 기록되었습니다. 세 전략 모두 정산 결과 100건을 생성했고 총 결제금액, 총 취소금액, 총 수수료, 총 정산금액이 동일했습니다.

#### 다음 작업

Hibernate `batch_size`, `order_inserts`, PostgreSQL JDBC `reWriteBatchedInserts=true` 적용 여부를 별도 단계로 분리해 측정합니다. 이후 GROUP_BY_BULK_INDEX에서 조회 조건 기준 인덱스와 실행 계획을 검토합니다.

## 최종 README 정리 기준

모든 단계가 끝나면 README는 포트폴리오 제출용으로 다음 흐름에 맞춰 재정리합니다.

```txt
1. 프로젝트 개요
2. 문제 상황
3. 데이터 규모
4. 정산 계산 기준
5. 처리 전략 비교
   - BASIC_LOOP
   - GROUP_BY_QUERY
   - GROUP_BY_BULK_SAVE
   - GROUP_BY_BULK_INDEX
6. 성능 비교 결과
7. 실행 계획 분석 결과
8. 대사 기능과 정합성 검증
9. 안정성 개선
   - BatchJobHistory
   - 실패 이력
   - RUNNING 중복 실행 방지
10. 기술적 의사결정
11. 한계와 고도화 방향
   - 날짜 파티셔닝
   - chunk 처리
   - 실패 건 재처리
12. 실행 방법
13. 테스트 방법
14. 포트폴리오 요약
```

## PPT 산출 기준

최종 PPT는 README의 진행 기록과 성능 측정 결과를 기반으로 작성합니다.

```txt
1장. 프로젝트 개요
2장. 문제 상황: BASIC_LOOP 기준선
3장. 개선 1: GROUP_BY_QUERY
4장. 개선 2: GROUP_BY_BULK_SAVE
5장. 개선 3: GROUP_BY_BULK_INDEX + EXPLAIN ANALYZE
6장. 정합성 검증: 대사 기능
7장. 안정성 개선: BatchJobHistory + RUNNING 중복 실행 방지
8장. 성능 비교 결과표
9장. 배운 점과 고도화 방향
```

각 장표는 `문제 → 판단 → 액션 → 검증 → 결과` 흐름으로 작성할 수 있어야 합니다.

## 고도화 항목: 날짜 파티셔닝

정산 배치는 특정 정산일자 기준으로 대량 Payment를 반복 조회합니다. 데이터가 월 단위, 연 단위로 장기 누적되면 `payments.transaction_date` 기준 파티셔닝을 검토할 수 있습니다.

다만 현재 포트폴리오 범위에서는 GROUP BY, Bulk Save, Index, EXPLAIN ANALYZE, 대사, RUNNING 중복 실행 방지까지 구현하고, 날짜 파티셔닝은 실제 구현이 아니라 고도화 방향으로 문서화합니다.

## 브랜치 전략

본 프로젝트는 기능 단위 개발 흐름을 관리하기 위해 `main`, `develop`, `feat` 브랜치를 분리하여 사용합니다.

- `main`: 최종 배포 가능한 안정 버전 관리
- `develop`: 기능 개발 결과를 통합하는 브랜치
- `feat/{issue-number}-{feature-name}`: GitHub Issue 단위 기능 개발 브랜치

기능 개발은 GitHub Issue로 작업 범위를 정의한 뒤 `feat` 브랜치에서 구현하고, Pull Request를 통해 `develop` 브랜치에 병합합니다. 최종 기능 검증 후 `develop` 브랜치를 `main` 브랜치에 병합하는 방식으로 관리합니다.

### 개발 흐름

```text
main
└── develop
    └── feat/{issue-number}-{feature-name}
```

1. GitHub Issue 생성
2. `develop` 브랜치에서 `feat/{issue-number}-{feature-name}` 브랜치 생성
3. 기능 구현 및 커밋
4. 기능 브랜치를 원격 저장소에 push
5. Pull Request 생성
   - base: `develop`
   - compare: `feat/{issue-number}-{feature-name}`
6. 리뷰 및 테스트 후 `develop` 브랜치에 병합
7. 최종 검증 후 `develop`에서 `main`으로 Pull Request 생성 및 병합

### 브랜치 예시

```text
feat/1-project-setup
feat/2-merchant-create
feat/3-payment-create
feat/4-payment-cancel
feat/5-daily-settlement
feat/6-prevent-duplicate-settlement
feat/7-batch-history
feat/8-settlement-read
feat/9-exception-test
feat/10-readme
```
