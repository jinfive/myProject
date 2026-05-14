
# Tech Debt Tracker

## 1. 목적

이 문서는 **금융 거래 처리 성능 최적화 포트폴리오 프로젝트**에서 발견된 기술부채를 추적하기 위한 문서이다.

이 프로젝트는 하나의 저장소 안에서 다음 두 가지 흐름을 단계적으로 구현한다.

```txt
1단계: 대용량 금융 거래 정산·대사 배치 성능 최적화
2단계: 실시간 주식 시세 API 기반 모의 주문·체결 처리 시스템
````

이 프로젝트의 핵심 키워드는 다음과 같다.

* 안정성
* 데이터 정합성
* 대용량 배치 처리
* 정산과 대사
* 성능 개선
* 실시간 시세 처리
* 주문·체결 처리
* 잔고와 예수금 정합성
* 운영 관점
* 포트폴리오와 PPT 설명 가능성

기술부채는 당장 기능 동작에는 문제가 없더라도, 장기적으로 다음 영역에 악영향을 줄 수 있는 문제를 의미한다.

* 유지보수성
* 안정성
* 데이터 정합성
* 성능
* 확장성
* 테스트 신뢰도
* 문서 신뢰도
* 포트폴리오 완성도

이 문서는 다음 내용을 관리한다.

* 알려진 기술부채
* 영향도
* 우선순위
* 현재 상태
* 관련 파일
* 해결 방향
* 완료 기준
* README와 PPT 반영 필요 여부

---

## 2. 현재 프로젝트 단계

현재 프로젝트는 1단계와 2단계로 나누어 진행한다.

### 2.1 1단계: 배치 처리 성능 개선

현재는 1단계인 **정산 배치 성능 비교용 MVP 중 GROUP_BY_BULK_SAVE 1차 구현 완료** 단계이다.

현재 구현된 기능:

* Spring Boot 백엔드 구조 구성
* Merchant, Payment, Settlement, BatchJobHistory 도메인 구현
* 더미 데이터 자동 생성

    * 가맹점 100개
    * 결제 데이터 100,000건
    * 특정일 거래 약 70,000건
* BASIC_LOOP 정산 배치 구현
* GROUP_BY_QUERY 정산 배치 구현
* GROUP_BY_BULK_SAVE 1차 정산 배치 구현
* 정산 실행 API 구현

    * `POST /api/settlements/run`
* 정산 결과 조회 API 구현

    * `GET /api/settlements`
* 배치 이력 조회 API 구현

    * `GET /api/batch-histories`
* React 대시보드 구현
* 프론트 빌드 성공
* 백엔드 테스트 명령 성공

현재 1단계의 핵심 기술부채:

```txt
1. DB 설정 불일치
2. 핵심 정산 로직 테스트 부족
3. 벌크 저장 최적화 미구현
4. 인덱스 적용 및 성능 비교 미구현
5. EXPLAIN ANALYZE 실행 계획 확인 미완료
6. 대사 기능 미구현
7. RUNNING 중복 실행 방지 미구현
8. 날짜 파티셔닝 고도화 문서화 필요
9. 실제 코드와 DB 문서 불일치
10. 성능 비교 결과 문서화 부족
```

---

### 2.2 2단계: 증권 실시간 시세 기반 주문·체결 처리

2단계는 1단계 배치 성능 개선 프로젝트가 정리된 뒤 같은 프로젝트 안에서 확장한다.

2단계는 단순히 “실시간 기능 추가”가 아니라, 다음 흐름을 구현하는 **증권사 특화 포트폴리오 기능**이다.

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

2단계의 핵심 기술부채:

```txt
1. 실시간 시세 API 연동 설계 미작성
2. 모의 주문 생성기 설계 미작성
3. 주문 Queue 설계 미작성
4. 메모리 기반 OrderBook 설계 미작성
5. 체결 처리와 상태 변경 정책 미작성
6. 잔고/예수금 정합성 처리 설계 미작성
7. WebSocket 체결 알림 설계 미작성
8. 대량 주문 성능 테스트 설계 미작성
```

현재 우선순위는 1단계 완성이지만, 2단계 증권 실시간 처리 기획은 확정된 범위로 관리한다.

---

## 3. 기술부채 기록 기준

다음에 해당하는 항목은 기술부채로 기록한다.

* 임시 구현이 들어간 경우
* 현재는 동작하지만 포트폴리오 핵심 메시지를 약하게 만드는 경우
* 테스트가 부족한 경우
* 문서가 실제 코드와 다른 경우
* 성능 개선 전략이 아직 구현되지 않은 경우
* 데이터 정합성 검증이 부족한 경우
* 실패 이력, 재실행, 예외 처리가 부족한 경우
* 설정, 실행 환경, DB 연결 정보가 불일치하는 경우
* 구조 개선이 필요하지만 범위가 큰 경우
* 2단계 증권 실시간 처리 확장을 막을 수 있는 구조가 있는 경우
* README 또는 PPT에서 설명하기 어려운 구조가 있는 경우

---

## 4. 상태 기준

| 상태          | 의미                   |
| ----------- | -------------------- |
| Open        | 아직 해결되지 않음           |
| In Progress | 해결 중                 |
| Blocked     | 외부 요인이나 선행 작업 때문에 막힘 |
| Deferred    | 의도적으로 보류             |
| Resolved    | 해결 완료                |
| Won't Fix   | 해결하지 않기로 결정          |
| Planned     | 향후 단계에서 처리 예정        |

---

## 5. 우선순위 기준

| 우선순위 | 의미         | 예시                                    |
| ---- | ---------- | ------------------------------------- |
| P0   | 즉시 해결 필요   | 데이터 손상 가능성, 핵심 기능 장애, 실행 불가           |
| P1   | 빠르게 해결 필요  | 정합성 문제, 핵심 테스트 부족, 주요 성능 개선 미구현       |
| P2   | 일정 내 해결 권장 | 유지보수성 저하, 문서 불일치, 구조 개선 필요, 2단계 설계 필요 |
| P3   | 낮은 우선순위    | 작은 리팩토링, 화면 스타일, 문구 정리                |

---

## 6. 영향도 기준

| 영향도    | 의미                                       |
| ------ | ---------------------------------------- |
| High   | 안정성, 데이터 정합성, 핵심 기능, 포트폴리오 핵심 메시지에 직접 영향 |
| Medium | 유지보수성, 성능 비교, 확장성, 문서 신뢰도에 영향            |
| Low    | 코드 가독성, 문서 품질, 작은 개선 사항에 영향              |

---

## 7. 기술부채 목록

| ID     | 제목                                          | 상태      | 우선순위 | 영향도    | 담당자 | 발견일        | 목표 해결일 |
| ------ | ------------------------------------------- | ------- | ---- | ------ | --- | ---------- | ------ |
| TD-001 | `application.yml`과 `compose.yaml` DB 설정 불일치 | Open    | P1   | High   | TBD | 2026-05-11 | TBD    |
| TD-002 | 핵심 정산 계산 테스트 부족                             | Open    | P1   | High   | TBD | 2026-05-11 | TBD    |
| TD-003 | 중복 정산 방지 테스트 부족                             | Resolved | P1   | High   | TBD | 2026-05-11 | 2026-05-12 |
| TD-004 | GROUP BY 기반 정산 개선 전략 미구현                    | Resolved | P1   | High   | TBD | 2026-05-11 | 2026-05-13 |
| TD-005 | GROUP_BY_BULK_SAVE 1차 saveAll 적용                  | Resolved | P1   | High   | TBD | 2026-05-11 | 2026-05-14 |
| TD-006 | 인덱스 적용 및 성능 비교 미구현                          | Open    | P1   | High   | TBD | 2026-05-11 | TBD    |
| TD-007 | 실제 테이블 기준 DB 문서 미갱신                         | Open    | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-008 | 배치 실패 이력과 재실행 정책 부족                         | In Progress | P1   | High   | TBD | 2026-05-11 | TBD    |
| TD-009 | 성능 비교 결과 문서화 부족                             | Open    | P1   | High   | TBD | 2026-05-11 | TBD    |
| TD-010 | 정산 처리 전략 구조 분리 부족                           | In Progress | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-011 | 프론트엔드 대시보드 구조가 단일 파일 중심                     | Open    | P3   | Low    | TBD | 2026-05-11 | TBD    |
| TD-012 | 공통 예외 응답 구조 부족                              | Open    | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-013 | 2단계 증권 실시간 시세 API 연동 설계 미작성                 | Planned | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-014 | 2단계 모의 주문 생성기 설계 미작성                        | Planned | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-015 | 2단계 주문 Queue와 OrderBook 설계 미작성              | Planned | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-016 | 2단계 체결 결과와 잔고/예수금 정합성 설계 미작성                | Planned | P2   | High   | TBD | 2026-05-11 | TBD    |
| TD-017 | 2단계 WebSocket 체결 알림 설계 미작성                  | Planned | P3   | Medium | TBD | 2026-05-11 | TBD    |
| TD-018 | 2단계 대량 주문 성능 테스트 설계 미작성                     | Planned | P2   | Medium | TBD | 2026-05-11 | TBD    |
| TD-019 | 로컬 벤치마크 데이터 날짜 동기화 운영 오용 방지                  | In Progress | P2   | Medium | TBD | 2026-05-12 | TBD    |
| TD-020 | 로컬 DB check constraint와 enum 값 불일치                  | Resolved | P1   | High   | TBD | 2026-05-12 | 2026-05-12 |
| TD-021 | EXPLAIN ANALYZE 실행 계획 확인 미완료                  | Open | P1   | High   | TBD | 2026-05-13 | TBD |
| TD-022 | Payment 원천 데이터와 Settlement 결과 대사 기능 미구현                  | Open | P1   | High   | TBD | 2026-05-13 | TBD |
| TD-023 | RUNNING 상태 기준 중복 실행 방지 미구현                  | Open | P1   | High   | TBD | 2026-05-13 | TBD |
| TD-024 | 날짜 파티셔닝 고도화 항목 문서화 필요                  | Open | P2   | Medium | TBD | 2026-05-13 | TBD |
| TD-025 | Hibernate batch_size와 reWriteBatchedInserts 적용 검토                  | Open | P1   | High   | TBD | 2026-05-14 | TBD |

---

## TD-019: 로컬 벤치마크 데이터 날짜 동기화 운영 오용 방지

### 상태

In Progress

### 우선순위

P2

### 영향도

Medium

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Reliability
* Performance
* Documentation

### 관련 파일

* `src/main/java/com/example/myproject/data/BenchmarkDataDateSyncService.java`
* `src/main/java/com/example/myproject/data/DummyDataRunner.java`
* `src/main/resources/application.yml`

### 문제

대용량 성능 비교를 반복하기 위해 매번 Payment 데이터를 새로 생성하면 시간이 오래 걸리고 실험 조건도 달라질 수 있다.

반대로 기존 Payment 날짜를 자동으로 바꾸는 기능은 운영 환경에서 사용되면 원천 거래 데이터 의미를 훼손할 수 있다.

### 해결 방향

로컬 벤치마크 환경에서는 기존 Payment 데이터를 재사용하되, 정산 기준일만 오늘 날짜로 동기화한다.

이미 모든 Payment가 오늘 날짜이면 아무 작업도 하지 않고, 날짜가 다를 때만 기존 settlements를 삭제한 뒤 Payment 날짜를 오늘로 일괄 변경한다.

batch_job_histories는 실행 이력이므로 삭제하지 않고 보존한다.

이 기능은 운영 기능이 아니라 개발용 기능이며 `benchmark.data-date-sync-enabled` 설정으로 제어한다.

### 완료 기준

* [x] Payment 전체를 Entity List로 로딩하지 않는다.
* [x] Payment 날짜 변경은 벌크 update로 처리한다.
* [x] Settlement 삭제는 벌크 delete로 처리한다.
* [x] BatchJobHistory는 삭제하지 않는다.
* [x] 실행 여부와 변경 건수를 로그로 남긴다.
* [x] 설정값으로 켜고 끌 수 있다.
* [x] 정책 테스트가 추가되었다.
* [ ] 운영/배포 프로파일이 생기면 기본 비활성화 여부를 별도 검토한다.

### 메모

이를 통해 같은 데이터 규모와 조건에서 BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE, GROUP_BY_BULK_INDEX 전략을 반복 비교할 수 있다.

---

## TD-020: 로컬 DB check constraint와 enum 값 불일치

### 상태

Resolved

### 우선순위

P1

### 영향도

High

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Database
* Reliability

### 문제

BatchJobHistory에 `RUNNING` 상태가 추가되었지만, 기존 로컬 PostgreSQL DB의 `batch_job_histories_status_check` 제약조건이 `RUNNING` 값을 허용하지 않아 정산 실행 시 시작 이력 저장이 실패할 수 있다.

Hibernate `ddl-auto=update`는 기존 check constraint를 안전하게 갱신하지 못할 수 있다.

### 해결 방향

`RUNNING` 상태와 시작 이력 저장 흐름은 유지한다.

기존 batch_job_histories 데이터는 삭제하지 않고, 로컬 스키마 보정 러너와 수동 SQL 문서에서 기존 status check constraint를 제거한 뒤 `RUNNING`, `SUCCESS`, `FAILED`를 허용하는 새 check constraint를 추가한다.

processing_strategy check constraint도 현재 enum 값인 `BASIC_LOOP`, `GROUP_BY_QUERY`, `GROUP_BY_BULK_SAVE`, `GROUP_BY_BULK_INDEX`와 일치하도록 갱신한다.

### 완료 기준

* [x] BatchJobStatus.RUNNING은 유지한다.
* [x] batch_job_histories 데이터는 삭제하지 않는다.
* [x] status check constraint가 RUNNING, SUCCESS, FAILED를 허용하도록 보정된다.
* [x] processing_strategy check constraint가 현재 enum 값과 일치하도록 보정된다.
* [x] 수동 확인 SQL과 마이그레이션 SQL을 문서화했다.
* [x] `./gradlew test`가 통과한다.

---

## TD-021: EXPLAIN ANALYZE 실행 계획 확인 미완료

### 상태

Open

### 우선순위

P1

### 영향도

High

### 문제

GROUP_BY_BULK_INDEX 단계에서 인덱스를 추가하더라도 실제 집계 쿼리가 인덱스를 사용하는지 확인하지 않으면 성능 개선 근거가 약하다.

### 해결 방향

인덱스 적용 전후 `EXPLAIN ANALYZE` 결과를 기록하고, Seq Scan/Index Scan 여부, 실행 시간, rows 예측과 실제 rows 차이를 README와 PPT 재료로 남긴다. 대량 데이터 생성 또는 날짜 동기화 후에는 필요 시 `ANALYZE payments;`를 실행하도록 문서화한다.

### 완료 기준

* [ ] 인덱스 적용 전 실행 계획을 기록했다.
* [ ] 인덱스 적용 후 실행 계획을 기록했다.
* [ ] 실제 실행 시간이 README에 반영되었다.
* [ ] PPT 장표에 사용할 요약이 정리되었다.

---

## TD-022: Payment 원천 데이터와 Settlement 결과 대사 기능 미구현

### 상태

Open

### 우선순위

P1

### 영향도

High

### 문제

성능 개선 전략이 늘어날수록 처리 시간이 줄어도 정산 결과가 원천 Payment와 일치하는지 별도로 증명해야 한다.

### 해결 방향

Payment 기준 총 결제금액, 총 취소금액과 Settlement 기준 금액, 수수료, 최종 정산금액, 차이 금액, 일치 여부를 비교하는 대사 기능을 추가한다.

### 완료 기준

* [ ] Payment 원천 합계와 Settlement 결과를 비교할 수 있다.
* [ ] 전략별 대사 결과를 확인할 수 있다.
* [ ] 불일치 여부와 차이 금액이 드러난다.
* [ ] README/PPT에 정합성 검증 근거로 기록했다.

---

## TD-023: RUNNING 상태 기준 중복 실행 방지 미구현

### 상태

Open

### 우선순위

P1

### 영향도

High

### 문제

SUCCESS 결과 중복 방지는 저장된 Settlement 기준으로 동작하지만, 같은 날짜와 같은 전략의 배치가 이미 RUNNING인 상태에서 새 요청이 들어오는 상황은 별도의 안정성 문제다.

### 해결 방향

BatchJobHistory의 `RUNNING` 상태를 기준으로 같은 `settlementDate + processingStrategy` 실행 중 요청을 거부한다. 이는 버튼 반복 클릭이나 동시 요청으로 인한 불필요한 DB 부하와 충돌을 줄이기 위한 안정성 개선으로 README에 기록한다.

### 완료 기준

* [ ] 같은 날짜와 같은 전략의 RUNNING 이력이 있으면 새 실행을 거부한다.
* [ ] SUCCESS 결과 중복 방지와 RUNNING 중복 실행 방지의 책임이 구분된다.
* [ ] 자동 테스트 또는 수동 검증 결과가 있다.
* [ ] README/PPT에 안정성 개선으로 기록했다.

---

## TD-024: 날짜 파티셔닝 고도화 항목 문서화 필요

### 상태

Open

### 우선순위

P2

### 영향도

Medium

### 문제

정산 배치는 특정 정산일자 기준으로 대량 Payment를 반복 조회하므로 데이터가 장기 누적되면 날짜 기준 파티셔닝을 검토할 수 있다. 다만 현재 포트폴리오 구현 범위에 포함하면 핵심 단계가 흐려질 수 있다.

### 해결 방향

날짜 파티셔닝은 실제 구현하지 않고 고도화 항목으로 문서화한다. 현재 구현 범위는 GROUP BY, Bulk Save, Index, EXPLAIN ANALYZE, 대사, RUNNING 중복 실행 방지까지로 고정한다.

### 완료 기준

* [ ] 날짜 파티셔닝을 고도화 항목으로 README에 정리했다.
* [ ] 실제 파티셔닝 구현은 하지 않는다.
* [ ] 장기 데이터 누적 시 검토할 방향과 이유를 설명했다.

---

## 8. 상세 기록 템플릿

아래 형식을 복사해서 새 기술부채를 기록한다.

```md
## TD-000: 제목

