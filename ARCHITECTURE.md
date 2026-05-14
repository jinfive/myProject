
# Architecture

## 1. 목적

이 문서는 **금융 거래 처리 성능 최적화 포트폴리오 프로젝트**의 전체 구조와 주요 설계 방향을 설명한다.

이 프로젝트는 단순 CRUD 서비스가 아니라, 금융 IT에서 중요한 다음 세 가지 키워드를 코드와 문서로 보여주는 것을 목표로 한다.

- 안정성
- 데이터 정합성
- 실시간 처리

프로젝트는 하나의 저장소 안에서 단계적으로 확장한다.

```txt
1단계: 대용량 금융 거래 정산·대사 배치 성능 최적화
2단계: 실시간 주식 주문·체결 및 잔고 정합성 처리
````

현재는 1단계인 **정산 배치 성능 비교용 MVP**를 먼저 구현한다.

이 문서는 에이전트와 개발자가 코드를 수정하기 전에 다음 내용을 이해하도록 돕는다.

* 프로젝트의 전체 구조
* 현재 구현된 기능과 앞으로 확장할 기능
* 주요 계층의 역할
* 정산 배치 요청과 응답 흐름
* 성능 개선 전략별 책임 분리
* 데이터 정합성 관리 기준
* 새 파일을 추가할 위치
* 변경 시 지켜야 할 아키텍처 원칙

---

## 2. 프로젝트 방향

이 프로젝트는 포트폴리오와 발표 자료 제작을 전제로 한다.

따라서 기능을 많이 추가하는 것보다, 다음 흐름을 명확히 보여주는 것이 중요하다.

```txt
문제 상황 정의
→ 기본 구현
→ 병목 확인
→ 개선 전략 적용
→ 성능 수치 비교
→ 안정성/정합성 관점 정리
→ README와 PPT로 설명 가능하게 정리
```

---

## 3. 전체 프로젝트 단계

### 3.1 1단계: 배치 처리 성능 개선

1단계는 대용량 금융 거래 데이터를 일자별로 정산하고, 처리 방식별 성능 차이를 비교하는 프로젝트이다.

핵심 목표:

* 대용량 결제/취소 데이터 생성
* 일자별 정산 배치 실행
* BASIC_LOOP 방식 기준선 구현
* GROUP_BY_QUERY 방식 개선
* GROUP_BY_BULK_SAVE 방식 개선
* GROUP_BY_BULK_INDEX 방식 개선
* 배치 실행 이력 관리
* 실패 이력 관리
* 정산 결과와 원천 데이터의 대사
* README와 PPT에 성능 개선 결과 정리

현재 구현 상태:

```txt
정산 배치 성능 비교용 MVP 중 GROUP_BY_QUERY 구현 완료
```

현재 구현된 기능:

* Spring Boot 백엔드 구조 구성
* Merchant, Payment, Settlement, BatchJobHistory 도메인 구현
* 더미 데이터 자동 생성

    * 가맹점 100개
    * 결제 데이터 100,000건
    * 특정일 거래 약 70,000건
* BASIC_LOOP 정산 배치 구현
* GROUP_BY_QUERY 정산 배치 구현
* 정산 실행 API 구현
* 정산 결과 조회 API 구현
* 배치 이력 조회 API 구현
* React 대시보드 구현
* 프론트 빌드 성공
* 백엔드 테스트 명령 성공

현재 API:

```txt
POST /api/settlements/run
GET  /api/settlements
GET  /api/batch-histories
```

아직 필요한 작업:

* 벌크 저장 최적화
* 인덱스 적용 및 성능 비교
* EXPLAIN ANALYZE 실행 계획 확인
* 대사 기능 추가
* RUNNING 중복 실행 방지
* API 응답 검증 테스트 추가
* DB 설정 정리
* 실제 테이블 기준 DB 문서 갱신
* 실패 이력 저장
* 재실행 정책 정리
* README 성능 개선 결과 정리

---

### 3.2 2단계: 증권 실시간 처리 성능 개선

2단계는 실시간으로 들어오는 주식 주문을 처리하고, 가격 우선·시간 우선 원칙에 따라 체결하는 모의 증권 거래 시스템이다.

2단계는 1단계 배치 프로젝트가 완료된 뒤 진행한다.

핵심 목표:

* 실시간 시세 또는 모의 시세 처리
* 매수/매도 주문 접수
* 주문 Queue 처리
* 메모리 기반 OrderBook 구성
* 가격 우선·시간 우선 체결
* 부분체결/전체체결/미체결 처리
* 주문 상태 변경
* 잔고와 예수금 반영
* WebSocket 체결 알림
* 가상 주문 생성기를 통한 대량 주문 테스트
* 처리 지연시간 측정

2단계는 증권사 지원 시 다음 역량을 보여주기 위한 확장이다.

* 실시간 처리
* 동시성 제어
* 주문 정합성
* 잔고/예수금 정합성
* 이벤트 기반 처리
* 사용자 실시간 알림

---

## 4. 현재 프로젝트 구조

현재 프로젝트는 Spring Boot 백엔드와 React 프론트엔드로 구성한다.

현재 주요 파일은 다음과 같다.

```txt
src/main/java/com/example/myproject
├── api
│   └── SettlementController.java
│
├── service
│   └── BasicLoopSettlementService.java
│
├── data
│   └── DummyDataService.java
│
├── domain 또는 entity
│   ├── Merchant
│   ├── Payment
│   ├── Settlement
│   └── BatchJobHistory
│
└── repository
    ├── MerchantRepository
    ├── PaymentRepository
    ├── SettlementRepository
    └── BatchJobHistoryRepository
