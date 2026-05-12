# AGENTS.md — 에이전트 작업 진입점

## 1. 목적

이 파일은 AI 에이전트가 프로젝트에서 작업을 시작할 때 참고하는 진입점이다.

AGENTS.md는 전체 매뉴얼이 아니다.  
프로젝트의 구조, 설계, 실행 계획, 품질 기준, 보안 기준이 어디에 있는지 안내하는 색인 역할을 한다.

세부 지침은 각 문서에서 관리한다.

---

## 2. 기본 작업 원칙

에이전트는 작업 전 다음 원칙을 따른다.

1. 관련 문서를 먼저 확인한다.
2. 기존 코드와 문서의 패턴을 우선한다.
3. 큰 변경은 실행 계획을 먼저 작성한다.
4. 보안, DB, 아키텍처 변경은 임의로 진행하지 않는다.
5. 문서와 코드가 다르면 현재 동작하는 코드를 기준으로 판단한다.
6. 변경 후 필요한 문서를 갱신한다.
7. 기술부채가 생기면 기록한다.
8. AGENTS.md에는 세부 내용을 계속 추가하지 않고, 반복되는 내용은 별도 문서로 분리한다.

---

## 3. 빠른 참조

| 목적 | 문서 |
|---|---|
| 전체 아키텍처 | [ARCHITECTURE.md](ARCHITECTURE.md) |
| 설계 원칙 | [docs/DESIGN.md](docs/DESIGN.md) |
| 프론트엔드 기준 | [docs/FRONTEND.md](docs/FRONTEND.md) |
| 보안/인증/인가 | [docs/SECURITY.md](docs/SECURITY.md) |
| 안정성/운영 기준 | [docs/RELIABILITY.md](docs/RELIABILITY.md) |
| 품질 기준 | [docs/QUALITY_SCORE.md](docs/QUALITY_SCORE.md) |
| 실행 계획 작성법 | [docs/PLANS.md](docs/PLANS.md) |
| 제품 감각/기획 기준 | [docs/PRODUCT_SENSE.md](docs/PRODUCT_SENSE.md) |
| 제품 스펙 색인 | [docs/product-specs/index.md](docs/product-specs/index.md) |
| 설계 문서 색인 | [docs/design-docs/index.md](docs/design-docs/index.md) |
| 활성 실행 계획 | [docs/exec-plans/active/](docs/exec-plans/active/) |
| 완료된 실행 계획 | [docs/exec-plans/completed/](docs/exec-plans/completed/) |
| 기술부채 추적 | [docs/exec-plans/tech-debt-tracker.md](docs/exec-plans/tech-debt-tracker.md) |
| 생성된 DB 스키마 | [docs/generated/db-schema.md](docs/generated/db-schema.md) |
| 외부 참고 자료 | [docs/references/](docs/references/) |

---

## 4. 문서 구조

```txt
AGENTS.md
ARCHITECTURE.md
docs/
├── design-docs/
│   ├── index.md
│   ├── core-beliefs.md
│   └── ...
├── exec-plans/
│   ├── active/
│   ├── completed/
│   └── tech-debt-tracker.md
├── generated/
│   └── db-schema.md
├── product-specs/
│   ├── index.md
│   ├── auth.md
│   ├── onboarding.md
│   ├── user-roles.md
│   └── ...
├── references/
│   ├── framework-reference-llms.txt
│   ├── database-reference-llms.txt
│   ├── deployment-reference-llms.txt
│   └── ...
├── DESIGN.md
├── FRONTEND.md
├── PLANS.md
├── PRODUCT_SENSE.md
├── QUALITY_SCORE.md
├── RELIABILITY.md
└── SECURITY.md