### 상태

Resolved

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

YYYY-MM-DD

### 목표 해결일

TBD

### 관련 단계

- 1단계 배치
- 2단계 증권 실시간
- 공통

### 관련 영역

- Backend
- Frontend
- Database
- Performance
- Reliability
- Reconciliation
- Realtime
- Trading
- Testing
- Documentation
- DevOps
- Portfolio

### 관련 파일

- `path/to/file`

### 문제

현재 어떤 문제가 있는지 설명한다.

### 영향

이 문제가 계속 남아 있을 경우 어떤 영향이 있는지 설명한다.

예시:

- 정산 결과의 신뢰도가 낮아진다.
- 성능 개선 전후 비교가 어렵다.
- 배치 실패 시 원인 추적이 어렵다.
- 주문 체결 결과와 잔고가 불일치할 수 있다.
- 실시간 처리 흐름을 README와 PPT에서 설명하기 어렵다.
- 2단계 증권 실시간 처리 확장 시 구조 충돌이 발생할 수 있다.

### 해결 방향

어떤 방식으로 해결할지 작성한다.

### 완료 기준

- [ ] 문제 원인이 제거되었다.
- [ ] 관련 코드가 정리되었다.
- [ ] 필요한 테스트가 추가되었다.
- [ ] 관련 문서가 갱신되었다.
- [ ] README 또는 PPT 반영 여부를 확인했다.
- [ ] 기존 기능이 정상 동작한다.

