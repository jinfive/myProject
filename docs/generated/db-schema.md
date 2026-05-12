# Database Schema

## 1. 목적

이 문서는 **금융 거래 처리 성능 최적화 포트폴리오 프로젝트**의 데이터베이스 스키마를 정리하기 위한 문서이다.

이 프로젝트는 하나의 저장소 안에서 다음 두 단계를 구현한다.

```txt
1단계: 대용량 금융 거래 정산·대사 배치 성능 최적화
2단계: 실시간 주식 시세 API 기반 모의 주문·체결 처리 시스템
```

따라서 이 문서는 다음 두 영역의 테이블 구조를 함께 관리한다.

- 1단계 배치 정산 성능 개선 테이블
- 2단계 증권 실시간 주문·체결 처리 확장 테이블

현재는 1단계인 **정산 배치 성능 비교용 MVP 중 BASIC_LOOP 기준선 구현 완료** 상태이다.

현재 구현된 핵심 도메인은 다음과 같다.

- Merchant
- Payment
- Settlement
- BatchJobHistory

향후 1단계 고도화에서 추가할 도메인은 다음과 같다.

- SettlementError 또는 BatchError
- ReconciliationResult

2단계 증권 실시간 처리에서 추가할 도메인은 다음과 같다.

- Account
- Stock
- StockPrice
- StockOrder
- Execution
- Holding
- CashBalance
- AccountTransaction
- OrderEvent 또는 ExecutionEvent

이 문서는 테이블, 컬럼, 관계, 인덱스, 제약조건, enum 값을 한눈에 확인할 수 있도록 관리한다.

---

## 2. 문서 역할

이 문서는 다음 작업 전에 참고한다.

- 테이블 추가
- 컬럼 추가, 수정, 삭제
- 외래키 관계 변경
- 인덱스 추가 또는 삭제
- unique 제약조건 변경
- enum 값 변경
- API 응답에 필요한 데이터 확인
- 정산 결과 정합성 검토
- 체결 결과와 잔고/예수금 정합성 검토
- 성능 개선 전후 비교
- 마이그레이션 작성
- DB 관련 오류 분석
- README와 PPT용 ERD 정리

---

## 3. 작성 원칙

이 문서는 단순히 테이블 목록만 정리하지 않는다.

포트폴리오와 PPT에서 설명할 수 있도록 다음 관점으로 작성한다.

```txt
어떤 문제가 있었는가
→ 왜 이 테이블이나 제약조건이 필요한가
→ 어떤 정합성을 지키기 위한 구조인가
→ 어떤 성능 병목을 줄이기 위한 인덱스인가
→ 어떻게 검증할 것인가
```

예를 들어 `payments.payment_date` 인덱스는 단순히 조회를 빠르게 하기 위한 것이 아니라, 다음 문제를 해결하기 위한 구조이다.

```txt
문제:
정산 배치는 특정 정산일자의 대량 결제 데이터를 반복 조회한다.

판단:
정산일자는 배치 조회 조건에서 가장 자주 사용되므로 인덱스가 필요하다.

액션:
payment_date 또는 payment_date + merchant_id 복합 인덱스를 적용한다.

검증:
동일한 데이터와 동일한 정산일자로 인덱스 적용 전후 처리 시간을 비교한다.
```

---

## 4. 현재 구현 상태

현재 구현된 기능은 다음과 같다.

- Spring Boot 백엔드 구조 구성
- Merchant, Payment, Settlement, BatchJobHistory 도메인 구현
- 더미 데이터 자동 생성
    - 가맹점 100개
    - 결제 데이터 100,000건
    - 특정일 거래 약 70,000건
- BASIC_LOOP 정산 배치 구현
- 정산 실행 API 구현
    - `POST /api/settlements/run`
- 정산 결과 조회 API 구현
    - `GET /api/settlements`
- 배치 이력 조회 API 구현
    - `GET /api/batch-histories`
- React 대시보드 구현
- 프론트 빌드 성공
- 백엔드 테스트 명령 성공

현재 부족한 부분은 다음과 같다.

- GROUP BY 기반 개선 전략 미구현
- 벌크 저장 최적화 미구현
- 인덱스 적용 및 성능 비교 미구현
- 테스트 부족
- DB 설정 불일치
- 실제 테이블 기준 DB 문서 갱신 필요
- 실패 이력 저장 부족
- 재실행 정책 부족
- 대사 기능 미구현

이 문서는 현재 구현된 테이블과 향후 추가할 테이블을 구분해서 관리한다.

---

## 5. 전체 테이블 목록

### 5.1 1단계 배치 정산 테이블

| 테이블명 | 상태 | 설명 |
|---|---|---|
| `merchants` | Implemented | 가맹점 정보와 수수료율 |
| `payments` | Implemented | 결제/취소 원천 거래 데이터 |
| `settlements` | Implemented | 가맹점별 정산 결과 |
| `batch_job_histories` | Implemented | 정산 배치 실행 이력 |
| `settlement_errors` | Planned | 정산 실패 이력 |
| `reconciliation_results` | Planned | 정산 결과와 원천 데이터 대사 결과 |

---

### 5.2 2단계 증권 실시간 처리 테이블

2단계는 아직 구현하지 않는다.

다만 프로젝트 기획상 다음 테이블을 향후 추가 대상으로 관리한다.

| 테이블명 | 상태 | 설명 |
|---|---|---|
| `accounts` | Planned | 모의 증권 계좌 |
| `stocks` | Planned | 종목 정보 |
| `stock_prices` | Planned | 실시간 또는 모의 시세 데이터 |
| `stock_orders` | Planned | 매수/매도 주문 |
| `executions` | Planned | 체결 내역 |
| `holdings` | Planned | 계좌별 보유 종목 |
| `cash_balances` | Planned | 계좌별 예수금 |
| `account_transactions` | Planned | 예수금 및 보유 수량 변경 이력 |
| `order_events` | Optional | 주문 이벤트 기록 |
| `execution_events` | Optional | 체결 이벤트 기록 |

---

# 6. 1단계 배치 정산 테이블 상세

## merchants

### 설명

가맹점 정보를 저장하는 테이블이다.

정산 배치에서 가맹점별 수수료율을 적용하기 위해 사용한다.

### 문제와 설계 이유

정산금액은 가맹점별 수수료율에 따라 달라진다.

