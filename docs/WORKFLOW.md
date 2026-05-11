# Workflow

## 1. 목적

이 문서는 프로젝트의 개발 작업 흐름을 정의한다.

다음 내용을 다룬다.

- 브랜치 전략
- 이슈 작성 기준
- Git worktree 작업 기준
- 실행 계획 관리 기준
- 기능 브랜치 작업 방식
- Pull Request 작성 기준
- 리뷰 후 후속 조치 방식
- 머지 기준
- 작업 완료 후 정리 방법
- 에이전트 작업 지침

---

## 2. 기본 원칙

개발 작업은 다음 원칙을 따른다.

- `main` 브랜치에서 직접 작업하지 않는다.
- `develop` 브랜치에서 직접 작업하지 않는다.
- 모든 작업은 이슈를 먼저 작성한 뒤 시작한다.
- 기능, 수정, 문서 작업은 별도 브랜치에서 진행한다.
- 큰 작업이나 에이전트 작업은 Git worktree에서 진행한다.
- 큰 기능 추가, DB 변경, 보안 변경, 아키텍처 변경은 `docs/exec-plans/active/`에 실행 계획을 작성한 뒤 진행한다.
- 작업 완료 후 실행 계획은 `docs/exec-plans/completed/`로 이동한다.
- 작업 완료 후 Pull Request를 작성한다.
- PR 리뷰 또는 후속 조치가 있으면 기존 작업 브랜치에서 바로 수정한다.
- 수정 후 다시 push하여 같은 PR에 반영한다.
- 머지 후 작업 브랜치와 worktree를 정리한다.

---

## 3. 브랜치 전략

기본 브랜치는 다음과 같다.

| 브랜치 | 역할 |
|---|---|
| `main` | 배포 또는 안정 버전 브랜치 |
| `develop` | 개발 통합 브랜치 |
| `feature/*` | 기능 개발 브랜치 |
| `fix/*` | 버그 수정 브랜치 |
| `refactor/*` | 리팩토링 브랜치 |
| `docs/*` | 문서 수정 브랜치 |
| `test/*` | 테스트 보완 브랜치 |

---

## 4. main 브랜치

`main` 브랜치는 안정된 상태를 유지한다.

원칙:

- 직접 커밋하지 않는다.
- 직접 작업하지 않는다.
- 검증된 변경만 머지한다.
- 배포 기준 브랜치로 사용할 수 있다.
- 긴급 수정이 필요한 경우에도 별도 브랜치에서 작업한 뒤 PR로 반영한다.

---

## 5. develop 브랜치

`develop` 브랜치는 개발 통합 브랜치이다.

원칙:

- 기능 브랜치는 `develop`에서 생성한다.
- 기능 작업 완료 후 `develop`으로 PR을 보낸다.
- `develop`은 다음 배포 후보가 되는 변경 사항을 모으는 브랜치이다.
- 직접 커밋하지 않는다.
- 여러 기능이 합쳐지므로 항상 안정성을 확인한다.

---

## 6. 기능 브랜치

기능 브랜치는 작업 단위별로 생성한다.

브랜치 이름 예시:

```txt
feature/123-login-api
feature/124-user-profile
fix/125-refresh-token-error
refactor/126-auth-service
docs/127-update-security-md
test/128-auth-service-test
````

브랜치 이름 규칙:

```txt
작업유형/이슈번호-작업내용
```

작업 유형:

| 유형         | 사용 상황         |
| ---------- | ------------- |
| `feature`  | 새로운 기능 추가     |
| `fix`      | 버그 수정         |
| `refactor` | 구조 개선, 리팩토링   |
| `docs`     | 문서 수정         |
| `test`     | 테스트 추가 또는 수정  |
| `chore`    | 설정, 빌드, 기타 작업 |

---

## 7. 이슈 작성 기준

모든 작업은 이슈를 먼저 작성한 뒤 시작한다.

이슈를 작성하는 이유:

* 작업 목적을 명확히 하기 위해
* 작업 범위를 제한하기 위해
* 나중에 변경 이력을 추적하기 위해
* PR과 작업 내용을 연결하기 위해
* 에이전트가 작업 맥락을 이해하게 하기 위해

---

## 8. 이슈 템플릿

```md
# 작업명

## 목적

이 작업이 필요한 이유를 작성한다.

## 작업 내용

- 
- 
- 

## 완료 기준

- [ ] 
- [ ] 
- [ ] 

## 참고 문서

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/PLANS.md`

## 관련 영역

- Backend
- Frontend
- Database
- Security
- Reliability
- Documentation

## 메모

추가로 기록할 내용을 작성한다.
```