### 메모

추가로 기록할 내용을 작성한다.
```

---

# 9. 상세 기록

## TD-001: `application.yml`과 `compose.yaml` DB 설정 불일치

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치
* 공통

### 관련 영역

* Backend
* Database
* DevOps
* Reliability

### 관련 파일

* `src/main/resources/application.yml`
* `compose.yaml`

### 문제

현재 `application.yml`의 DB 설정과 `compose.yaml`의 DB 설정이 서로 다르다.

현재 확인된 불일치:

```txt
application.yml:
- database: settlement_perf_db
- username: root
- password: 1234

compose.yaml:
- database: myproject
- username: postgres
- password: postgres
```

### 영향

DB 설정이 다르면 실행 환경에 따라 애플리케이션이 정상적으로 실행되지 않을 수 있다.

또한 README, PPT, 실행 가이드에서 어떤 DB 기준으로 성능을 측정했는지 설명하기 어려워진다.

성능 비교 프로젝트에서는 동일한 환경에서 비교해야 하므로, DB 설정 불일치는 성능 측정 신뢰도에도 영향을 준다.

### 해결 방향

로컬 개발과 Docker Compose 실행 기준 DB 설정을 하나로 통일한다.

권장 방향:

```txt
database: settlement_perf_db
username: postgres
password: postgres
```

또는 현재 compose 기준을 유지하려면 `application.yml`을 compose 기준으로 맞춘다.

### 완료 기준

* [ ] `application.yml`과 `compose.yaml`의 DB 이름이 일치한다.
* [ ] DB 사용자명이 일치한다.
* [ ] DB 비밀번호가 일치한다.
* [ ] 애플리케이션이 Docker Compose DB에 정상 연결된다.
* [ ] `./gradlew test`가 통과한다.
* [ ] README 실행 방법이 갱신되었다.

### 메모

현재 가장 먼저 해결해야 하는 안정성 관련 기술부채이다.

---

## TD-002: 핵심 정산 계산 테스트 부족

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Testing
* Reliability
* Reconciliation

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`
* `src/test/java`

### 문제

현재 백엔드 테스트 명령은 성공하지만, 핵심 정산 계산 로직에 대한 테스트가 부족하다.

현재는 `contextLoads` 수준의 테스트만 존재하는 것으로 보이며, 다음 계산 로직을 검증하는 테스트가 필요하다.

* 총 결제금액 계산
* 총 취소금액 차감
* 순매출 계산
* 수수료 계산
* 최종 정산금액 계산
* 가맹점별 집계

### 영향

정산 계산은 이 프로젝트의 핵심이다.

테스트가 부족하면 성능 개선 전략을 추가하는 과정에서 정산 결과가 틀어져도 빠르게 발견하기 어렵다.

성능이 좋아져도 금액이 틀리면 금융 프로젝트로서 설득력이 떨어진다.

### 해결 방향

정산 계산 단위 테스트를 추가한다.

우선 추가할 테스트:

```txt
1. 결제 데이터만 있는 경우 정산금액 계산
2. 결제와 취소가 함께 있는 경우 정산금액 계산
3. 가맹점별 수수료율이 다른 경우 수수료 계산
4. 정산 대상 데이터가 없는 경우 처리
5. 여러 가맹점 데이터가 섞인 경우 가맹점별 집계
```