```

프론트엔드 주요 파일:

```txt
frontend/src/main.jsx
```

현재 구조는 BASIC_LOOP 기준선 구현에 맞춰져 있다.

앞으로 성능 개선 전략이 추가되면 Service와 Repository 구조를 더 명확히 분리한다.

---

## 5. 목표 패키지 구조

향후 구조는 다음과 같이 정리한다.

```txt
src/main/java/com/example/myproject
├── MyProjectApplication.java
│
├── settlement
│   ├── api
│   ├── service
│   ├── strategy
│   ├── repository
│   ├── entity
│   └── dto
│
├── merchant
│   ├── repository
│   ├── entity
│   └── dto
│
├── payment
│   ├── repository
│   ├── entity
│   ├── dto
│   └── data
│
├── batch
│   ├── repository
│   ├── entity
│   ├── dto
│   └── service
│
├── reconciliation
│   ├── service
│   ├── repository
│   ├── entity
│   └── dto
│
├── common
│   ├── exception
│   ├── response
│   ├── type
│   └── util
│
└── config
```

초기에는 현재 구조를 유지하되, 기능이 커질수록 위 구조로 점진적으로 이동한다.

중요한 원칙은 다음과 같다.

> 정산 실행, 데이터 조회, 성능 측정, 배치 이력, 실패 이력, 대사 검증의 책임을 분리한다.

---

## 6. 주요 도메인

### Merchant

가맹점을 나타내는 도메인이다.

역할:

* 가맹점 기본 정보 관리
* 가맹점별 수수료율 관리
* Payment와 Settlement의 기준 식별자 역할

---

### Payment

결제와 취소 거래 데이터를 나타내는 도메인이다.

역할:

* 결제 완료 데이터 저장
* 취소 완료 데이터 저장
* 정산 대상 원천 데이터 역할
* 성능 테스트를 위한 대용량 데이터 역할

Payment는 실제 결제 승인 기능을 구현하기 위한 도메인이 아니다.

이 프로젝트에서는 대용량 정산 배치와 성능 개선을 검증하기 위한 원천 거래 데이터이다.

---

### Settlement

가맹점별 정산 결과를 나타내는 도메인이다.

역할:

* 정산일자 저장
* 가맹점별 총 결제금액 저장
* 가맹점별 총 취소금액 저장
* 순매출 저장
* 수수료 저장
* 최종 정산금액 저장
* 정산 처리 전략 저장

중복 정산 방지를 위해 같은 가맹점과 같은 정산일자에 대해 중복 결과가 생성되지 않도록 해야 한다.

---

### BatchJobHistory

배치 실행 이력을 나타내는 도메인이다.

역할:

* 배치 실행 시작 시간 기록
* 배치 실행 종료 시간 기록
* 실행 시간 기록
* 처리 전략 기록
* 처리 건수 기록
* 성공 건수 기록
* 실패 건수 기록
* 배치 상태 기록

BatchJobHistory는 이 프로젝트의 포트폴리오 핵심 도메인이다.

성능 개선 전후 비교를 위해 반드시 관리한다.

---

### SettlementError 또는 BatchError

정산 실패 이력을 나타내는 도메인이다.

현재는 미구현 또는 약한 영역이다.

향후 역할:

* 실패한 정산일자 기록
* 실패한 가맹점 기록
* 실패 원인 기록
* 배치 실행 이력과 연결
* 재실행 대상 여부 관리

---

### Reconciliation

정산 결과와 원천 거래 데이터를 대사하는 도메인이다.

현재는 향후 확장 대상이다.

역할:

* Payment 기준 집계 금액과 Settlement 결과 비교
* 총 결제금액 일치 여부 검증
* 총 취소금액 일치 여부 검증
* 최종 정산금액 일치 여부 검증
* 불일치 발생 시 오류 이력 저장

대사 기능은 금융권 포트폴리오에서 데이터 정합성을 보여주기 위한 핵심 기능이다.

---

## 7. 계층 역할

### Controller / API

외부 요청을 받는 계층이다.

역할:

* HTTP 요청을 받는다.
* 요청 DTO를 검증한다.
* Service를 호출한다.
* 응답 DTO를 반환한다.
* 정산 배치 실행, 정산 결과 조회, 배치 이력 조회 요청을 처리한다.

현재 주요 Controller:

```txt
SettlementController
```

하지 말아야 할 것:

* 정산 금액을 직접 계산하지 않는다.
* Payment 데이터를 직접 조회하지 않는다.
* Entity를 그대로 응답하지 않는다.
* 성능 측정 로직을 직접 처리하지 않는다.
* 복잡한 조건 분기를 Controller에 넣지 않는다.

---

### Service

핵심 비즈니스 로직을 처리하는 계층이다.

역할:

* 정산 배치의 전체 흐름을 관리한다.
* 처리 전략을 선택한다.
* 정산 대상 데이터를 조회한다.
* 가맹점별 정산 금액을 계산한다.
* 정산 결과를 저장한다.
* 배치 실행 이력을 저장한다.
* 실패 이력을 저장한다.
* 트랜잭션을 관리한다.

현재 주요 Service:

```txt
BasicLoopSettlementService
DummyDataService
```

하지 말아야 할 것:

* HTTP 요청/응답 객체에 직접 의존하지 않는다.
* 화면 표시 방식에 의존하지 않는다.
* 모든 처리 전략을 하나의 메서드에 몰아넣지 않는다.
* Repository의 단순 조회 책임까지 모두 대신하지 않는다.

---

### Strategy

정산 처리 전략을 분리하는 계층이다.

향후 추가할 구조이다.

역할:

* BASIC_LOOP 전략 구현
* GROUP_BY_QUERY 전략 구현
* GROUP_BY_BULK_SAVE 전략 구현
* GROUP_BY_BULK_INDEX 전략 구현
* 전략별 성능 비교가 가능하도록 처리 흐름 분리

권장 구조:

```txt
settlement/strategy
├── SettlementStrategy.java
├── BasicLoopSettlementStrategy.java
├── GroupByQuerySettlementStrategy.java
├── BulkSaveSettlementStrategy.java
└── IndexAppliedSettlementStrategy.java
```

전략 분리 이유:

* 성능 개선 전후 비교가 명확해진다.
* README와 PPT에서 처리 방식 차이를 설명하기 쉬워진다.
* 특정 전략 수정이 다른 전략에 영향을 주지 않는다.

---

### Repository

데이터베이스와 통신하는 계층이다.

역할:

* Entity 조회, 저장, 수정, 삭제
* 정산 대상 Payment 조회
* GROUP BY 집계 쿼리 작성
* 중복 정산 여부 확인
* 배치 이력 조회
* 실패 이력 조회

하지 말아야 할 것:

* 정산 정책을 직접 결정하지 않는다.
* 수수료 계산 로직을 담당하지 않는다.
* API 응답 형식을 만들지 않는다.
* 화면에 필요한 데이터를 무리하게 조립하지 않는다.

성능 개선 시 Repository에서 고려할 것:

* 필요한 컬럼만 조회
* DTO Projection 사용
* GROUP BY 집계 쿼리 사용
* 불필요한 Entity 전체 로딩 제거
* 인덱스를 고려한 조회 조건 작성

---

### Entity / Domain

비즈니스 개념과 데이터베이스 테이블을 표현하는 계층이다.

역할:

* 핵심 데이터 구조 표현
* 데이터베이스 테이블 매핑
* 도메인 상태값 관리
* 필요한 경우 최소한의 도메인 메서드 제공

주의할 점:

* API 응답으로 직접 반환하지 않는다.
* 외부 요청 DTO와 직접 연결하지 않는다.
* 금액 계산에는 double 또는 float를 사용하지 않는다.
* 금액 필드는 BigDecimal을 사용한다.
* 상태값은 문자열보다 Enum을 우선 사용한다.
* 중복 정산 방지를 위한 DB 제약조건을 고려한다.

---

### DTO

외부 입출력 데이터를 표현하는 객체이다.

역할:

* API 요청 데이터 표현
* API 응답 데이터 표현
* Entity와 외부 응답 형식 분리
* 화면에 필요한 필드만 전달

원칙:

* 요청 DTO와 응답 DTO를 분리한다.
* Entity를 그대로 반환하지 않는다.
* 정산 결과, 배치 이력, 성능 지표는 응답 DTO로 변환한다.
* 프론트 화면에 필요한 데이터만 포함한다.

---

### Config

프로젝트 설정을 관리하는 영역이다.

예시:

* 데이터베이스 설정
* JPA 설정
* CORS 설정
* 환경별 설정
* 배치 관련 설정
* 성능 측정 관련 설정

현재 반드시 정리해야 할 부분:

```txt
application.yml DB 설정과 compose.yaml DB 설정 불일치
```

이 설정 불일치는 실행 안정성에 영향을 줄 수 있으므로 우선 정리한다.

---

### Common

여러 영역에서 공통으로 사용하는 코드를 둔다.

예시:

* 공통 예외
* 공통 응답 형식
* 공통 Enum
* 날짜 유틸리티
* 금액 계산 유틸리티
* 성능 측정 유틸리티

주의할 점:

* 무분별하게 common에 넣지 않는다.
* 특정 도메인에만 필요한 코드는 해당 도메인 패키지 안에 둔다.
* common은 여러 도메인에서 반복 사용되는 코드만 둔다.

---

## 8. 기본 요청 흐름

일반적인 API 요청 흐름은 다음과 같다.

```txt
Client
→ Controller
→ Service
→ Repository
→ Database
→ Repository
→ Service
→ Controller
→ Client
```

Controller는 요청과 응답만 담당한다.

정산 계산, 성능 측정, 배치 이력 저장, 실패 이력 저장은 Service 또는 Strategy 계층에서 처리한다.

---

## 9. 정산 배치 실행 흐름

현재 BASIC_LOOP 기준 정산 배치 실행 흐름은 다음과 같다.

```txt
Client
→ SettlementController
→ BasicLoopSettlementService
→ BatchJobHistory 시작 이력 생성
→ 정산 대상 Payment 전체 조회
→ Java 반복문으로 가맹점별 집계
→ 수수료 계산
→ 정산금액 계산
→ Settlement 저장
→ BatchJobHistory 종료 이력 업데이트
→ 응답 반환
```

향후 목표 흐름은 다음과 같다.

```txt
Client
→ SettlementController
→ SettlementBatchService
→ SettlementStrategy 선택
→ BatchJobHistory 시작 이력 생성
→ 전략별 정산 처리
→ Settlement 저장
→ Reconciliation 검증
→ SettlementError 또는 BatchError 저장
→ BatchJobHistory 종료 이력 업데이트
→ 응답 반환
```

---

## 10. 처리 전략 구조

성능 개선 전후 비교를 위해 정산 처리 전략을 명확히 구분한다.

처리 전략은 Enum으로 관리한다.

```txt
BASIC_LOOP
GROUP_BY_QUERY
GROUP_BY_BULK_SAVE
GROUP_BY_BULK_INDEX
```

---

### 10.1 BASIC_LOOP

현재 구현된 기준선 전략이다.

흐름:

```txt
특정 정산일자의 Payment 전체 조회
→ Java 반복문으로 가맹점별 결제금액과 취소금액 집계
→ 수수료 계산
→ 정산금액 계산
→ Settlement 저장
→ 실행 시간 기록
```

목적:

* 성능 개선 전 기준선 확보
* 대용량 데이터 처리 시 병목 확인
* 이후 전략과 처리 시간 비교

문제점:

* Payment Entity 전체 로딩
* 애플리케이션 메모리 사용 증가
* 데이터가 증가할수록 처리 시간 증가
* 반복문 기반 집계로 DB 집계 기능을 활용하지 못함

---

### 10.2 GROUP_BY_QUERY

향후 구현할 쿼리 개선 전략이다.

흐름:

```txt
Payment 전체 조회 제거
→ DB GROUP BY로 가맹점별 결제금액/취소금액 집계
→ 필요한 집계 결과만 Service로 전달
→ 정산금액 계산
→ Settlement 저장
→ 실행 시간 기록
```

목적:

* 불필요한 Entity 전체 로딩 제거
* DB 집계 기능 활용
* 조회 데이터량 감소
* BASIC_LOOP 대비 처리 시간 개선

---

### 10.3 GROUP_BY_BULK_SAVE

향후 구현할 저장 성능 개선 전략이다.

흐름:

```txt
Settlement를 한 건씩 저장하지 않음
→ 정산 결과 목록을 생성
→ saveAll 또는 batch insert 적용
→ 반복적인 DB 저장 호출 감소
→ 실행 시간 기록
```

목적:

* 저장 구간 병목 완화
* 반복적인 save 호출 감소
* 대량 정산 결과 저장 성능 개선

---

### 10.4 GROUP_BY_BULK_INDEX

향후 구현할 인덱스 적용 전략이다.

흐름:

```txt
정산 조건에 자주 사용되는 컬럼에 인덱스 적용
→ 동일 데이터 건수와 동일 정산일자 기준으로 배치 실행
→ 인덱스 적용 전후 처리 시간 비교
→ 결과를 README와 PPT에 기록
```

우선 고려할 인덱스:

```txt
payment_date
merchant_id
payment_status
payment_type
payment_date + merchant_id
payment_date + merchant_id + payment_type
settlement_date + merchant_id
```

목적:

* 정산 대상 데이터 조회 속도 개선
* GROUP BY 집계 조건 최적화
* 중복 정산 검증 속도 개선

---

## 11. 데이터 정합성 원칙

금융 프로젝트에서 정합성은 가장 중요한 기준 중 하나이다.

이 프로젝트에서는 다음 정합성 원칙을 지킨다.

* 같은 가맹점과 같은 정산일자는 중복 정산되지 않아야 한다.
* 총 결제금액과 총 취소금액은 원천 Payment 데이터 기준과 일치해야 한다.
* 정산금액은 정해진 계산식에 따라 일관되게 계산되어야 한다.
* 배치 실행 이력은 성공과 실패 여부와 관계없이 남아야 한다.
* 실패한 정산 건은 추적 가능해야 한다.
* 재실행 시 기존 결과와 충돌하지 않아야 한다.

정산 계산식:

```txt
순매출 = 총 결제금액 - 총 취소금액
수수료 = 순매출 × 가맹점 수수료율
정산금액 = 순매출 - 수수료
```

금액 계산 원칙:

* double 사용 금지
* float 사용 금지
* BigDecimal 사용
* 반올림 기준 명확화
* 수수료 계산 기준 명확화

---

## 12. 트랜잭션 원칙

정산 배치 처리 과정에서는 데이터 정합성을 위해 트랜잭션을 적용한다.

기본 원칙:

* 정산 결과 저장과 배치 이력 저장은 하나의 업무 흐름으로 관리한다.
* 배치 실패 시에도 실행 이력은 남아야 한다.
* 특정 가맹점 정산 실패 시 실패 이력으로 분리한다.
* 성공한 가맹점 정산 결과는 보존한다.
* 실패한 가맹점은 재처리할 수 있어야 한다.

주의할 점:

* 전체 배치를 하나의 트랜잭션으로 묶으면 일부 실패 시 전체 결과가 사라질 수 있다.
* 배치 실행 이력은 실패 상황에서도 남도록 별도 저장 전략을 고려한다.
* 가맹점 단위 부분 실패 처리 여부는 구현 단계에서 명확히 정한다.

---

## 13. 안정성 원칙

이 프로젝트에서 안정성은 다음 의미를 가진다.

* 같은 입력에 대해 같은 정산 결과가 나온다.
* 배치 실패 시 원인을 추적할 수 있다.
* 중복 실행해도 데이터가 무너너지 않는다.
* 설정 불일치로 실행 환경이 달라지지 않는다.
* 테스트로 핵심 계산 로직을 검증한다.
* 배치 실행 결과를 이력으로 확인할 수 있다.

현재 우선 개선해야 할 안정성 항목:

```txt
1. application.yml과 compose.yaml DB 설정 일치
2. 대사 기능 추가
3. RUNNING 중복 실행 방지
4. 배치 실행 실패 이력 저장
5. 실제 테이블 기준 DB 문서 갱신
```

---

## 14. 성능 측정 원칙

이 프로젝트는 성능 개선 과정을 보여주는 것이 핵심이다.

따라서 모든 정산 배치 실행은 성능 측정이 가능해야 한다.

기록해야 할 항목:

* 데이터 건수
* 정산일자
* 처리 전략
* 실행 시작 시간
* 실행 종료 시간
* 총 실행 시간
* 처리 건수
* 성공 건수
* 실패 건수
* 적용된 개선 방식

성능 비교 시 지켜야 할 기준:

* 같은 데이터 건수로 비교한다.
* 같은 정산일자로 비교한다.
* 같은 로컬 환경에서 비교한다.
* 같은 DB 설정에서 비교한다.
* 절대 성능보다 개선 전후 차이를 중심으로 기록한다.
* README와 PPT에 성능 개선 결과 표를 남긴다.

성능 비교 표 예시:

```md
| 전략 | 데이터 건수 | 처리 시간 | 개선 내용 |
|---|---:|---:|---|
| BASIC_LOOP | 100,000건 | 측정값 | 전체 조회 후 Java 반복문 집계 |
| GROUP_BY_QUERY | 100,000건 | 측정값 | DB GROUP BY 집계 |
| GROUP_BY_BULK_SAVE | 100,000건 | 측정값 | DB GROUP BY 집계 + 정산 결과 일괄 저장 |
| GROUP_BY_BULK_INDEX | 100,000건 | 측정값 | DB GROUP BY 집계 + 일괄 저장 + 조회 조건 인덱스 적용 |
```

---

## 15. 프론트엔드 구조

현재 프론트엔드는 React 기반 대시보드로 구성한다.

현재 주요 기능:

* 정산일자 선택
* BASIC_LOOP 정산 배치 실행
* 정산 결과 테이블 표시
* 실행 시간 표시
* 배치 이력 표시

향후 추가할 화면:

### 15.1 성능 비교 화면

표시 항목:

* BASIC_LOOP 처리 시간
* GROUP_BY_QUERY 처리 시간
* GROUP_BY_BULK_SAVE 처리 시간
* GROUP_BY_BULK_INDEX 처리 시간
* 개선율

### 15.2 대사 결과 화면

표시 항목:

* 정산일자
* 원천 거래 기준 금액
* Settlement 기준 금액
* 일치 여부
* 불일치 원인

### 15.3 실패 이력 화면

표시 항목:

* 실패 발생 시간
* 정산일자
* 가맹점
* 실패 원인
* 재처리 가능 여부

---

## 16. 2단계 실시간 처리 확장 구조

2단계는 1단계 완료 후 같은 프로젝트 안에서 추가한다.

증권 실시간 처리 구조는 다음과 같이 설계한다.

```txt
Client
→ OrderController
→ OrderService
→ OrderQueue
→ MatchingEngine
→ OrderBook
→ ExecutionService
→ AccountService
→ Database
→ WebSocket Notification
→ Client
```

주요 도메인:

```txt
Account
Stock
Order
OrderBook
Execution
Holding
CashBalance
OrderEvent
ExecutionEvent
Notification
```

핵심 원칙:

* 주문 접수와 체결 처리를 분리한다.
* 주문은 Queue에 적재한다.
* 체결은 메모리 기반 OrderBook에서 처리한다.
* 같은 종목의 주문은 순서를 보장한다.
* 체결 결과 저장, 주문 상태 변경, 잔고 변경, 예수금 변경은 트랜잭션으로 처리한다.
* 체결 결과는 WebSocket으로 사용자에게 전달한다.

2단계는 현재 바로 구현하지 않는다.

현재 우선순위는 1단계 배치 성능 개선 프로젝트를 완성하는 것이다.

---

## 17. 새 파일 추가 기준

새 파일은 책임에 맞는 위치에 추가한다.

예시:

```txt
정산 실행 API
→ settlement/api