따라서 결제 데이터만으로는 최종 정산금액을 계산할 수 없고, 가맹점별 수수료율 정보를 별도 테이블로 관리해야 한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 가맹점 ID |
| name | varchar | No |  | 가맹점명 |
| fee_rate | decimal | No |  | 가맹점 수수료율 |
| created_at | timestamp | No | current timestamp | 생성 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_merchants | id | Primary Key | 기본 키 |
| uk_merchants_name | name | Unique | 가맹점명 중복 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_merchants_name | name | Index | 가맹점명 검색 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| payments | 1:N | 한 가맹점은 여러 결제/취소 데이터를 가진다 |
| settlements | 1:N | 한 가맹점은 여러 정산 결과를 가진다 |

### 비고

- 수수료율은 금액 계산에 직접 영향을 준다.
- 수수료 계산은 `double`, `float`가 아니라 `BigDecimal` 기준으로 처리한다.
- 수수료율 변경 이력이 필요해지면 별도 `merchant_fee_histories` 테이블을 검토한다.

---

## payments

### 설명

결제와 취소 원천 거래 데이터를 저장하는 테이블이다.

1단계 배치 성능 개선 프로젝트에서 가장 많은 데이터가 저장되는 핵심 테이블이다.

### 문제와 설계 이유

정산 배치는 특정 정산일자의 대량 결제/취소 데이터를 조회해 가맹점별로 집계한다.

현재 BASIC_LOOP 방식은 특정 날짜의 Payment 데이터를 모두 조회한 뒤 Java 반복문으로 집계한다.

향후 GROUP_BY_QUERY 방식에서는 이 테이블을 기준으로 DB에서 직접 가맹점별 금액을 집계한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 결제/취소 데이터 ID |
| merchant_id | bigint | No |  | 가맹점 ID |
| payment_date | date | No |  | 결제 또는 취소 기준일 |
| amount | decimal | No |  | 거래 금액 |
| payment_type | varchar | No |  | 거래 유형 |
| payment_status | varchar | No |  | 거래 상태 |
| created_at | timestamp | No | current timestamp | 생성 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_payments | id | Primary Key | 기본 키 |
| fk_payments_merchant_id | merchant_id | Foreign Key | merchants.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_payments_payment_date | payment_date | Index | 정산일자 기준 조회 최적화 |
| idx_payments_merchant_id | merchant_id | Index | 가맹점별 조회 최적화 |
| idx_payments_type | payment_type | Index | 결제/취소 유형별 조회 |
| idx_payments_status | payment_status | Index | 완료 상태 데이터 조회 |
| idx_payments_date_merchant | payment_date, merchant_id | Composite Index | 정산일자 + 가맹점별 집계 최적화 |
| idx_payments_date_merchant_type | payment_date, merchant_id, payment_type | Composite Index | 정산일자 + 가맹점 + 결제/취소 유형 집계 최적화 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| merchants | N:1 | 결제/취소 데이터는 특정 가맹점에 속한다 |

### 비고

- 실제 결제 승인 기능을 위한 테이블이 아니다.
- 대용량 정산 배치 성능 테스트를 위한 원천 거래 데이터이다.
- 현재 더미 데이터는 100,000건 이상 생성된다.
- 특정일에 약 70,000건이 몰리도록 구성되어 성능 병목을 확인할 수 있다.
- GROUP BY 개선 전략의 핵심 조회 대상이다.

---

## settlements

### 설명

가맹점별 정산 결과를 저장하는 테이블이다.

정산일자, 가맹점, 처리 전략 기준으로 정산 결과를 저장한다.

### 문제와 설계 이유

성능 비교 프로젝트에서는 같은 정산일자에 대해 여러 처리 전략을 실행할 수 있어야 한다.

예를 들어 같은 날짜에 대해 다음 전략을 각각 실행하고 결과와 시간을 비교해야 한다.

```txt
BASIC_LOOP
GROUP_BY_QUERY
GROUP_BY_BULK_SAVE
GROUP_BY_BULK_INDEX
```

따라서 중복 정산 방지 기준은 단순히 `merchant_id + settlement_date`만으로 잡으면 안 된다.

성능 비교를 위해서는 `merchant_id + settlement_date + processing_strategy` 기준으로 중복을 제어하는 것이 더 적절하다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 정산 결과 ID |
| merchant_id | bigint | No |  | 가맹점 ID |
| settlement_date | date | No |  | 정산일자 |
| processing_strategy | varchar | No |  | 정산 처리 전략 |
| total_payment_amount | decimal | No | 0 | 총 결제금액 |
| total_cancel_amount | decimal | No | 0 | 총 취소금액 |
| net_sales_amount | decimal | No | 0 | 순매출 |
| fee_amount | decimal | No | 0 | 수수료 |
| settlement_amount | decimal | No | 0 | 최종 정산금액 |
| created_at | timestamp | No | current timestamp | 생성 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_settlements | id | Primary Key | 기본 키 |
| fk_settlements_merchant_id | merchant_id | Foreign Key | merchants.id 참조 |
| uk_settlements_merchant_date_strategy | merchant_id, settlement_date, processing_strategy | Unique | 같은 가맹점, 같은 정산일자, 같은 전략의 중복 정산 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_settlements_settlement_date | settlement_date | Index | 정산일자별 결과 조회 |
| idx_settlements_merchant_id | merchant_id | Index | 가맹점별 정산 결과 조회 |
| idx_settlements_strategy | processing_strategy | Index | 처리 전략별 결과 조회 |
| idx_settlements_date_strategy | settlement_date, processing_strategy | Composite Index | 정산일자 + 전략별 결과 조회 |
| idx_settlements_merchant_date | merchant_id, settlement_date | Composite Index | 중복 정산 검증 및 가맹점별 정산 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| merchants | N:1 | 정산 결과는 특정 가맹점에 속한다 |
| batch_job_histories | N:1 또는 간접 관계 | 정산 결과는 특정 배치 실행 이력과 연결될 수 있다 |

### 비고

정산 계산식은 다음과 같다.

```txt
순매출 = 총 결제금액 - 총 취소금액
수수료 = 순매출 × 가맹점 수수료율
정산금액 = 순매출 - 수수료
```

금액 계산에는 `BigDecimal`을 사용한다.

성능 비교를 위해 같은 날짜에 여러 전략 결과를 저장할 수 있도록 `processing_strategy`를 포함한다.