### 완료 기준

* [ ] 정산 금액 계산 테스트가 추가되었다.
* [ ] 취소 금액 반영 테스트가 추가되었다.
* [ ] 가맹점별 수수료 계산 테스트가 추가되었다.
* [ ] 정산 대상 데이터 없음 테스트가 추가되었다.
* [ ] `./gradlew test`가 통과한다.
* [ ] 테스트 내용을 README 또는 테스트 문서에 요약했다.

### 메모

성능 개선보다 먼저 정합성 테스트를 확보하는 것이 좋다.

---

## TD-003: 중복 정산 방지 테스트 부족

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Database
* Testing
* Reliability
* Reconciliation

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`
* `src/main/java/com/example/myproject/repository/SettlementRepository.java`
* `src/test/java`

### 문제

같은 가맹점과 같은 정산일자에 대해 중복 정산이 발생하지 않아야 하지만, 이에 대한 테스트가 부족하다.

### 영향

중복 정산이 발생하면 정산 결과가 중복 저장되어 금액 신뢰도가 깨진다.

금융권 포트폴리오에서 데이터 정합성을 강조하려면 중복 정산 방지 로직과 테스트가 반드시 필요하다.

### 해결 방향

애플리케이션 검증과 DB 제약조건을 함께 검토한다.

권장 방향:

```txt
1. SettlementRepository에서 settlementDate + processingStrategy 기준 중복 여부 확인
2. DB unique constraint 적용 검토
3. 중복 실행 시 기존 결과 삭제 후 재생성 또는 실행 거부 정책 결정
4. 정책에 맞는 테스트 추가
```

### 완료 기준

* [x] 중복 정산 정책이 문서화되었다.
* [x] 중복 정산 방지 로직이 구현되었다.
* [x] 같은 정산일자와 같은 전략을 두 번 실행하는 테스트가 추가되었다.
* [x] DB 제약조건 적용 여부가 결정되었다.
* [x] `./gradlew test`가 통과한다.

### 메모

2026-05-12에 중복 기준을 `merchant_id + settlement_date + processing_strategy`로 변경하고, 같은 날짜의 다른 전략 저장 허용과 같은 전략 중복 저장 차단 테스트를 추가했다.

---

## TD-004: GROUP BY 기반 정산 개선 전략 미구현

### 상태

In Progress

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Database
* Performance
* Portfolio

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`
* `src/main/java/com/example/myproject/repository/PaymentRepository.java`

### 문제

현재 정산 배치는 BASIC_LOOP 방식만 구현되어 있다.

즉, 특정 정산일자의 Payment 데이터를 전체 조회한 뒤 Java 반복문으로 가맹점별 집계를 수행한다.

하지만 이 프로젝트의 핵심은 성능 개선 비교이므로, DB GROUP BY 기반 개선 전략이 반드시 필요하다.

### 영향

GROUP BY 개선 전략이 없으면 프로젝트가 “성능 개선 프로젝트”가 아니라 “정산 배치 기능 구현 프로젝트”로 보일 수 있다.

README와 PPT에서 처리 시간 개선 수치를 보여주기 어렵다.

### 해결 방향

GROUP_BY_QUERY 전략을 추가한다.

권장 구현 방향:

```txt
1. PaymentRepository에 정산일자 기준 GROUP BY 집계 쿼리 추가
2. 가맹점별 결제금액, 취소금액을 Projection으로 조회
3. Service에서 수수료와 최종 정산금액 계산
4. BatchJobHistory에 처리 전략과 실행 시간 저장
5. BASIC_LOOP와 동일 데이터 기준으로 성능 비교
```

### 완료 기준

* [x] GROUP_BY_QUERY 전략이 구현되었다.
* [x] 필요한 Projection 또는 DTO가 추가되었다.
* [x] BASIC_LOOP와 동일 정산일자로 실행 가능하다.
* [x] BatchJobHistory에 전략명이 기록된다.
* [x] 실행 시간이 기록된다.
* [x] README 성능 비교 표에 구현 상태와 비교 방법을 추가했다.

### 메모

2026-05-13에 GROUP_BY_QUERY를 구현했다. Payment 전체 Entity List를 가져오지 않고 DB GROUP BY 집계 결과만 조회하며, Settlement 저장은 성능 개선 효과 분리를 위해 개별 저장으로 유지했다.

---

## TD-005: 벌크 저장 최적화 미구현

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Database
* Performance

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`
* `src/main/java/com/example/myproject/repository/SettlementRepository.java`

### 문제

정산 결과 저장 시 개별 저장이 반복되면 데이터가 많아질수록 DB 저장 호출이 많아질 수 있다.

GROUP_BY_BULK_SAVE 1차 구현 전에는 벌크 저장 최적화가 GROUP BY 이후 단계형 전략으로 구현되어 있지 않았다.

### 영향

조회 성능을 개선하더라도 저장 구간에서 병목이 남을 수 있다.

성능 개선 흐름에서 “조회 최적화”와 “저장 최적화”를 분리해 보여주기 어렵다.

### 해결 방향

GROUP_BY_BULK_SAVE 전략을 추가한다.

권장 구현 방향:

```txt
1. 정산 결과를 List<Settlement>로 생성
2. 반복 save 대신 saveAll 적용
3. Hibernate batch insert 설정은 후속 단계로 분리
4. BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE와 처리 시간 비교
```

### 완료 기준

* [x] GROUP_BY_BULK_SAVE 전략이 구현되었다.
* [x] Settlement 저장 방식이 `saveAll` 기반으로 개선되었다.
* [x] 처리 시간이 BatchJobHistory에 기록된다.
* [x] 기존 결과와 금액이 동일한지 테스트 또는 수동 검증했다.
* [x] README 성능 비교 표에 결과를 추가했다.

### 메모

2026-05-14에 `saveAll` 기준 1차 구현을 완료했다. 100,000건 동일 조건 1회 측정에서 BASIC_LOOP 882ms, GROUP_BY_QUERY 133ms, GROUP_BY_BULK_SAVE 110ms가 기록되었다. 세 전략 모두 정산 결과 100건과 총 금액 합계가 동일했다.

Hibernate `batch_size`, `order_inserts`, PostgreSQL JDBC `reWriteBatchedInserts=true`는 TD-025로 분리해 다음 단계에서 검토한다.

---

## TD-025: Hibernate batch_size와 reWriteBatchedInserts 적용 검토

### 상태

Open

### 문제

`saveAll`은 코드상 일괄 저장 구조를 만들지만, JPA/Hibernate에서 실제 DB batch insert 효과를 충분히 보려면 Hibernate `jdbc.batch_size`, `order_inserts`, PostgreSQL JDBC `reWriteBatchedInserts=true` 같은 설정이 추가로 필요할 수 있다.

### 영향

`saveAll` 적용만으로 벌크 저장 성능 효과를 단정하면 포트폴리오 설명이 부정확해질 수 있다.

### 해결 방향

GROUP_BY_BULK_SAVE 1차 결과를 기준으로 다음 설정을 별도 단계에서 적용하고 성능과 정합성을 재검증한다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 1000
        order_inserts: true
        order_updates: true
```

PostgreSQL JDBC URL 후보:

```txt
reWriteBatchedInserts=true
```

### 완료 기준

* [ ] Hibernate batch_size 적용 전후 실행 시간이 비교되었다.
* [ ] PostgreSQL reWriteBatchedInserts 적용 여부가 판단되었다.
* [ ] BASIC_LOOP, GROUP_BY_QUERY, GROUP_BY_BULK_SAVE 결과 금액 동일성이 재검증되었다.
* [ ] README와 포트폴리오 문서에 실제 측정값만 반영되었다.

---