---

## 9. Git Worktree 사용 원칙

큰 작업, 기능 작업, 에이전트 작업은 가능하면 Git worktree에서 진행한다.

worktree를 사용하는 이유:

* 기존 작업 디렉터리를 깨끗하게 유지하기 위해
* `main` 또는 `develop` 브랜치를 실수로 오염시키지 않기 위해
* 여러 작업을 독립적으로 진행하기 위해
* 에이전트가 작업하는 범위를 명확히 제한하기 위해
* 오류가 발생해도 원래 작업 디렉터리에 영향을 줄이기 위해

---

## 10. Worktree를 사용하는 경우

다음 작업은 worktree 사용을 권장한다.

* 새로운 기능 추가
* 버그 수정
* 인증/인가 변경
* DB 구조 변경
* 외부 API 연동
* 대규모 리팩토링
* 에이전트에게 코드 수정을 맡기는 작업
* 여러 브랜치를 동시에 확인해야 하는 작업
* 실험적인 구현

작은 문서 수정이나 단순 오타 수정은 기존 작업 디렉터리에서 진행할 수 있다.
단, 작업 중 충돌이나 오류 가능성이 있으면 worktree를 사용한다.

---

## 11. Worktree 생성 흐름

작업 시작 전 `develop`을 최신 상태로 만든다.

```bash
git checkout develop
git pull origin develop
```

새 worktree와 브랜치를 생성한다.

```bash
git worktree add ../프로젝트명-작업명 -b 브랜치명 develop
```

예시:

```bash
git worktree add ../project-login-api -b feature/123-login-api develop
git worktree add ../project-refresh-token-fix -b fix/124-refresh-token-error develop
git worktree add ../project-auth-refactor -b refactor/125-auth-service develop
```

생성한 worktree로 이동한다.

```bash
cd ../project-login-api
```

---

## 12. Worktree 작업 흐름

기본 작업 흐름은 다음과 같다.

```txt
1. 이슈 작성
2. develop 최신화
3. worktree 생성
4. 기능 브랜치 생성
5. 관련 문서 확인
6. 큰 작업이면 `docs/exec-plans/active/`에 실행 계획 작성
7. 작업 진행
8. 테스트 또는 수동 검증
9. 커밋
10. 원격 브랜치 push
11. Pull Request 작성
12. 리뷰 또는 후속 조치 반영
13. 머지
14. 실행 계획을 `completed`로 이동
15. worktree 정리
```

---

## 13. 실행 계획 관리

큰 작업은 구현 전에 실행 계획을 작성한다.

실행 계획을 작성하는 이유:

* 작업 목표와 범위를 명확히 하기 위해
* 에이전트가 임의로 작업 범위를 넓히지 않게 하기 위해
* 구현 순서와 검증 방법을 남기기 위해
* 작업 중 발생한 위험 요소와 후속 조치를 추적하기 위해
* 완료된 작업의 의사결정 기록을 보관하기 위해

---

## 18. 실행 계획을 작성하는 경우

다음 작업은 `docs/exec-plans/active/`에 실행 계획을 작성한 뒤 진행한다.

* 새로운 핵심 기능 추가
* DB 테이블, 컬럼, 관계 변경
* 인증 또는 인가 흐름 변경
* 보안에 영향을 주는 변경
* 아키텍처 구조 변경
* 외부 API 또는 외부 서비스 연동
* 대규모 리팩토링
* 여러 계층과 파일에 영향을 주는 작업
* 실패 시 롤백이 어려운 작업
* 에이전트에게 큰 작업을 맡기는 경우

작은 오타 수정, 단순 문서 수정, 작은 버그 수정은 실행 계획 없이 진행할 수 있다.  
단, 작은 작업이라도 보안, DB, 권한, 배포에 영향을 주면 실행 계획을 작성한다.

---

## 19. 실행 계획 작성 위치

진행 중인 실행 계획은 다음 위치에 작성한다.

```txt
docs/exec-plans/active/
```

예시:

```txt
docs/exec-plans/active/123-login-api.md
docs/exec-plans/active/124-refresh-token-fix.md
docs/exec-plans/active/125-auth-refactor.md
```

작업이 완료되면 해당 실행 계획을 다음 위치로 이동한다.

```txt
docs/exec-plans/completed/
```

예시:

```txt
docs/exec-plans/active/123-login-api.md
↓
docs/exec-plans/completed/123-login-api.md
```

---

## 20. 실행 계획 템플릿