현재 마이그레이션 도구는 도입하지 않았다. 기존 로컬 DB가 있는 경우 Hibernate `ddl-auto: update`만으로는 `processing_strategy` NOT NULL 컬럼 추가가 실패할 수 있으므로 애플리케이션 시작 시 로컬 PostgreSQL 스키마 보정 러너가 아래와 같은 순서로 보정한다. 직접 확인하거나 수동으로 처리해야 하는 경우 아래 SQL을 실행한다.

기존 `settlements`, `batch_job_histories` 데이터는 BASIC_LOOP 기준선 실행으로 생성된 데이터로 간주하고 `processing_strategy = 'BASIC_LOOP'`로 backfill한다. 그 후 `NOT NULL`, check constraint, unique constraint를 적용한다. 코드에서는 `processingStrategy`를 nullable로 낮추지 않고, DB 데이터를 보정한 뒤 NOT NULL을 유지한다.

```sql
begin;

-- 1. 기존 데이터가 있는 테이블에 NOT NULL 컬럼을 바로 추가하지 않는다.
alter table settlements
    add column if not exists processing_strategy varchar(30);

alter table batch_job_histories
    add column if not exists processing_strategy varchar(30);

-- RUNNING 이력은 아직 종료 시간이 없으므로 ended_at은 nullable이어야 한다.
alter table batch_job_histories
    alter column ended_at drop not null;

-- 예전 스키마의 strategy 컬럼이 남아 있으면 현재 코드는 값을 쓰지 않으므로 nullable이어야 한다.
do $$
begin
    if exists (
        select 1
        from information_schema.columns
        where table_schema = current_schema()
          and table_name = 'batch_job_histories'
          and column_name = 'strategy'
    ) then
        alter table batch_job_histories
            alter column strategy drop not null;
    end if;
end $$;

-- 기존 status check constraint가 RUNNING을 허용하지 않으면 제거한다.
alter table batch_job_histories
    drop constraint if exists batch_job_histories_status_check;

-- 이름이 다른 status check constraint가 남아 있는 로컬 DB를 위한 보조 제거 로직이다.
do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select c.conname
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        join pg_namespace n on n.oid = t.relnamespace
        where n.nspname = current_schema()
          and t.relname = 'batch_job_histories'
          and c.contype = 'c'
          and exists (
              select 1
              from unnest(c.conkey) as cols(attnum)
              join pg_attribute a on a.attrelid = t.oid and a.attnum = cols.attnum
              where a.attname = 'status'
          )
    loop
        execute format('alter table batch_job_histories drop constraint %I', constraint_name);
    end loop;
end $$;

alter table batch_job_histories
    add constraint batch_job_histories_status_check
    check (status in ('RUNNING', 'SUCCESS', 'FAILED'));

-- 2. 기존 로컬 데이터는 BASIC_LOOP 기준선 결과로 backfill한다.
update settlements
set processing_strategy = 'BASIC_LOOP'
where processing_strategy is null;

update batch_job_histories
set processing_strategy = 'BASIC_LOOP'
where processing_strategy is null;

-- 3. 허용 전략 값만 저장되도록 check constraint를 적용한다.
alter table settlements
    drop constraint if exists ck_settlements_processing_strategy;

alter table settlements
    drop constraint if exists settlements_processing_strategy_check;

alter table settlements
    add constraint ck_settlements_processing_strategy
    check (processing_strategy in (
        'BASIC_LOOP',
        'GROUP_BY_QUERY',
        'GROUP_BY_BULK_SAVE',
        'GROUP_BY_BULK_INDEX'
    ));

alter table batch_job_histories
    drop constraint if exists ck_batch_job_histories_processing_strategy;

alter table batch_job_histories
    drop constraint if exists batch_job_histories_processing_strategy_check;

alter table batch_job_histories
    add constraint ck_batch_job_histories_processing_strategy
    check (processing_strategy in (
        'BASIC_LOOP',
        'GROUP_BY_QUERY',
        'GROUP_BY_BULK_SAVE',
        'GROUP_BY_BULK_INDEX'
    ));

-- 4. backfill 이후 NOT NULL을 적용한다.
alter table settlements
    alter column processing_strategy set not null;

alter table batch_job_histories
    alter column processing_strategy set not null;

-- 5. 기존 merchant_id + settlement_date unique 제약을 제거한다.
alter table settlements
    drop constraint if exists uk_settlements_merchant_settlement_date;

alter table settlements
    drop constraint if exists uk_settlements_merchant_date;

-- 이름이 다른 unique 제약이 남아 있는 로컬 DB를 위한 보조 제거 로직이다.
do $$
declare
    constraint_name text;
begin
    for constraint_name in
        select c.conname
        from pg_constraint c
        join pg_class t on t.oid = c.conrelid
        join pg_namespace n on n.oid = t.relnamespace
        where n.nspname = current_schema()
          and t.relname = 'settlements'
          and c.contype = 'u'
          and (
              select array_agg(a.attname::text order by u.ordinality)
              from unnest(c.conkey) with ordinality as u(attnum, ordinality)
              join pg_attribute a on a.attrelid = t.oid and a.attnum = u.attnum
          ) = array['merchant_id', 'settlement_date']
    loop
        execute format('alter table settlements drop constraint %I', constraint_name);
    end loop;
end $$;

-- 6. 전략별 정산 결과 저장을 허용하는 새 unique 제약을 적용한다.
do $$
begin
    if not exists (
        select 1
        from pg_constraint
        where conname = 'uk_settlements_merchant_date_strategy'
          and conrelid = 'settlements'::regclass
    ) then
        alter table settlements
            add constraint uk_settlements_merchant_date_strategy
            unique (merchant_id, settlement_date, processing_strategy);
    end if;
end $$;

commit;
```

마이그레이션 후 확인 쿼리:

```sql
select processing_strategy, count(*)
from settlements
group by processing_strategy;

select processing_strategy, count(*)
from batch_job_histories
group by processing_strategy;

select conname, pg_get_constraintdef(oid)
from pg_constraint
where conrelid = 'settlements'::regclass
  and contype = 'u';

select
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
from pg_constraint
where conrelid = 'batch_job_histories'::regclass;

select
    conname as constraint_name,
    pg_get_constraintdef(oid) as constraint_definition
from pg_constraint
where conrelid = 'settlements'::regclass;
```

---

## batch_job_histories

### 설명

정산 배치 실행 이력을 저장하는 테이블이다.

이 테이블은 포트폴리오에서 성능 개선 결과를 보여주는 핵심 테이블이다.