## TD-006: 인덱스 적용 및 성능 비교 미구현

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Database
* Performance
* Documentation
* Portfolio

### 관련 파일

* `src/main/java/com/example/myproject/domain/Payment.java`
* `src/main/java/com/example/myproject/domain/Settlement.java`
* `docs/PERFORMANCE.md`
* `README.md`

### 문제

정산 조건에 자주 사용되는 컬럼에 대한 인덱스 적용과 성능 비교가 아직 구현되어 있지 않다.

우선 검토할 인덱스:

```txt
transaction_date
merchant_id
status
type
transaction_date + merchant_id
transaction_date + merchant_id + type
settlement_date + merchant_id
```

### 영향

인덱스 적용 전후 성능 차이를 보여줄 수 없으면, DB 튜닝 경험을 포트폴리오에서 강조하기 어렵다.

또한 데이터가 증가할수록 정산 대상 조회와 중복 정산 검증이 느려질 수 있다.

### 해결 방향

인덱스 적용 전후를 비교한다.

권장 구현 방향:

```txt
1. 현재 쿼리 조건 확인
2. 인덱스 적용 전 실행 시간 측정
3. Payment와 Settlement에 필요한 인덱스 추가
4. 동일 데이터, 동일 정산일자로 재측정
5. 결과를 README와 PERFORMANCE.md에 정리
```

### 완료 기준

* [ ] 인덱스 적용 대상 컬럼이 결정되었다.
* [ ] 인덱스 적용 전 실행 시간이 측정되었다.
* [ ] 인덱스가 적용되었다.
* [ ] 인덱스 적용 후 실행 시간이 측정되었다.
* [ ] README와 PPT용 표에 결과를 정리했다.

### 메모

인덱스는 무조건 많이 거는 것이 아니라, 실제 정산 쿼리 조건 기준으로 적용한다.

---

## TD-007: 실제 테이블 기준 DB 문서 미갱신

### 상태

Open

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Database
* Documentation
* Portfolio

### 관련 파일

* `docs/generated/db-schema.md`
* Entity 클래스들

### 문제

현재 `docs/generated/db-schema.md`가 실제 코드의 테이블 구조가 아니라 템플릿성 사용자/권한 테이블 내용으로 되어 있다.

현재 프로젝트의 실제 도메인은 다음에 가깝다.

```txt
Merchant
Payment
Settlement
BatchJobHistory
```

### 영향

문서와 실제 코드가 다르면 에이전트나 개발자가 잘못된 문서를 기준으로 작업할 수 있다.

또한 포트폴리오와 PPT에서 DB 구조를 설명할 때 신뢰도가 떨어진다.

### 해결 방향

현재 Entity 기준으로 DB 문서를 갱신한다.

포함할 내용:

```txt
테이블명
컬럼명
타입
Nullable 여부
인덱스
Unique 제약조건
연관관계
정산 흐름에서의 역할
```

### 완료 기준

* [ ] 실제 Entity 기준으로 DB 스키마 문서가 갱신되었다.
* [ ] 오래된 사용자/권한 관련 템플릿 내용이 제거되었다.
* [ ] Merchant, Payment, Settlement, BatchJobHistory 테이블이 정리되었다.
* [ ] 인덱스 적용 후 문서가 다시 갱신되었다.

### 메모

성능 개선 결과와 함께 DB 구조를 설명할 수 있어야 한다.

---

## TD-008: 배치 실패 이력과 재실행 정책 부족

### 상태

