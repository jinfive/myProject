# GROUP_BY_BULK_SAVE 1차 구현 계획

## 1. 문제

GROUP_BY_QUERY는 Payment 전체 Entity 로딩을 제거했지만 Settlement 저장은 개별 `save` 반복 구조였다.

저장 단계도 데이터 규모가 커지면 병목이 될 수 있으므로, 조회 최적화와 저장 최적화를 분리해 비교할 필요가 있었다.

## 2. 판단

한 번에 `saveAll`, Hibernate `batch_size`, PostgreSQL `reWriteBatchedInserts=true`를 모두 적용하면 어떤 변경이 성능에 영향을 줬는지 설명하기 어렵다.

따라서 이번 1차 구현에서는 `saveAll`만 적용하고, 실제 JDBC batch insert 설정은 다음 단계로 분리했다.

## 3. 구현 액션

- `GroupByBulkSaveSettlementProcessor` 추가
- GROUP_BY_QUERY의 DB GROUP BY 집계 쿼리 재사용
- Payment 전체 Entity List 조회 없이 집계 결과로 Settlement 생성
- 개별 `save` 반복 대신 `settlementRepository.saveAll(settlements)` 적용
- `BasicLoopSettlementService`에 `GROUP_BY_BULK_SAVE` 분기 추가
- 프론트에서 `GROUP_BY_BULK_SAVE` 실행 가능 처리
- `GROUP_BY_BULK_INDEX`는 미구현 상태 유지

## 4. 검증

- 집계 결과 건수와 저장 Settlement 수 일치 검증
- BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE 금액 동일성 검증
- 같은 날짜 + GROUP_BY_BULK_SAVE 중복 실행 방지 검증
- GROUP_BY_BULK_SAVE 실패 시 Settlement 롤백과 BatchJobHistory FAILED 보존 검증
- `saveAll` 사용과 Payment Entity List 미조회 단위 테스트

## 5. 성능 측정

측정 조건:

- 데이터 건수: 100,000건
- 정산일자: 2026-05-14
- 기준: `batch_job_histories.elapsed_ms`
- 같은 날짜의 Settlement만 초기화하고 BatchJobHistory는 보존

| 전략 | 실행 시간 | 정산 결과 |
|---|---:|---|
| BASIC_LOOP | 882ms | 기준 |
| GROUP_BY_QUERY | 133ms | BASIC_LOOP와 동일 |
| GROUP_BY_BULK_SAVE | 110ms | BASIC_LOOP와 동일 |

## 6. 다음 작업

- Hibernate `jdbc.batch_size` 적용 전후 비교
- `order_inserts` 적용 검토
- PostgreSQL JDBC `reWriteBatchedInserts=true` 적용 검토
- GROUP_BY_BULK_INDEX 구현
- EXPLAIN ANALYZE로 실행 계획 확인