### 문제와 설계 이유

성능 개선 프로젝트에서는 단순히 정산 결과만 저장하면 안 된다.

각 전략을 실행했을 때 다음 정보를 남겨야 한다.

- 어떤 전략을 실행했는가
- 몇 건을 처리했는가
- 얼마나 걸렸는가
- 성공했는가
- 실패했다면 왜 실패했는가

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 배치 실행 이력 ID |
| settlement_date | date | No |  | 정산일자 |
| processing_strategy | varchar | No |  | 처리 전략 |
| status | varchar | No |  | 배치 상태 |
| total_count | bigint | No | 0 | 전체 처리 건수 |
| success_count | bigint | No | 0 | 성공 건수 |
| failure_count | bigint | No | 0 | 실패 건수 |
| started_at | timestamp | No |  | 배치 시작 일시 |
| ended_at | timestamp | Yes |  | 배치 종료 일시 |
| elapsed_millis | bigint | Yes |  | 총 실행 시간 |
| error_message | text | Yes |  | 전체 실패 원인 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_batch_job_histories | id | Primary Key | 기본 키 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_batch_histories_date | settlement_date | Index | 정산일자별 배치 이력 조회 |
| idx_batch_histories_strategy | processing_strategy | Index | 전략별 배치 이력 조회 |
| idx_batch_histories_status | status | Index | 성공/실패 상태별 조회 |
| idx_batch_histories_started_at | started_at | Index | 최신 배치 이력 조회 |
| idx_batch_histories_date_strategy | settlement_date, processing_strategy | Composite Index | 동일 조건의 성능 비교 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| settlement_errors | 1:N | 하나의 배치 실행은 여러 실패 이력을 가질 수 있다 |
| reconciliation_results | 1:N | 하나의 배치 실행은 여러 대사 결과를 가질 수 있다 |

### 비고

- 성능 비교 표는 이 테이블의 데이터를 기반으로 작성한다.
- README와 PPT에 들어갈 처리 시간은 `elapsed_millis`를 기준으로 한다.
- 실패해도 이력은 남아야 한다.

---

## settlement_errors

### 상태

Planned

### 설명

정산 배치 중 발생한 실패 이력을 저장하는 테이블이다.

### 문제와 설계 이유

정산 배치는 성공만 중요한 것이 아니다.

실패했을 때 어떤 가맹점에서, 어떤 정산일자에, 어떤 이유로 실패했는지 추적할 수 있어야 한다.

이 테이블은 안정성과 운영 관점을 보여주기 위해 필요하다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 실패 이력 ID |
| batch_job_history_id | bigint | No |  | 배치 실행 이력 ID |
| merchant_id | bigint | Yes |  | 실패한 가맹점 ID |
| settlement_date | date | No |  | 정산일자 |
| processing_strategy | varchar | No |  | 처리 전략 |
| error_code | varchar | Yes |  | 에러 코드 |
| error_message | text | No |  | 실패 원인 |
| retryable | boolean | No | false | 재처리 가능 여부 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_settlement_errors | id | Primary Key | 기본 키 |
| fk_settlement_errors_batch_id | batch_job_history_id | Foreign Key | batch_job_histories.id 참조 |
| fk_settlement_errors_merchant_id | merchant_id | Foreign Key | merchants.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_settlement_errors_batch_id | batch_job_history_id | Index | 배치별 실패 이력 조회 |
| idx_settlement_errors_date | settlement_date | Index | 정산일자별 실패 이력 조회 |
| idx_settlement_errors_merchant_id | merchant_id | Index | 가맹점별 실패 이력 조회 |
| idx_settlement_errors_retryable | retryable | Index | 재처리 가능 건 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| batch_job_histories | N:1 | 실패 이력은 특정 배치 실행에 속한다 |
| merchants | N:1 | 실패 이력은 특정 가맹점과 연결될 수 있다 |

### 비고

- 배치 전체 실패와 가맹점 단위 실패를 모두 기록할 수 있도록 `merchant_id`는 nullable로 둔다.
- 재처리 정책과 함께 설계한다.

---

## reconciliation_results

### 상태

Planned

### 설명

원천 Payment 데이터와 Settlement 결과를 비교한 대사 결과를 저장하는 테이블이다.

### 문제와 설계 이유

금융 프로젝트에서 중요한 것은 단순히 정산 결과를 만드는 것이 아니라, 그 결과가 원천 데이터와 일치하는지 검증하는 것이다.

대사 기능은 이 프로젝트의 데이터 정합성 메시지를 강화한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 대사 결과 ID |
| batch_job_history_id | bigint | No |  | 배치 실행 이력 ID |
| settlement_date | date | No |  | 정산일자 |
| processing_strategy | varchar | No |  | 처리 전략 |
| source_payment_amount | decimal | No | 0 | Payment 기준 총 결제금액 |
| source_cancel_amount | decimal | No | 0 | Payment 기준 총 취소금액 |
| settlement_payment_amount | decimal | No | 0 | Settlement 기준 총 결제금액 |
| settlement_cancel_amount | decimal | No | 0 | Settlement 기준 총 취소금액 |
| matched | boolean | No | false | 대사 일치 여부 |
| mismatch_reason | text | Yes |  | 불일치 원인 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_reconciliation_results | id | Primary Key | 기본 키 |
| fk_reconciliation_batch_id | batch_job_history_id | Foreign Key | batch_job_histories.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_reconciliation_date | settlement_date | Index | 정산일자별 대사 결과 조회 |
| idx_reconciliation_strategy | processing_strategy | Index | 전략별 대사 결과 조회 |
| idx_reconciliation_matched | matched | Index | 불일치 건 조회 |
| idx_reconciliation_date_strategy | settlement_date, processing_strategy | Composite Index | 정산일자 + 전략별 대사 결과 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| batch_job_histories | N:1 | 대사 결과는 특정 배치 실행과 연결된다 |

### 비고

대사 기준 예시는 다음과 같다.

```txt
Payment 기준 총 결제금액 = Settlement 기준 총 결제금액
Payment 기준 총 취소금액 = Settlement 기준 총 취소금액
```

불일치가 발생하면 원인을 기록한다.

---

# 7. 2단계 증권 실시간 처리 테이블 상세

2단계는 아직 구현하지 않는다.

하지만 기획은 다음 구조로 확정한다.