Open

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Reliability
* Reconciliation
* Documentation

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`
* `BatchJobHistory`
* 향후 `SettlementError` 또는 `BatchError`

### 문제

현재 배치 실패 이력 저장, 부분 실패 처리, 재실행 정책이 약하다.

정산 배치는 성공만 중요한 것이 아니라 실패했을 때 원인을 추적하고 재처리할 수 있어야 한다.

### 영향

배치 실패 시 원인을 추적하기 어렵다.

또한 같은 정산일자를 다시 실행할 때 기존 결과를 어떻게 처리할지 명확하지 않으면 중복 정산이나 데이터 불일치가 발생할 수 있다.

### 해결 방향

실패 이력과 재실행 정책을 설계하고 구현한다.

결정해야 할 내용:

```txt
1. 배치 전체 실패 시 BatchJobHistory 상태 처리
2. 특정 가맹점 실패 시 부분 실패 허용 여부
3. 실패 이력 저장 테이블 추가 여부
4. 재실행 시 기존 Settlement 처리 방식
5. 실패 건만 재처리할 수 있는지 여부
```

### 완료 기준

* [x] 배치 실패 상태가 BatchJobHistory에 기록된다.
* [x] 실패 원인이 저장된다.
* [x] MVP 재실행 정책이 문서화되었다.
* [x] 중복 정산 방지 정책과 충돌하지 않는다.
* [x] 실패 케이스 테스트 또는 수동 검증이 완료되었다.

### 메모

2026-05-12에 BatchJobHistory를 별도 트랜잭션으로 분리해 `RUNNING`, `SUCCESS`, `FAILED`와 `errorMessage`를 보존하도록 개선했다. 부분 실패 처리, 실패 건 재처리, 강제 재실행 기능은 이번 MVP 범위에서 제외하고 향후 작업으로 남긴다.

---

## TD-009: 성능 비교 결과 문서화 부족

### 상태

In Progress

### 우선순위

P1

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Performance
* Documentation
* Portfolio

### 관련 파일

* `README.md`
* `docs/PERFORMANCE.md`
* `docs/PORTFOLIO_SUMMARY.md`

### 문제

현재 BASIC_LOOP 기준선은 구현되어 있지만, 성능 개선 전략별 결과를 README나 별도 문서에 충분히 정리하지 못했다.

### 영향

성능 개선 프로젝트임에도 개선 전후 수치를 보여주지 못하면 포트폴리오 설득력이 약해진다.

자기소개서와 면접에서 “무엇을 개선했는지” 설명하기 어렵다.

### 해결 방향

성능 측정 결과를 표와 설명으로 정리한다.

기본 표:

```md
| 전략 | 데이터 건수 | 처리 시간 | 개선 내용 |
|---|---:|---:|---|
| BASIC_LOOP | 100,000건 | 측정값 | 전체 조회 후 Java 반복문 집계 |
| GROUP_BY_QUERY | 100,000건 | 측정값 | DB GROUP BY 집계 |
| GROUP_BY_BULK_SAVE | 100,000건 | 측정값 | DB GROUP BY 집계 + 정산 결과 일괄 저장 |
| GROUP_BY_BULK_INDEX | 100,000건 | 측정값 | DB GROUP BY 집계 + 일괄 저장 + 조회 조건 인덱스 적용 |
```

### 완료 기준

* [x] BASIC_LOOP 처리 시간이 기록되었다.
* [x] GROUP_BY_QUERY 처리 시간이 기록되었다.
* [ ] GROUP_BY_BULK_SAVE 처리 시간이 기록되었다.
* [ ] GROUP_BY_BULK_INDEX 처리 시간이 기록되었다.
* [x] README에 BASIC_LOOP와 GROUP_BY_QUERY 1차 성능 개선 표가 추가되었다.
* [ ] PPT 제작용 요약 문장이 작성되었다.

### 메모

2026-05-14 로컬 PostgreSQL에서 100,000건 기준 BASIC_LOOP 728~1043ms, GROUP_BY_QUERY 104~265ms를 반복 측정하고 README에 1차 비교 결과를 반영했다. 단일 실행값은 DB/OS 캐시와 JVM warm-up 상태에 따라 흔들릴 수 있으므로 범위로 기록한다. GROUP_BY_BULK_SAVE와 GROUP_BY_BULK_INDEX 측정은 후속 단계로 남긴다.

---

## TD-010: 정산 처리 전략 구조 분리 부족

### 상태

Open

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Backend
* Architecture
* Performance

### 관련 파일

* `src/main/java/com/example/myproject/service/BasicLoopSettlementService.java`

### 문제

현재는 BASIC_LOOP 정산 서비스 중심으로 구현되어 있다.

향후 GROUP_BY_QUERY, GROUP_BY_BULK_SAVE, GROUP_BY_BULK_INDEX 전략을 추가하면 하나의 서비스에 로직이 과도하게 몰릴 수 있다.

### 영향

전략별 성능 비교가 어려워지고, 코드 유지보수성이 떨어질 수 있다.

README와 PPT에서 처리 전략별 구조를 설명하기도 어려워진다.

### 해결 방향

정산 처리 전략을 분리한다.

권장 구조:

```txt
settlement/strategy
├── SettlementStrategy.java
├── BasicLoopSettlementStrategy.java
├── GroupByQuerySettlementStrategy.java
├── GroupByBulkSaveSettlementStrategy.java
└── GroupByBulkIndexSettlementStrategy.java
```

### 완료 기준

* [x] 처리 전략 Enum이 정의되었다.
* [ ] 공통 Strategy 인터페이스가 정의되었다.
* [x] BASIC_LOOP 처리 processor가 분리되었다.
* [x] GROUP_BY_QUERY 전략이 분리되었다.
* [x] API에서 처리 전략을 선택할 수 있다.
* [x] 전략별 실행 시간이 기록된다.

### 메모

리팩토링 범위가 커질 수 있으므로 GROUP_BY_QUERY 구현 전후로 적절한 시점을 잡는다.

---

## TD-011: 프론트엔드 대시보드 구조가 단일 파일 중심

### 상태

Open

### 우선순위

P3

### 영향도

Low

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치

### 관련 영역

* Frontend
* Maintainability

### 관련 파일

* `frontend/src/main.jsx`

### 문제

현재 프론트엔드 대시보드가 `main.jsx` 중심으로 구현되어 있다.

기능이 많아질수록 코드가 길어지고 유지보수가 어려워질 수 있다.

### 영향

성능 비교 화면, 대사 결과 화면, 실패 이력 화면이 추가되면 단일 파일 구조가 복잡해질 수 있다.

2단계에서 실시간 시세, 주문 입력, 체결 알림 화면까지 추가되면 프론트 구조가 더 복잡해질 수 있다.

### 해결 방향

기능별 컴포넌트로 분리한다.

1단계 권장 구조:

```txt
frontend/src
├── components
│   ├── SettlementRunForm.jsx
│   ├── SettlementResultTable.jsx
│   ├── BatchHistoryTable.jsx
│   └── PerformanceSummary.jsx
├── api
│   └── settlementApi.js
└── main.jsx
```

2단계 확장 시 추가 구조:

```txt
frontend/src
├── trading
│   ├── StockPricePanel.jsx
│   ├── OrderForm.jsx
│   ├── OrderBookView.jsx
│   ├── ExecutionList.jsx
│   └── AccountBalancePanel.jsx
```

### 완료 기준

* [ ] API 호출 로직이 분리되었다.
* [ ] 정산 실행 폼이 컴포넌트로 분리되었다.
* [ ] 정산 결과 테이블이 컴포넌트로 분리되었다.
* [ ] 배치 이력 테이블이 컴포넌트로 분리되었다.
* [ ] 기존 화면이 정상 동작한다.

### 메모

우선순위는 낮다. 1단계 성능 개선 구현 후 정리해도 된다.

---

## TD-012: 공통 예외 응답 구조 부족

### 상태

Open

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 1단계 배치
* 2단계 증권 실시간
* 공통

### 관련 영역

* Backend
* API
* Reliability

### 관련 파일

* `src/main/java/com/example/myproject/api/SettlementController.java`
* 향후 `common/exception`
* 향후 `common/response`

### 문제

API 예외 응답 형식이 충분히 표준화되어 있지 않을 수 있다.

### 영향

프론트엔드에서 에러를 처리하기 어렵고, 배치 실패 원인을 화면과 문서에서 일관되게 설명하기 어렵다.

2단계에서 주문 거절, 잔고 부족, 보유 수량 부족, 체결 실패 같은 에러가 추가되면 공통 에러 구조가 더 중요해진다.

### 해결 방향

공통 에러 응답 구조를 정의한다.

예시:

```json
{
  "code": "SETTLEMENT_DUPLICATED",
  "message": "이미 정산된 일자입니다.",
  "detail": "settlementDate=2026-05-11",
  "timestamp": "2026-05-11T10:00:00"
}
```

2단계에서 추가될 수 있는 에러 예시:

```txt
ORDER_INSUFFICIENT_CASH
ORDER_INSUFFICIENT_HOLDING
ORDER_INVALID_PRICE
ORDER_NOT_FOUND
EXECUTION_FAILED
```

### 완료 기준

* [ ] 공통 에러 응답 DTO가 정의되었다.
* [ ] GlobalExceptionHandler가 추가되었다.
* [ ] 중복 정산, 정산 대상 없음, 배치 실패 에러가 구분된다.
* [ ] 프론트엔드에서 에러 메시지를 표시할 수 있다.
* [ ] API 문서가 갱신되었다.

### 메모

안정성과 운영 관점 설명에 도움이 된다.

---

## TD-013: 2단계 증권 실시간 시세 API 연동 설계 미작성

### 상태

Planned

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Realtime
* External API
* Backend
* Frontend
* Portfolio

### 관련 파일

* 향후 `docs/design-docs/realtime-stock-price-design.md`
* 향후 `docs/design-docs/realtime-trading-design.md`

### 문제

2단계에서는 실제 주식 시세 API를 통해 삼성전자 같은 특정 종목의 현재가를 수신하고, 이를 기준으로 모의 주문을 발생시키는 구조가 필요하다.

하지만 아직 실시간 시세 API 연동 방식, 실패 처리, 대체 모의 시세 생성 방식이 설계되어 있지 않다.

### 영향

시세 API 연동이 막히면 2단계 전체 구현이 지연될 수 있다.

또한 외부 API 장애나 인증 문제 발생 시 실시간 주문·체결 시뮬레이션이 중단될 수 있다.

### 해결 방향

실시간 시세 API 연동과 모의 시세 대체 전략을 함께 설계한다.

권장 방향:

```txt
1. 1순위: 모의 시세 생성기로 먼저 구현
2. 2순위: 실제 주식 시세 API 연동
3. API 실패 시 마지막 현재가 또는 모의 시세로 대체
4. 현재가를 기준으로 주문 생성 가격 범위 결정
```

### 완료 기준

* [ ] 사용할 시세 API 후보가 정리되었다.
* [ ] 실시간 시세 수신 방식이 설계되었다.
* [ ] 모의 시세 생성 방식이 설계되었다.
* [ ] API 실패 시 대체 흐름이 정리되었다.
* [ ] 현재가 데이터 구조가 정의되었다.
* [ ] README와 PPT에서 설명할 실시간 시세 흐름이 정리되었다.

### 메모

외부 API 연동은 프로젝트 리스크가 있으므로 모의 시세 생성기를 먼저 구현하는 것이 안전하다.

---

## TD-014: 2단계 모의 주문 생성기 설계 미작성

### 상태

Planned

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Realtime
* Trading
* Performance
* Testing

### 관련 파일

* 향후 `VirtualOrderGenerator`
* 향후 `docs/design-docs/virtual-order-generator-design.md`

### 문제

2단계에서는 실제 사용자가 없어도 대량 주문 상황을 만들어야 한다.

이를 위해 현재가 기준으로 매수/매도 주문을 자동 생성하는 모의 주문 생성기가 필요하지만 아직 설계되어 있지 않다.

### 영향

모의 주문 생성기가 없으면 초당 주문 수 기준 성능 테스트를 수행하기 어렵다.

실시간 처리 프로젝트임에도 대량 트래픽 대응 역량을 보여주기 어렵다.

### 해결 방향

VirtualOrderGenerator를 설계한다.

권장 기능:

```txt
1. 가상 계좌 여러 개 생성
2. 현재가 기준 ±1~3% 가격으로 주문 생성
3. 매수/매도 비율 조절
4. 주문 수량 랜덤 생성
5. 초당 주문 수 조절
6. 주문 Queue에 직접 적재
7. 주문 생성 시작/중지 API 제공
```

예시 API:

```txt
POST /api/admin/order-generator/start?ordersPerSecond=100
POST /api/admin/order-generator/stop
```

### 완료 기준

* [ ] 모의 주문 생성 기준이 설계되었다.
* [ ] 초당 주문 수 조절 방식이 설계되었다.
* [ ] 현재가 기반 주문 가격 생성 방식이 정의되었다.
* [ ] 주문 생성 시작/중지 API가 설계되었다.
* [ ] 성능 테스트와 연결되는 지표가 정의되었다.

### 메모

혼자 개발하는 프로젝트에서 대량 주문 상황을 재현하기 위한 핵심 기능이다.

---

## TD-015: 2단계 주문 Queue와 OrderBook 설계 미작성

### 상태

Planned

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Backend
* Realtime
* Trading
* Performance
* Concurrency

### 관련 파일

* 향후 `OrderQueue`
* 향후 `MatchingEngine`
* 향후 `OrderBook`
* 향후 `docs/design-docs/orderbook-design.md`

### 문제

2단계에서는 주문 접수와 체결 처리를 분리해야 한다.

주문이 들어올 때마다 DB에서 상대 주문을 조회해 체결하면 트래픽 증가 시 병목이 발생할 수 있다.

이를 해결하기 위해 주문 Queue와 메모리 기반 OrderBook 설계가 필요하지만 아직 작성되지 않았다.

### 영향

Queue와 OrderBook 구조 없이 구현하면 실시간 처리 성능을 설명하기 어렵다.

또한 주문 순서 보장, 가격 우선·시간 우선 체결, 부분체결 처리가 복잡해질 수 있다.

### 해결 방향

주문 Queue와 종목별 OrderBook을 설계한다.

권장 구조:

```txt
Order API
→ OrderEvent 생성
→ OrderQueue 적재
→ MatchingEngine 처리
→ Symbol별 OrderBook 반영
→ 가격 우선·시간 우선 매칭
```

OrderBook 자료구조:

```txt
매수 주문: 높은 가격 우선 PriorityQueue
매도 주문: 낮은 가격 우선 PriorityQueue
같은 가격: 먼저 들어온 주문 우선
```

### 완료 기준

* [ ] 주문 Queue 구조가 설계되었다.
* [ ] OrderBook 자료구조가 설계되었다.
* [ ] 가격 우선·시간 우선 정렬 기준이 정의되었다.
* [ ] 부분체결 처리 방식이 정의되었다.
* [ ] 종목별 처리 기준이 정의되었다.
* [ ] DB 직접 처리 방식과 비교할 성능 기준이 정리되었다.

### 메모

2단계 증권 프로젝트의 핵심 설계이다.

---

## TD-016: 2단계 체결 결과와 잔고/예수금 정합성 설계 미작성

### 상태

Planned

### 우선순위

P2

### 영향도

High

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Trading
* Reliability
* Consistency
* Database
* Testing

### 관련 파일

* 향후 `ExecutionService`
* 향후 `AccountService`
* 향후 `Holding`
* 향후 `CashBalance`
* 향후 `docs/design-docs/trading-consistency-design.md`

### 문제

체결이 발생하면 주문 상태, 체결 내역, 보유 수량, 예수금이 함께 변경되어야 한다.

하지만 아직 체결 결과와 잔고/예수금 정합성을 어떻게 보장할지 설계되어 있지 않다.

### 영향

체결은 되었지만 잔고가 반영되지 않거나, 예수금은 차감됐지만 체결 내역이 없는 문제가 발생할 수 있다.

증권사 포트폴리오에서 가장 중요한 정합성 메시지가 약해질 수 있다.

### 해결 방향

체결 결과 반영을 하나의 트랜잭션 흐름으로 설계한다.

체결 시 처리할 항목:

```txt
1. Execution 저장
2. 매수 주문 상태 변경
3. 매도 주문 상태 변경
4. 매수자 보유 수량 증가
5. 매수자 예수금 감소
6. 매도자 보유 수량 감소
7. 매도자 예수금 증가
8. AccountTransaction 저장
9. ExecutionEvent 발행
```

### 완료 기준

* [ ] 체결 결과 반영 흐름이 설계되었다.
* [ ] 잔고 부족 시 주문 거절 정책이 정의되었다.
* [ ] 보유 수량 부족 시 매도 주문 거절 정책이 정의되었다.
* [ ] 부분체결 시 잔여 수량 처리 기준이 정의되었다.
* [ ] 트랜잭션 범위가 정의되었다.
* [ ] 정합성 테스트 항목이 정의되었다.

### 메모

2단계에서 가장 중요한 정합성 관련 기술부채이다.

---

## TD-017: 2단계 WebSocket 체결 알림 설계 미작성

### 상태

Planned

### 우선순위

P3

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Realtime
* Frontend
* Backend
* Notification

### 관련 파일

* 향후 `WebSocketConfig`
* 향후 `ExecutionNotificationService`
* 향후 `docs/design-docs/websocket-notification-design.md`

### 문제

체결 결과를 사용자에게 실시간으로 전달하기 위한 WebSocket 알림 구조가 아직 설계되어 있지 않다.

### 영향

체결 결과를 화면에 실시간 반영하지 못하면 2단계 프로젝트의 실시간성 메시지가 약해진다.

### 해결 방향

체결 이벤트를 WebSocket으로 사용자에게 전달하는 구조를 설계한다.

흐름:

```txt
Execution 생성
→ ExecutionEvent 발행
→ WebSocket Notification 전송
→ 사용자 화면에서 주문 상태와 잔고 갱신
```

알림 예시:

```json
{
  "orderId": 1,
  "stockCode": "005930",
  "status": "PARTIAL_FILLED",
  "filledQuantity": 3,
  "remainingQuantity": 7,
  "executionPrice": 72000
}
```

### 완료 기준

* [ ] WebSocket 엔드포인트가 설계되었다.
* [ ] 체결 알림 메시지 구조가 정의되었다.
* [ ] 주문 상태 변경 알림 기준이 정의되었다.
* [ ] 프론트엔드 수신 구조가 설계되었다.
* [ ] README와 PPT용 실시간 알림 흐름이 정리되었다.

### 메모

실시간성을 보여주는 핵심 화면 기능이다.

---

## TD-018: 2단계 대량 주문 성능 테스트 설계 미작성

### 상태

Planned

### 우선순위

P2

### 영향도

Medium

### 담당자

TBD

### 발견일

2026-05-11

### 목표 해결일

TBD

### 관련 단계

* 2단계 증권 실시간

### 관련 영역

* Performance
* Testing
* Trading
* Portfolio

### 관련 파일

* 향후 `docs/PERFORMANCE.md`
* 향후 `docs/design-docs/trading-performance-design.md`

### 문제

2단계에서는 실시간 주문 처리 성능을 보여주기 위해 초당 주문 수 기준 테스트가 필요하다.

하지만 아직 성능 테스트 기준과 측정 항목이 정의되어 있지 않다.

### 영향

실시간 처리 프로젝트임에도 처리량, 지연시간, Queue 대기 건수 같은 수치를 보여주기 어렵다.

포트폴리오와 PPT에서 “성능 개선”을 설명하기 어려워진다.

### 해결 방향

대량 주문 성능 테스트 기준을 설계한다.

측정 항목:

```txt
초당 주문 수
평균 주문 접수 시간
평균 체결 처리 시간
최대 체결 지연 시간
Queue 대기 건수
체결 성공 건수
미체결 건수
부분체결 건수
WebSocket 알림 전송 건수
```

비교 전략:

```txt
DB_DIRECT
QUEUE_BASED
MEMORY_ORDER_BOOK
SYMBOL_PARTITIONED
```

성능 비교 표:

```md
| 방식 | 초당 주문 수 | 평균 처리 시간 | 병목 지점 | 개선 내용 |
|---|---:|---:|---|---|
| DB_DIRECT | 100건 | 측정값 | DB 조회/수정 | 기본 구현 |
| QUEUE_BASED | 100건 | 측정값 | 단일 Queue | 주문 접수/처리 분리 |
| MEMORY_ORDER_BOOK | 100건 | 측정값 | 메모리 매칭 | DB 조회 감소 |
| SYMBOL_PARTITIONED | 100건 | 측정값 | 종목별 처리 | 병렬 처리 |
```

### 완료 기준

* [ ] 성능 측정 항목이 정의되었다.
* [ ] 비교 전략이 정의되었다.
* [ ] VirtualOrderGenerator와 연결 기준이 정리되었다.
* [ ] README와 PPT에 들어갈 표 형식이 정의되었다.
* [ ] 실시간 처리 성능 개선 메시지가 정리되었다.

### 메모

2단계의 포트폴리오 핵심 결과물이다.

---

## 10. 기술부채 해결 절차

기술부채를 해결할 때는 다음 순서로 진행한다.

1. 기술부채 항목을 확인한다.
2. 현재 프로젝트 단계와 관련 있는지 확인한다.
3. 영향도와 우선순위를 다시 검토한다.
4. 관련 코드와 문서를 확인한다.
5. 필요한 경우 설계 문서를 작성한다.
6. 필요한 경우 실행 계획을 작성한다.
7. 작은 단위로 수정한다.
8. 테스트 또는 수동 검증을 수행한다.
9. 관련 문서를 갱신한다.
10. README 또는 PPT 반영 여부를 확인한다.
11. 상태를 `Resolved`로 변경한다.
12. 해결 내용과 남은 문제를 기록한다.

---

## 11. 현재 해결 우선순위

현재는 1단계 배치 성능 개선 프로젝트를 먼저 완성한다.

우선순위는 다음과 같다.

```txt
1. TD-005 GROUP_BY_BULK_SAVE 구현
2. TD-006 GROUP_BY_BULK_INDEX 구현 및 인덱스 적용 비교
3. TD-021 EXPLAIN ANALYZE 실행 계획 확인
4. TD-022 Payment 원천 데이터와 Settlement 결과 대사 기능 구현
5. TD-023 RUNNING 상태 기준 중복 실행 방지
6. TD-024 날짜 파티셔닝 고도화 항목 문서화
7. TD-001 DB 설정 불일치 해결
8. TD-007 DB 문서 갱신
9. TD-009 성능 비교 결과 문서화
10. TD-010 정산 처리 전략 구조 분리
11. TD-011 프론트엔드 구조 정리
12. TD-012 공통 예외 응답 구조 정리
13. TD-013~TD-018 2단계 증권 실시간 처리 설계
```

현재 가장 중요한 기준은 다음이다.

```txt
1단계에서 BASIC_LOOP 기준선과 GROUP_BY_QUERY 개선에 이어,
벌크 저장, 인덱스 적용, 실행 계획 확인, 대사, RUNNING 중복 실행 방지까지 구현해
성능 개선과 정합성/안정성 확보 과정을 수치와 검증 기록으로 보여주는 것.

