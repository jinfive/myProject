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

## BASIC_LOOP 정산 배치

BASIC_LOOP는 실무 적용 방식이 아니라 성능 개선 전 기준선을 만들기 위해 의도적으로 구현했습니다. 이 단계에서는 GROUP BY 쿼리, 인덱스 튜닝, 벌크 저장 최적화를 적용하지 않고, 특정 정산일자의 Payment 데이터를 모두 조회한 뒤 Java 반복문으로 가맹점별 금액을 집계합니다.

실무 정산 배치라면 전체 Payment 데이터를 애플리케이션으로 가져오는 방식보다 DB GROUP BY 집계가 더 적절하다고 판단했습니다. 그래서 같은 정산일자에 대해 `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`, `GROUP_BY_BULK_INDEX`를 단계형 개선 전략으로 비교할 수 있도록 처리 전략을 정리했습니다.

처음에는 하루 정산 결과가 중복 저장되는 것을 막기 위해 정산일자 기준으로 재실행을 차단했습니다. 하지만 성능 비교를 위해 같은 정산일자라도 처리 전략별 결과를 각각 저장할 필요가 있다고 판단했습니다. 그래서 중복 기준을 정산일자 단위에서 가맹점 + 정산일자 + 처리 전략 단위로 변경했습니다.

또한 배치가 실패해도 원인을 추적할 수 있도록 BatchJobHistory를 별도 트랜잭션으로 관리해 `RUNNING`, `SUCCESS`, `FAILED` 상태와 실패 원인을 남기도록 개선했습니다. 정산 결과는 실패 시 전체 롤백되도록 유지해 일부 가맹점 결과만 저장되는 문제를 막았습니다.

처리 전략:

| 전략 | 의미 | 현재 실행 여부 |
|---|---|---|
| `BASIC_LOOP` | 전체 Payment 조회 후 Java 반복문 집계, 개별 저장 | 구현 |
| `GROUP_BY_QUERY` | DB GROUP BY 집계, 개별 저장 | 예정 |
| `GROUP_BY_BULK_SAVE` | DB GROUP BY 집계 + 일괄 저장 | 예정 |
| `GROUP_BY_BULK_INDEX` | DB GROUP BY 집계 + 일괄 저장 + 조회 조건 인덱스 | 예정 |

API 목록:

```text
POST /api/settlements/run?date=2026-05-08&strategy=BASIC_LOOP
GET  /api/settlements?date=2026-05-08
GET  /api/batch-histories
```

프론트에서 확인:

```bash
./gradlew bootRun

cd frontend
npm run dev
```

`http://localhost:5173`에서 정산일자 `2026-05-08`, 처리 전략 `BASIC_LOOP`을 선택한 뒤 정산 배치 실행 버튼을 누르면 백엔드 API를 호출하고 정산 결과와 배치 실행 이력을 갱신합니다.

DB에서 직접 확인:

```sql
select count(*) from settlements
where settlement_date = '2026-05-08'
  and processing_strategy = 'BASIC_LOOP';

select * from batch_job_histories order by started_at desc;
```

마이그레이션 도구를 아직 사용하지 않기 때문에 기존 로컬 DB가 있으면 스키마 보정이 필요합니다. 기존 `settlements`, `batch_job_histories` 데이터가 있는 상태에서 Hibernate가 `processing_strategy not null` 컬럼을 바로 추가하면 PostgreSQL에서 `contains null values` 오류가 발생할 수 있습니다.

애플리케이션 시작 시 로컬 PostgreSQL 스키마 보정 러너가 기존 데이터를 `BASIC_LOOP`로 backfill하고 `processing_strategy`의 `NOT NULL`, check constraint, `merchant_id + settlement_date + processing_strategy` unique 제약을 적용합니다. 직접 확인하거나 수동으로 처리해야 하는 경우 [docs/generated/db-schema.md](docs/generated/db-schema.md)의 수동 마이그레이션 SQL을 사용합니다. 코드에서는 `processingStrategy`를 nullable로 낮추지 않고 DB 데이터를 보정해 NOT NULL을 유지합니다.

또한 BatchJobHistory는 `RUNNING` 상태를 먼저 저장한 뒤 성공/실패 시 종료 시간을 채우므로 `batch_job_histories.ended_at`은 nullable이어야 합니다. 기존 로컬 DB에 예전 `ended_at NOT NULL` 제약이 남아 있으면 스키마 보정 러너가 이를 해제합니다.

기존 로컬 DB에 예전 `batch_job_histories.strategy` 컬럼이 남아 있고 `NOT NULL` 제약이 설정되어 있으면, 현재 코드는 `processing_strategy`만 쓰기 때문에 INSERT가 실패할 수 있습니다. 스키마 보정 러너는 이 예전 컬럼의 `NOT NULL` 제약도 해제합니다.

BatchJobHistory에 `RUNNING` 상태가 추가되면서 기존 로컬 DB의 status check constraint가 새 enum 값을 허용하지 않아 오류가 발생할 수 있습니다. Hibernate `ddl-auto=update`는 기존 check constraint를 자동으로 안전하게 수정하지 못할 수 있으므로, 스키마 보정 러너가 기존 status check constraint를 제거하고 `RUNNING`, `SUCCESS`, `FAILED`를 허용하는 새 check constraint를 추가합니다. `batch_job_histories`는 실행 이력이므로 삭제하지 않습니다.

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