```txt
실시간 주식 시세 API 수신
→ 현재가 기반 모의 주문 생성
→ 주문 Queue 적재
→ 메모리 기반 OrderBook 처리
→ 가격 우선·시간 우선 체결
→ 주문 상태 변경
→ 잔고와 예수금 반영
→ 체결 결과 DB 저장
→ WebSocket 실시간 알림
→ 초당 주문 수 기준 성능 테스트
```

2단계 테이블은 향후 `realtime-trading-design.md`, `orderbook-design.md`, `trading-consistency-design.md` 작성 후 실제 구현에 맞게 갱신한다.

---

## accounts

### 상태

Planned

### 설명

모의 증권 계좌 정보를 저장하는 테이블이다.

### 문제와 설계 이유

실시간 주문·체결 프로젝트에서는 주문을 발생시키는 주체가 필요하다.

실제 사용자를 만들기보다 포트폴리오 목적에 맞게 모의 계좌를 사용한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 계좌 ID |
| account_number | varchar | No |  | 모의 계좌번호 |
| account_name | varchar | Yes |  | 계좌명 |
| status | varchar | No | ACTIVE | 계좌 상태 |
| created_at | timestamp | No | current timestamp | 생성 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_accounts | id | Primary Key | 기본 키 |
| uk_accounts_account_number | account_number | Unique | 계좌번호 중복 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_accounts_status | status | Index | 계좌 상태별 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| cash_balances | 1:1 | 한 계좌는 하나의 예수금 정보를 가진다 |
| holdings | 1:N | 한 계좌는 여러 종목 보유 정보를 가진다 |
| stock_orders | 1:N | 한 계좌는 여러 주문을 가진다 |

---

## stocks

### 상태

Planned

### 설명

주식 종목 정보를 저장하는 테이블이다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 종목 ID |
| stock_code | varchar | No |  | 종목 코드 |
| stock_name | varchar | No |  | 종목명 |
| market | varchar | Yes |  | 시장 구분 |
| active | boolean | No | true | 거래 가능 여부 |
| created_at | timestamp | No | current timestamp | 생성 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_stocks | id | Primary Key | 기본 키 |
| uk_stocks_stock_code | stock_code | Unique | 종목 코드 중복 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_stocks_stock_code | stock_code | Index | 종목 코드 조회 |
| idx_stocks_active | active | Index | 거래 가능 종목 조회 |

### 비고

예시 종목:

```txt
005930 삼성전자
000660 SK하이닉스
035420 NAVER
```

---

## stock_prices

### 상태

Planned

### 설명

실시간 주식 시세 API 또는 모의 시세 생성기를 통해 수신한 현재가 데이터를 저장하는 테이블이다.

### 문제와 설계 이유

2단계에서는 실제 시세 API 또는 모의 시세를 기준으로 주문 가격을 생성한다.

따라서 현재가 흐름을 저장하거나 최소한 마지막 현재가를 관리할 수 있어야 한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 시세 ID |
| stock_id | bigint | No |  | 종목 ID |
| price | decimal | No |  | 현재가 |
| volume | bigint | Yes |  | 거래량 |
| price_source | varchar | No |  | API 또는 MOCK |
| received_at | timestamp | No |  | 시세 수신 일시 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_stock_prices | id | Primary Key | 기본 키 |
| fk_stock_prices_stock_id | stock_id | Foreign Key | stocks.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_stock_prices_stock_id | stock_id | Index | 종목별 시세 조회 |
| idx_stock_prices_received_at | received_at | Index | 수신 시간별 조회 |
| idx_stock_prices_stock_received | stock_id, received_at | Composite Index | 종목별 최신 시세 조회 |

### 비고

- 실시간 화면은 매번 DB를 조회하기보다 메모리 캐시 또는 이벤트 기반 전달을 우선한다.
- DB 저장은 이력 관리와 검증 용도로 사용한다.
- 외부 API 실패 시 모의 시세 생성기로 대체할 수 있다.

---

## stock_orders

### 상태

Planned

### 설명

매수/매도 주문을 저장하는 테이블이다.

### 문제와 설계 이유

주문은 체결되기 전, 부분체결 중, 체결 완료, 취소, 거절 등 여러 상태를 가진다.

따라서 주문 상태와 잔여 수량을 명확히 관리해야 한다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 주문 ID |
| account_id | bigint | No |  | 계좌 ID |
| stock_id | bigint | No |  | 종목 ID |
| order_type | varchar | No |  | 매수/매도 |
| order_price | decimal | No |  | 주문 가격 |
| order_quantity | bigint | No |  | 주문 수량 |
| filled_quantity | bigint | No | 0 | 체결 수량 |
| remaining_quantity | bigint | No |  | 잔여 수량 |
| status | varchar | No | RECEIVED | 주문 상태 |
| rejected_reason | text | Yes |  | 주문 거절 사유 |
| ordered_at | timestamp | No |  | 주문 접수 일시 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_stock_orders | id | Primary Key | 기본 키 |
| fk_stock_orders_account_id | account_id | Foreign Key | accounts.id 참조 |
| fk_stock_orders_stock_id | stock_id | Foreign Key | stocks.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_stock_orders_account_id | account_id | Index | 계좌별 주문 조회 |
| idx_stock_orders_stock_id | stock_id | Index | 종목별 주문 조회 |
| idx_stock_orders_status | status | Index | 주문 상태별 조회 |
| idx_stock_orders_stock_status | stock_id, status | Composite Index | 종목별 미체결 주문 조회 |
| idx_stock_orders_ordered_at | ordered_at | Index | 주문 접수 순서 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| accounts | N:1 | 주문은 특정 계좌에 속한다 |
| stocks | N:1 | 주문은 특정 종목에 속한다 |
| executions | 1:N | 하나의 주문은 여러 체결을 가질 수 있다 |

### 비고

- 매수/매도 주문은 같은 테이블에서 `order_type`으로 구분한다.
- 부분체결을 위해 `filled_quantity`와 `remaining_quantity`를 관리한다.
- 실제 매칭은 DB 조회보다 메모리 기반 OrderBook에서 처리한다.

---

## executions

### 상태

Planned

### 설명

체결 내역을 저장하는 테이블이다.

### 문제와 설계 이유

체결은 주문 상태, 보유 수량, 예수금에 영향을 준다.