이후 2단계에서 실시간 시세 API 기반 모의 주문·체결 처리로 확장해
증권사 특화 포트폴리오 메시지를 완성하는 것.
```

---

## 12. 기술부채 리뷰 체크리스트

정기적으로 다음을 확인한다.

* [ ] P0 또는 P1 항목이 방치되고 있지 않은가?
* [ ] 데이터 정합성에 영향을 주는 항목이 남아 있지 않은가?
* [ ] 성능 개선 프로젝트의 핵심 메시지를 약하게 만드는 항목이 있는가?
* [ ] 테스트 부족으로 정산 결과 신뢰도가 낮아지지 않는가?
* [ ] DB 설정과 실행 가이드가 일치하는가?
* [ ] DB 문서가 실제 Entity와 일치하는가?
* [ ] 배치 실패 시 원인을 추적할 수 있는가?
* [ ] 재실행 시 중복 정산이 발생하지 않는가?
* [ ] 2단계 증권 실시간 처리 기획이 문서에 반영되어 있는가?
* [ ] 실시간 시세 API 연동 실패 시 대체 흐름이 있는가?
* [ ] 모의 주문 생성기를 통해 대량 주문 상황을 재현할 수 있는가?
* [ ] 주문 체결 결과와 잔고/예수금 정합성 기준이 정의되어 있는가?
* [ ] README와 PPT에 반영해야 할 기술부채가 남아 있는가?
* [ ] 해결된 항목이 문서에 반영되었는가?
* [ ] 더 이상 필요 없는 항목이 정리되었는가?

---

## 13. 에이전트 작업 지침

AI 에이전트가 작업 중 기술부채를 발견하면 다음을 따른다.

* 즉시 수정 가능한 작은 문제는 수정한다.
* 범위가 큰 문제는 이 문서에 기록한다.
* 현재는 1단계 배치 성능 개선을 우선한다.
* 2단계 증권 실시간 처리는 기획이 확정된 확장 범위로 관리한다.
* 1단계 완료 전 2단계 구현을 무리하게 시작하지 않는다.
* 안정성, 데이터 정합성, 성능 측정과 관련된 문제는 우선순위를 높게 둔다.
* 실시간 시세 API, 주문 Queue, OrderBook, 체결 정합성 관련 문제는 2단계 기술부채로 기록한다.
* 임시 구현을 추가했다면 반드시 기술부채로 남긴다.
* 기술부채를 기록할 때는 문제, 영향, 해결 방향, 완료 기준을 함께 작성한다.
* 해결한 기술부채는 상태를 `Resolved`로 변경하고 해결 내용을 남긴다.
* 문서와 코드가 다르면 현재 정상 동작하는 코드를 기준으로 판단한다.
* 기술부채를 숨기지 않는다.
* 포트폴리오와 PPT에서 설명하기 어려운 구조는 기술부채로 본다.

---

## 14. 핵심 원칙 요약

* 기술부채는 실패가 아니라 관리 대상이다.
* 기록하지 않은 기술부채는 반복해서 문제를 만든다.
* 현재는 1단계 배치 성능 개선 완성이 최우선이다.
* 2단계는 실시간 주식 시세 API 기반 모의 주문·체결 처리로 확정한다.
* 안정성, 데이터 정합성, 성능 개선과 관련된 문제는 우선적으로 해결한다.
* DB 설정 불일치는 가장 먼저 정리한다.
* 테스트 없는 성능 개선은 신뢰도가 낮다.
* 성능 개선은 반드시 수치로 남긴다.
* 문서와 실제 코드가 다르면 문서를 갱신한다.
* 완료된 항목도 삭제하지 않고 이력으로 보관한다.
* 1단계는 금융권 공통 역량을 보여준다.
* 2단계는 증권사 특화 역량을 보여준다.