```md
# 실행 계획: 작업명

## 관련 이슈

- #

## 목표

이 작업의 목표를 작성한다.

## 작업 범위

포함하는 작업:

- 
- 

포함하지 않는 작업:

- 
- 

## 관련 문서

- `AGENTS.md`
- `ARCHITECTURE.md`
- `docs/WORKFLOW.md`
- `docs/PLANS.md`

## 구현 순서

1.
2.
3.

## 검증 방법

- [ ] 테스트 실행
- [ ] 수동 검증
- [ ] API 확인
- [ ] 화면 확인

## 위험 요소

- 
- 

## 롤백 방법

문제가 발생했을 때 되돌리는 방법을 작성한다.

## 완료 기준

- [ ] 작업 범위가 완료되었다.
- [ ] 테스트 또는 수동 검증을 완료했다.
- [ ] 필요한 문서를 갱신했다.
- [ ] 남은 후속 조치가 있으면 기록했다.
```

---

## 21. 실행 계획 완료 처리

작업이 완료되면 다음을 수행한다.

1. 실행 계획의 완료 기준을 확인한다.
2. 검증 결과를 실행 계획에 기록한다.
3. 남은 문제가 있으면 `docs/exec-plans/tech-debt-tracker.md`에 기록한다.
4. 실행 계획 문서를 `active`에서 `completed`로 이동한다.
5. PR 설명에 실행 계획 문서 경로를 남긴다.

원칙:

* 완료된 실행 계획은 삭제하지 않는다.
* 실행 계획과 실제 구현이 달라졌다면 실행 계획을 갱신한다.
* 작업 중 새로 발견한 후속 조치는 이슈 또는 기술부채로 분리한다.

---

## 22. 작업 중 오류가 발생한 경우

작업 중 오류가 발생하면 해당 worktree 안에서 수정한다.

원칙:

* 오류 수정도 같은 작업 브랜치에서 진행한다.
* 수정 후 테스트 또는 수동 검증을 다시 수행한다.
* 수정 내용을 커밋한다.
* 이미 PR이 열려 있다면 같은 브랜치에 push하여 PR에 반영한다.
* 오류 수정 때문에 작업 범위가 커지면 이슈 또는 PR 설명에 남긴다.
* 기존 작업 디렉터리나 다른 브랜치에서 임의로 수정하지 않는다.

예시:

```bash
# worktree 안에서 수정 후
git status
git add .
git commit -m "fix: handle refresh token validation error"
git push
```

---

## 18. Pull Request 작성 기준

작업 완료 후 Pull Request를 작성한다.

PR 대상 브랜치:

```txt
feature/* → develop
fix/* → develop
refactor/* → develop
docs/* → develop
test/* → develop
```

배포 또는 안정 버전 반영 시:

```txt
develop → main
```

---

## 19. Pull Request 템플릿

```md
# 작업 내용

## 관련 이슈

- Closes #

## 변경 사항

- 
- 
- 

## 검증 방법

- [ ] 테스트 실행
- [ ] 수동 검증
- [ ] 관련 화면 확인
- [ ] API 응답 확인

## 영향 범위

- Backend
- Frontend
- Database
- Security
- Reliability
- Documentation

## 후속 조치

- 
- 

## 체크리스트

- [ ] 이슈와 연결되어 있다.
- [ ] 작업 범위가 명확하다.
- [ ] 기존 기능을 깨지 않는다.
- [ ] 테스트 또는 수동 검증을 수행했다.
- [ ] 필요한 문서를 갱신했다.
- [ ] 보안 또는 DB 영향이 있으면 관련 문서를 확인했다.
```

---

## 20. PR 후속 조치 방식

PR 작성 후 리뷰, 오류, 추가 수정 사항이 있으면 기존 작업 브랜치에서 바로 수정한다.

원칙:

* 새 브랜치를 만들지 않는다.
* 기존 PR 브랜치에서 수정한다.
* 수정 후 같은 PR에 반영한다.
* 리뷰어가 다시 확인할 수 있도록 변경 내용을 명확히 남긴다.
* 후속 조치가 커지면 PR 설명의 `후속 조치` 항목을 갱신한다.

예시:

```bash
cd ../project-login-api

# 수정 후
git add .
git commit -m "fix: apply review feedback for login api"
git push
```

이미 열린 PR은 자동으로 업데이트된다.

---

## 21. 머지 기준

PR은 다음 조건을 만족한 뒤 머지한다.