따라서 체결 내역은 주문 결과를 추적하고 정합성을 검증하기 위한 기준 데이터이다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 체결 ID |
| buy_order_id | bigint | No |  | 매수 주문 ID |
| sell_order_id | bigint | No |  | 매도 주문 ID |
| stock_id | bigint | No |  | 종목 ID |
| execution_price | decimal | No |  | 체결 가격 |
| execution_quantity | bigint | No |  | 체결 수량 |
| executed_at | timestamp | No |  | 체결 일시 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_executions | id | Primary Key | 기본 키 |
| fk_executions_buy_order_id | buy_order_id | Foreign Key | stock_orders.id 참조 |
| fk_executions_sell_order_id | sell_order_id | Foreign Key | stock_orders.id 참조 |
| fk_executions_stock_id | stock_id | Foreign Key | stocks.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_executions_stock_id | stock_id | Index | 종목별 체결 조회 |
| idx_executions_executed_at | executed_at | Index | 체결 시간 조회 |
| idx_executions_buy_order_id | buy_order_id | Index | 매수 주문별 체결 조회 |
| idx_executions_sell_order_id | sell_order_id | Index | 매도 주문별 체결 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| stock_orders | N:1 | 체결은 매수 주문과 매도 주문에 연결된다 |
| stocks | N:1 | 체결은 특정 종목에 속한다 |

### 비고

체결이 발생하면 다음 작업이 하나의 트랜잭션 흐름으로 처리되어야 한다.

```txt
Execution 저장
매수 주문 상태 변경
매도 주문 상태 변경
매수자 보유 수량 증가
매수자 예수금 감소
매도자 보유 수량 감소
매도자 예수금 증가
AccountTransaction 저장
ExecutionEvent 발행
```

---

## holdings

### 상태

Planned

### 설명

계좌별 보유 종목 수량을 저장하는 테이블이다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 보유 ID |
| account_id | bigint | No |  | 계좌 ID |
| stock_id | bigint | No |  | 종목 ID |
| quantity | bigint | No | 0 | 보유 수량 |
| average_price | decimal | Yes |  | 평균 매입 단가 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_holdings | id | Primary Key | 기본 키 |
| fk_holdings_account_id | account_id | Foreign Key | accounts.id 참조 |
| fk_holdings_stock_id | stock_id | Foreign Key | stocks.id 참조 |
| uk_holdings_account_stock | account_id, stock_id | Unique | 계좌별 종목 보유 정보 중복 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_holdings_account_id | account_id | Index | 계좌별 보유 종목 조회 |
| idx_holdings_stock_id | stock_id | Index | 종목별 보유 현황 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| accounts | N:1 | 보유 정보는 특정 계좌에 속한다 |
| stocks | N:1 | 보유 정보는 특정 종목에 속한다 |

### 비고

- 매수 체결 시 증가한다.
- 매도 체결 시 감소한다.
- 매도 주문 전 보유 수량 부족 여부를 확인한다.

---

## cash_balances

### 상태

Planned

### 설명

계좌별 예수금을 저장하는 테이블이다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 예수금 ID |
| account_id | bigint | No |  | 계좌 ID |
| available_amount | decimal | No | 0 | 주문 가능 금액 |
| locked_amount | decimal | No | 0 | 주문 중 묶인 금액 |
| updated_at | timestamp | Yes |  | 수정 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_cash_balances | id | Primary Key | 기본 키 |
| fk_cash_balances_account_id | account_id | Foreign Key | accounts.id 참조 |
| uk_cash_balances_account_id | account_id | Unique | 계좌별 예수금 정보 중복 방지 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_cash_balances_account_id | account_id | Index | 계좌별 예수금 조회 |

### 관계

| 대상 테이블 | 관계 | 설명 |
|---|---|---|
| accounts | 1:1 | 예수금은 특정 계좌와 연결된다 |

### 비고

- 매수 주문 접수 시 주문 가능 금액을 확인한다.
- 체결 시 예수금이 차감된다.
- 실제 증권 시스템처럼 주문 접수 시 금액을 잠그는 구조까지 확장할 수 있다.

---

## account_transactions

### 상태

Planned

### 설명

계좌의 예수금 또는 보유 수량 변경 이력을 저장하는 테이블이다.

### 문제와 설계 이유

체결 결과가 잔고와 예수금에 제대로 반영되었는지 추적하려면 변경 이력이 필요하다.

이 테이블은 2단계 증권 실시간 처리의 정합성 검증 근거가 된다.

### 컬럼

| 컬럼명 | 타입 | Null 허용 | 기본값 | 설명 |
|---|---|---:|---|---|
| id | bigint | No | auto increment | 거래 이력 ID |
| account_id | bigint | No |  | 계좌 ID |
| stock_id | bigint | Yes |  | 종목 ID |
| execution_id | bigint | Yes |  | 체결 ID |
| transaction_type | varchar | No |  | 거래 이력 유형 |
| amount | decimal | Yes |  | 예수금 변경 금액 |
| quantity | bigint | Yes |  | 보유 수량 변경 |
| created_at | timestamp | No | current timestamp | 생성 일시 |

### 제약조건

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| pk_account_transactions | id | Primary Key | 기본 키 |
| fk_account_transactions_account_id | account_id | Foreign Key | accounts.id 참조 |
| fk_account_transactions_stock_id | stock_id | Foreign Key | stocks.id 참조 |
| fk_account_transactions_execution_id | execution_id | Foreign Key | executions.id 참조 |

### 인덱스

| 이름 | 컬럼 | 유형 | 설명 |
|---|---|---|---|
| idx_account_transactions_account_id | account_id | Index | 계좌별 이력 조회 |
| idx_account_transactions_execution_id | execution_id | Index | 체결별 이력 조회 |
| idx_account_transactions_created_at | created_at | Index | 시간순 이력 조회 |

### 비고

- 체결 결과와 잔고/예수금 변경을 추적하기 위한 이력 테이블이다.
- 포트폴리오에서는 “체결 결과를 잔고와 예수금에 정합성 있게 반영했다”는 근거가 된다.

---

# 8. Enum 값

## 8.1 1단계 배치 정산 Enum

### payment_type

| 값 | 설명 |
|---|---|
| PAYMENT | 결제 |
| CANCEL | 취소 |

### payment_status

| 값 | 설명 |
|---|---|
| COMPLETED | 완료 |
| FAILED | 실패 |
| CANCELED | 취소 |

### processing_strategy

