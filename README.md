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