* 이슈와 연결되어 있다.
* 작업 범위가 명확하다.
* 주요 기능이 정상 동작한다.
* 기존 기능을 깨지 않는다.
* 테스트 또는 수동 검증을 완료했다.
* 보안, DB, 권한 변경이 있으면 관련 문서를 확인했다.
* 필요한 문서가 갱신되었다.
* 리뷰 후속 조치가 반영되었다.

---

## 22. 머지 후 작업

PR 머지 후 다음을 수행한다.

1. 로컬 `develop` 브랜치를 최신화한다.
2. 작업 브랜치를 삭제한다.
3. worktree를 제거한다.
4. 완료된 이슈를 닫는다.
5. 남은 후속 작업이 있으면 새 이슈로 분리한다.

예시:

```bash
# 기본 작업 디렉터리로 이동
cd ../project

# develop 최신화
git checkout develop
git pull origin develop

# worktree 제거
git worktree remove ../project-login-api

# 로컬 브랜치 삭제
git branch -d feature/123-login-api
```

원격 브랜치 삭제는 PR 머지 후 GitHub 또는 명령어로 처리한다.

```bash
git push origin --delete feature/123-login-api
```

---

## 23. 커밋 메시지 규칙

커밋 메시지는 변경 목적이 드러나게 작성한다.

형식:

```txt
type: message
```

예시:

```txt
feat: add login api
fix: handle refresh token expiration
refactor: simplify auth service
docs: update security guide
test: add auth service tests
chore: update build config
```

커밋 타입:

| 타입         | 의미            |
| ---------- | ------------- |
| `feat`     | 기능 추가         |
| `fix`      | 버그 수정         |
| `refactor` | 리팩토링          |
| `docs`     | 문서 수정         |
| `test`     | 테스트 추가 또는 수정  |
| `chore`    | 설정, 빌드, 기타 작업 |
| `style`    | 포맷팅, 스타일 수정   |
| `perf`     | 성능 개선         |

---

## 24. 작업 시작 체크리스트

작업 시작 전 확인한다.

* [ ] 이슈를 작성했는가?
* [ ] 작업 범위가 명확한가?
* [ ] `develop`을 최신화했는가?
* [ ] 기능 브랜치를 생성했는가?
* [ ] 필요한 경우 worktree를 생성했는가?
* [ ] 관련 문서를 확인했는가?
* [ ] 보안, DB, 권한 영향이 있는가?
* [ ] 큰 작업이면 실행 계획이 필요한가?

---

## 25. 작업 완료 체크리스트

작업 완료 후 확인한다.

* [ ] 변경 내용이 이슈 범위를 벗어나지 않는가?
* [ ] 테스트 또는 수동 검증을 수행했는가?
* [ ] 오류가 있으면 같은 브랜치에서 수정했는가?
* [ ] 필요한 문서를 갱신했는가?
* [ ] PR을 작성했는가?
* [ ] PR 설명에 검증 방법을 적었는가?
* [ ] 후속 조치가 있으면 같은 브랜치에서 반영했는가?
* [ ] 머지 후 worktree를 정리했는가?

---

## 26. 에이전트 작업 지침

AI 에이전트가 작업할 때는 다음을 따른다.

* 작업 전 `AGENTS.md`를 확인한다.
* 개발 흐름은 이 문서를 따른다.
* 직접 `main` 또는 `develop`에서 작업하지 않는다.
* 작업 전 이슈 또는 작업 목적을 확인한다.
* 큰 작업은 별도 worktree에서 진행한다.
* 작업 브랜치는 `develop`에서 생성한다.
* 변경 범위를 임의로 확장하지 않는다.
* 오류가 발생하면 해당 worktree와 브랜치 안에서 수정한다.
* PR 후속 조치는 같은 브랜치에서 반영한다.
* 보안, DB, 아키텍처 변경은 관련 문서를 함께 확인한다.
* 작업 완료 후 변경 파일, 검증 방법, 남은 문제를 요약한다.

---

## 27. 핵심 원칙 요약

* 모든 작업은 이슈에서 시작한다.
* `main`과 `develop`에서는 직접 작업하지 않는다.
* 기능 브랜치는 `develop`에서 만든다.
* 큰 작업은 worktree에서 분리해서 진행한다.
* 큰 작업은 `docs/exec-plans/active/`에 실행 계획을 작성한 뒤 진행한다.
* 완료된 실행 계획은 `docs/exec-plans/completed/`로 이동한다.
* PR 후속 조치는 같은 브랜치에서 바로 수정한다.
* 머지 후 worktree와 브랜치를 정리한다.
* 작업 범위와 검증 방법을 항상 남긴다.