정산 배치 로직
→ settlement/service

정산 처리 전략
→ settlement/strategy

정산 결과 Entity
→ settlement/entity

정산 결과 조회 DTO
→ settlement/dto

결제 데이터 조회 쿼리
→ payment/repository

더미 데이터 생성
→ payment/data 또는 payment/service

배치 실행 이력
→ batch

대사 검증
→ reconciliation

공통 예외
→ common/exception

공통 응답
→ common/response

설정 파일
→ config
```

파일을 추가하기 전에 다음을 확인한다.

* 이 코드가 특정 도메인에 속하는가?
* 여러 도메인에서 공통으로 쓰이는가?
* 정산 배치 흐름과 직접 관련 있는가?
* 성능 측정 또는 정합성 검증에 필요한 코드인가?
* 프로젝트 목적과 무관한 기능은 아닌가?

---

## 18. 테스트 기준

현재 테스트는 부족하다.

우선 다음 테스트를 추가한다.

```txt
1. 정산 금액 계산 테스트
2. 취소 금액 반영 테스트
3. 가맹점별 수수료 계산 테스트
4. 중복 정산 방지 테스트
5. 정산 대상 데이터 없음 처리 테스트
6. 배치 실행 이력 저장 테스트
7. 정산 실행 API 응답 테스트
8. BASIC_LOOP와 GROUP_BY_QUERY 성능 비교 테스트
```

테스트 우선순위:

```txt
정합성 테스트
→ 안정성 테스트
→ API 테스트
→ 성능 비교 테스트
```

포트폴리오에서는 테스트 결과도 README에 정리한다.

---

## 19. 문서화 기준

이 프로젝트는 포트폴리오와 PPT 제작을 전제로 한다.

따라서 코드 구현만큼 문서화가 중요하다.

필수 문서:

```txt
README.md
docs/ARCHITECTURE.md
docs/API.md
docs/DB_SCHEMA.md
docs/PERFORMANCE.md
docs/TROUBLESHOOTING.md
docs/PORTFOLIO_SUMMARY.md
```

README에 반드시 포함할 내용:

* 프로젝트 목적
* 금융권 포트폴리오로서의 핵심 키워드
* 현재 구현 상태
* 주요 기능
* 아키텍처
* 정산 배치 흐름
* 데이터 정합성 관리
* 성능 개선 전략
* 성능 개선 결과 표
* 테스트 결과
* 트러블슈팅
* 향후 실시간 처리 확장 계획

최종 README 정리 기준:

```txt
1. 프로젝트 개요
2. 문제 상황
3. 데이터 규모
4. 정산 계산 기준
5. 처리 전략 비교
6. 성능 비교 결과
7. 실행 계획 분석 결과
8. 대사 기능과 정합성 검증
9. 안정성 개선
10. 기술적 의사결정
11. 한계와 고도화 방향
12. 실행 방법
13. 테스트 방법
14. 포트폴리오 요약
```

PPT에 반드시 포함할 내용:

* 문제 정의
* 기존 방식의 병목
* 개선 전략
* 아키텍처 흐름도
* 성능 비교 그래프
* 정합성 관리 방식
* 안정성 확보 방식
* 실시간 처리 확장 계획
* 배운 점

PPT 장표 기준:

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

README와 PPT는 항상 `문제 → 판단 → 액션 → 검증 → 결과` 흐름으로 작성한다.

---

## 20. 추가하지 않을 기능

1단계 배치 프로젝트에는 다음 기능을 추가하지 않는다.

```txt
회원가입
로그인
권한 관리
장바구니
상품 관리
주문 화면
실제 결제 승인 기능
외부 결제 API 연동
복잡한 관리자 기능
```

현재 프로젝트의 목적은 쇼핑몰이나 실제 결제 서비스 구현이 아니다.

1단계의 목적은 다음 질문에 답하는 것이다.

```txt
대용량 금융 거래 데이터를 배치로 정산할 때 어떤 병목이 있었고,
어떤 방식으로 성능을 개선했으며,
정산 결과의 정합성을 어떻게 보장했는가?
```

2단계의 목적은 다음 질문에 답하는 것이다.

```txt
실시간으로 들어오는 주문을 어떻게 순서 있게 처리하고,
체결 결과에 따라 잔고와 예수금을 어떻게 정합성 있게 반영했는가?
```

---

## 21. 현재 작업 우선순위

현재 기준으로 다음 순서로 작업한다.

```txt
1. GROUP_BY_QUERY 구현
2. BASIC_LOOP와 결과 동일성 검증
3. GROUP_BY_BULK_SAVE 구현
4. GROUP_BY_BULK_INDEX 구현
5. EXPLAIN ANALYZE로 실행 계획 확인
6. 대사 기능 추가
7. RUNNING 중복 실행 방지
8. 날짜 파티셔닝은 고도화 항목으로 문서화
```

이 순서는 임의로 바꾸지 않는다. 현재 GROUP_BY_QUERY와 BASIC_LOOP 결과 동일성 검증은 완료된 상태이므로 다음 구현 작업은 GROUP_BY_BULK_SAVE이다.

---

## 22. 아키텍처 원칙 요약

이 프로젝트에서 지켜야 할 원칙은 다음과 같다.

* Controller에 비즈니스 로직을 넣지 않는다.
* Entity를 API 응답으로 직접 반환하지 않는다.
* 정산 계산 로직은 Service 또는 Strategy에 둔다.
* 데이터 조회와 집계는 Repository에서 담당한다.
* 성능 비교를 위해 처리 전략을 명확히 구분한다.
* 금액 계산에는 BigDecimal을 사용한다.
* 상태값은 Enum으로 관리한다.
* 대량 데이터 처리 시 불필요한 전체 객체 로딩을 줄인다.
* 중복 정산 방지를 위해 애플리케이션 검증과 DB 제약조건을 함께 고려한다.
* 배치 실행 이력은 반드시 남긴다.
* 실패 이력은 추적 가능해야 한다.
* 성능 개선 결과는 README와 PPT에 수치로 정리한다.
* 1단계 배치 프로젝트를 먼저 완성한 뒤 2단계 실시간 처리로 확장한다.
