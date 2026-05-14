# GROUP_BY_QUERY Settlement Strategy Plan

## Problem

`BASIC_LOOP` is implemented as the baseline strategy. It loads every `Payment` for a settlement date and aggregates amounts in Java. That is useful for comparison, but it is not the preferred shape for large settlement batches because entity loading and application-side grouping become the bottleneck.

## Decision

Implement `GROUP_BY_QUERY` as the first improvement step:

- Aggregate completed payments in the database by merchant.
- Return only merchant-level aggregate rows to Java.
- Keep fee and final settlement calculation in Java using the same rounding policy as `BASIC_LOOP`.
- Keep individual `Settlement` saves so the effect of DB aggregation is isolated from bulk insert optimization.
- Leave `GROUP_BY_BULK_SAVE` and `GROUP_BY_BULK_INDEX` unimplemented with explicit errors.

## Scope

1. Add a repository projection and GROUP BY query using actual entity field names:
   - `Payment.transactionDate`
   - `Payment.status`
   - `Payment.type`
   - `Payment.amount`
   - `Payment.merchant`
2. Add `GroupBySettlementProcessor`.
3. Route `GROUP_BY_QUERY` from the settlement execution service.
4. Keep `BatchJobHistoryService` as `REQUIRES_NEW`.
5. Update the React dashboard so `GROUP_BY_QUERY` can run and future strategies are clearly marked as not implemented.
6. Add tests for result equality, duplicate blocking, history success, failure rollback/history, and no target data.
7. Update README and supporting docs with the action-oriented explanation and DB constraint check SQL.

## DB Constraint Check

Before implementation, local PostgreSQL constraints were checked with:

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

Observed local constraints already allow:

- `processing_strategy`: `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`, `GROUP_BY_BULK_INDEX`
- `status`: `RUNNING`, `SUCCESS`, `FAILED`
- `settlements`: unique on `merchant_id + settlement_date + processing_strategy`

## Verification

- `./gradlew test`
- `cd frontend` then `npm run build`
- Local API verification on 2026-05-14 with 100,000 payments:
  - `BASIC_LOOP`: 728~1043ms across repeated runs
  - `GROUP_BY_QUERY`: 104~265ms across repeated runs
  - Settlement totals matched for payment amount, cancel amount, fee amount, and final settlement amount.