| 값 | 설명 |
|---|---|
| BASIC_LOOP | 전체 조회 후 Java 반복문 집계 기준선 |
| GROUP_BY_QUERY | DB GROUP BY 기반 집계 |
| GROUP_BY_BULK_SAVE | DB GROUP BY 기반 집계 후 정산 결과 일괄 저장 |
| GROUP_BY_BULK_INDEX | DB GROUP BY 기반 집계, 일괄 저장, 조회 조건 인덱스 적용 |

### batch_status

| 값 | 설명 |
|---|---|
| RUNNING | 실행 중 |
| SUCCESS | 성공 |
| FAILED | 실패 |

---

## 8.2 2단계 증권 실시간 처리 Enum

### account_status

| 값 | 설명 |
|---|---|
| ACTIVE | 활성 계좌 |
| SUSPENDED | 정지 계좌 |
| CLOSED | 해지 계좌 |

### price_source

| 값 | 설명 |
|---|---|
| API | 외부 주식 시세 API |
| MOCK | 모의 시세 생성기 |

### order_type

| 값 | 설명 |
|---|---|
| BUY | 매수 |
| SELL | 매도 |

### order_status

| 값 | 설명 |
|---|---|
| RECEIVED | 주문 접수 |
| PARTIAL_FILLED | 부분 체결 |
| FILLED | 전체 체결 |
| CANCELED | 취소 |
| REJECTED | 거절 |

### transaction_type

| 값 | 설명 |
|---|---|
| BUY_EXECUTION | 매수 체결 |
| SELL_EXECUTION | 매도 체결 |
| CASH_DEPOSIT | 예수금 입금 |
| CASH_WITHDRAW | 예수금 출금 |
| CASH_LOCK | 주문 금액 잠금 |
| CASH_RELEASE | 주문 금액 해제 |

---

# 9. 관계 요약

## 9.1 1단계 배치 정산 관계

```txt
merchants
├── payments
└── settlements

batch_job_histories
├── settlement_errors
└── reconciliation_results
```

| 기준 테이블 | 대상 테이블 | 관계 | 설명 |
|---|---|---|---|
| merchants | payments | 1:N | 한 가맹점은 여러 결제/취소 데이터를 가진다 |
| merchants | settlements | 1:N | 한 가맹점은 여러 정산 결과를 가진다 |
| batch_job_histories | settlement_errors | 1:N | 하나의 배치는 여러 실패 이력을 가질 수 있다 |
| batch_job_histories | reconciliation_results | 1:N | 하나의 배치는 여러 대사 결과를 가질 수 있다 |

---

## 9.2 2단계 증권 실시간 처리 관계

```txt
accounts
├── cash_balances
├── holdings
├── stock_orders
└── account_transactions

stocks
├── stock_prices
├── stock_orders
├── executions
└── holdings

stock_orders
└── executions

executions
└── account_transactions
```

| 기준 테이블 | 대상 테이블 | 관계 | 설명 |
|---|---|---|---|
| accounts | cash_balances | 1:1 | 한 계좌는 하나의 예수금 정보를 가진다 |
| accounts | holdings | 1:N | 한 계좌는 여러 종목 보유 정보를 가진다 |
| accounts | stock_orders | 1:N | 한 계좌는 여러 주문을 가진다 |
| accounts | account_transactions | 1:N | 한 계좌는 여러 잔고/예수금 변경 이력을 가진다 |
| stocks | stock_prices | 1:N | 한 종목은 여러 시세 데이터를 가진다 |
| stocks | stock_orders | 1:N | 한 종목은 여러 주문을 가진다 |
| stocks | executions | 1:N | 한 종목은 여러 체결을 가진다 |
| stocks | holdings | 1:N | 한 종목은 여러 계좌 보유 정보에 포함된다 |
| stock_orders | executions | 1:N | 하나의 주문은 여러 체결 내역을 가질 수 있다 |
| executions | account_transactions | 1:N | 하나의 체결은 여러 계좌 변경 이력을 만들 수 있다 |

---

# 10. 인덱스 요약

## 10.1 1단계 배치 정산 인덱스

| 테이블 | 인덱스 | 컬럼 | 목적 |
|---|---|---|---|
| payments | idx_payments_payment_date | payment_date | 정산일자 기준 조회 |
| payments | idx_payments_merchant_id | merchant_id | 가맹점별 조회 |
| payments | idx_payments_type | payment_type | 결제/취소 유형별 조회 |
| payments | idx_payments_status | payment_status | 완료 상태 조회 |
| payments | idx_payments_date_merchant | payment_date, merchant_id | 정산일자 + 가맹점별 집계 |
| payments | idx_payments_date_merchant_type | payment_date, merchant_id, payment_type | 정산일자 + 가맹점 + 유형별 집계 |
| settlements | idx_settlements_settlement_date | settlement_date | 정산일자별 결과 조회 |
| settlements | idx_settlements_date_strategy | settlement_date, processing_strategy | 정산일자 + 전략별 결과 조회 |
| settlements | idx_settlements_merchant_date | merchant_id, settlement_date | 중복 정산 검증 |
| batch_job_histories | idx_batch_histories_date_strategy | settlement_date, processing_strategy | 동일 조건 성능 비교 |
| reconciliation_results | idx_reconciliation_date_strategy | settlement_date, processing_strategy | 정산일자 + 전략별 대사 결과 조회 |

---

## 10.2 2단계 증권 실시간 처리 인덱스

| 테이블 | 인덱스 | 컬럼 | 목적 |
|---|---|---|---|
| stocks | idx_stocks_stock_code | stock_code | 종목 코드 조회 |
| stock_prices | idx_stock_prices_stock_received | stock_id, received_at | 종목별 최신 시세 조회 |
| stock_orders | idx_stock_orders_account_id | account_id | 계좌별 주문 조회 |
| stock_orders | idx_stock_orders_stock_status | stock_id, status | 종목별 미체결 주문 조회 |
| stock_orders | idx_stock_orders_ordered_at | ordered_at | 주문 접수 순서 조회 |
| executions | idx_executions_stock_id | stock_id | 종목별 체결 조회 |
| executions | idx_executions_executed_at | executed_at | 체결 시간 조회 |
| holdings | uk_holdings_account_stock | account_id, stock_id | 계좌별 종목 보유 정보 중복 방지 |
| cash_balances | uk_cash_balances_account_id | account_id | 계좌별 예수금 정보 중복 방지 |
| account_transactions | idx_account_transactions_account_id | account_id | 계좌별 이력 조회 |

---

# 11. 제약조건 요약

## 11.1 1단계 배치 정산 제약조건

| 테이블 | 제약조건 | 유형 | 설명 |
|---|---|---|---|
| merchants | pk_merchants | Primary Key | 가맹점 기본 키 |
| merchants | uk_merchants_name | Unique | 가맹점명 중복 방지 |
| payments | pk_payments | Primary Key | 결제/취소 데이터 기본 키 |
| payments | fk_payments_merchant_id | Foreign Key | merchants.id 참조 |
| settlements | pk_settlements | Primary Key | 정산 결과 기본 키 |
| settlements | fk_settlements_merchant_id | Foreign Key | merchants.id 참조 |
| settlements | uk_settlements_merchant_date_strategy | Unique | 가맹점 + 정산일자 + 전략 중복 방지 |
| batch_job_histories | pk_batch_job_histories | Primary Key | 배치 이력 기본 키 |
| settlement_errors | fk_settlement_errors_batch_id | Foreign Key | batch_job_histories.id 참조 |
| reconciliation_results | fk_reconciliation_batch_id | Foreign Key | batch_job_histories.id 참조 |

---

## 11.2 2단계 증권 실시간 처리 제약조건

| 테이블 | 제약조건 | 유형 | 설명 |
|---|---|---|---|
| accounts | pk_accounts | Primary Key | 계좌 기본 키 |
| accounts | uk_accounts_account_number | Unique | 계좌번호 중복 방지 |
| stocks | pk_stocks | Primary Key | 종목 기본 키 |
| stocks | uk_stocks_stock_code | Unique | 종목 코드 중복 방지 |
| stock_prices | fk_stock_prices_stock_id | Foreign Key | stocks.id 참조 |
| stock_orders | fk_stock_orders_account_id | Foreign Key | accounts.id 참조 |
| stock_orders | fk_stock_orders_stock_id | Foreign Key | stocks.id 참조 |
| executions | fk_executions_buy_order_id | Foreign Key | stock_orders.id 참조 |
| executions | fk_executions_sell_order_id | Foreign Key | stock_orders.id 참조 |
| holdings | uk_holdings_account_stock | Unique | 계좌별 종목 보유 정보 중복 방지 |
| cash_balances | uk_cash_balances_account_id | Unique | 계좌별 예수금 정보 중복 방지 |

---

# 12. 마이그레이션 작성 기준

DB 스키마 변경 시 다음 기준을 따른다.

- 변경 전 현재 스키마를 확인한다.
- 컬럼 추가 시 null 허용 여부와 기본값을 명확히 한다.
- 기존 데이터에 영향을 주는 변경은 별도 검증이 필요하다.
- 컬럼 삭제는 신중하게 진행한다.
- enum 값 변경은 기존 데이터와 코드 영향을 함께 확인한다.
- 외래키 추가 시 참조 무결성을 확인한다.
- unique 제약조건 추가 시 기존 중복 데이터 여부를 확인한다.
- 인덱스 추가 시 조회 성능과 쓰기 비용을 함께 고려한다.
- 성능 비교용 인덱스는 적용 전후 측정 결과를 함께 기록한다.
- 운영 환경 마이그레이션은 롤백 방법을 고려한다.

---

# 13. DB 변경 체크리스트

DB 변경 전후 다음을 확인한다.

- [ ] 변경 대상 테이블이 명확한가?
- [ ] 컬럼 타입이 적절한가?
- [ ] null 허용 여부가 명확한가?
- [ ] 기본값이 필요한가?
- [ ] unique 제약조건이 필요한가?
- [ ] 외래키 관계가 필요한가?
- [ ] 인덱스가 필요한가?
- [ ] 기존 데이터와 충돌하지 않는가?
- [ ] 정산 계산식에 영향이 있는가?
- [ ] 대사 기준에 영향이 있는가?
- [ ] 배치 실행 이력에 영향이 있는가?
- [ ] 성능 비교 기준에 영향이 있는가?
- [ ] 2단계 실시간 주문·체결 확장에 영향이 있는가?
- [ ] API 응답에 영향이 있는가?
- [ ] 마이그레이션 실패 시 롤백 가능한가?
- [ ] 관련 문서가 갱신되었는가?
- [ ] README와 PPT에 반영할 내용이 있는가?

---

# 14. 에이전트 작업 지침

AI 에이전트가 DB 관련 작업을 할 때는 다음을 따른다.

- 이 문서를 먼저 확인한다.
- 실제 코드의 Entity, Model, Migration 파일과 비교한다.
- DB 스키마와 코드가 다르면 현재 동작하는 코드를 기준으로 판단한다.
- 스키마 변경이 필요한 경우 실행 계획을 작성한다.
- 금액 필드는 `double`, `float`가 아니라 `BigDecimal` 기준으로 처리한다.
- 외래키, unique, index 등 제약조건을 함께 검토한다.
- 기존 데이터를 손상시킬 수 있는 변경은 피하거나 롤백 방법을 작성한다.
- 정산 결과 중복 방지 기준을 확인한다.
- 배치 성능 비교용 인덱스는 적용 전후 측정 계획을 함께 작성한다.
- 2단계 증권 실시간 처리 테이블은 1단계 완료 후 설계 문서를 먼저 작성한 뒤 추가한다.
- 변경 후 이 문서를 갱신한다.

---

# 15. 핵심 원칙 요약

- DB 스키마는 프로젝트 데이터 구조의 기준이다.
- 현재는 1단계 배치 정산 성능 개선 테이블을 우선한다.
- 2단계 증권 실시간 주문·체결 테이블은 향후 확장 대상으로 관리한다.
- 테이블, 컬럼, 관계, 제약조건을 명확히 기록한다.
- 금액 데이터는 `BigDecimal` 기준으로 처리한다.
- 정산 결과는 중복 저장되지 않아야 한다.
- 단, 성능 비교를 위해 처리 전략별 정산 결과는 구분 저장할 수 있어야 한다.
- 배치 실행 이력은 성공과 실패 여부와 관계없이 남아야 한다.
- 대사 기능은 정산 결과의 데이터 정합성을 검증하기 위한 핵심 구조이다.
- 증권 실시간 처리에서는 체결 결과와 잔고/예수금 정합성이 핵심이다.
- DB 변경은 코드, API, 안정성, 성능 비교, 포트폴리오 설명에 영향을 줄 수 있다.
- 스키마 변경 후 문서를 갱신한다.
