# CLAUDE.md

Claude 계열 에이전트를 위한 저장소 루트 진입점입니다.

자동 로딩할 핵심 문서:
- @docs/conventions.md
- @docs/architecture.md
- @docs/orchestration.md

이 3개 문서는 세션 시작 전에 이미 알고 있어야 하는 규칙입니다.
공통 규칙의 source of truth는 `docs/`이고, 루트 파일은 짧은 진입점으로만 유지합니다.

## 프로젝트 메모

- 스택: Spring Boot 3, Java 17, Gradle, Thymeleaf, MyBatis, JPA, Redis, Spring Security, WebSocket, gRPC
- 공통 문서는 `docs/` 아래에 있습니다.
- Claude 전용 설정과 스킬은 `.claude/` 아래에 있습니다.
- `docs/MEMORY.md`는 프로젝트 작업 기록 파일로 유지합니다.
- 이 저장소에서는 Claude가 주로 코드 작성과 수정 작업을 맡고, Codex는 기본적으로 리뷰 용도로 사용합니다.

## 추가 문서 읽기 기준

아래 문서는 필요할 때만 읽습니다.

- `docs/frontend.md`: Thymeleaf, HTML, JS, CSS 수정 시
- `docs/git.md`: 브랜치, 커밋, push 판단 시
- `docs/runbook.md`: 로컬 실행, 프로필, 명령어 확인 시
- `docs/testing.md`: 테스트 작성, 실행, 검증 범위 판단 시
- `docs/review-flow.md`: Claude -> Codex 리뷰 플로우 템플릿이 필요할 때
- `docs/troubleshooting.md`: 반복되는 실행 실패, 환경 이슈, 테스트 실패 추적 시
- `docs/MEMORY.md`, `docs/daily/*`: 과거 작업 이력 확인 시

## GitHub 운영 방식

모든 구현·수정 작업은 아래 순서를 반드시 따릅니다.

1. **이슈 생성**: 구현하거나 수정할 내용을 먼저 GitHub 이슈로 등록합니다. (`gh issue create`)
2. **브랜치 분기**: `main`에서 작업용 브랜치를 하나 만듭니다. (`feat/...`, `fix/...`, `docs/...` 등 목적 기반 이름)
3. **커밋 누적**: 해당 브랜치에 작업 단위로 커밋을 쌓습니다. `main`에 직접 커밋하지 않습니다.
4. **PR 생성**: 작업이 완료되면 해당 브랜치를 `main`으로 향하는 PR로 올립니다. (`gh pr create`)

원칙:

- `main` 직접 커밋·푸시 금지. 항상 브랜치 → PR → 머지 플로우를 거칩니다.
- 하나의 이슈는 하나의 브랜치, 하나의 PR로 대응하는 것을 기본으로 합니다.

## 커밋 · PR 작성 규칙 (AI 표기 금지)

GitHub에 올라가는 **모든 산출물**(커밋, 이슈, PR, 브랜치명, 코멘트 등)에 AI(Claude) 표기와 협력 표시를 **일체 하지 않습니다**.

- 커밋 메시지에 `Co-Authored-By:`, `Generated with`, `Claude`, `Anthropic` 등 AI 협력 표기를 **절대 넣지 않습니다**.
- PR 제목·본문, 이슈 제목·본문, PR/이슈 코멘트에도 Claude 또는 AI 툴 관련 언급을 넣지 않습니다.
- 브랜치명에 `claude/` 접두어를 사용하지 않습니다.
- 모든 작업은 사용자 본인이 단독으로 진행한 것으로 표기합니다.

## 작업 규칙

- 같은 규칙 문서를 여러 파일에 중복 작성하지 않습니다.
- 공통 코딩 규칙과 아키텍처 규칙은 `docs/`에서 관리합니다.
- Claude 전용 자산만 `.claude/`에 둡니다.
- 공통 규칙을 바꾸려면 `docs/`를 먼저 수정합니다.

## 작업 완료 기준

- 기능 요구사항이 충족되어야 합니다.
- 관련 테스트가 존재하면 모두 통과해야 합니다.
- 신규 기능이나 회귀 위험이 큰 변경은 필요한 경우 테스트를 추가합니다.
- 변경된 내용, 검증 결과, 남은 리스크를 요약해서 정리합니다.

## 리뷰 요청 포맷

Codex에게 리뷰를 요청할 때는 가능하면 아래 정보를 함께 제공합니다.

- 변경 목적
- 변경 파일 목록
- 주요 변경 내용 요약
- `git diff`
- diff가 크면 핵심 파일 diff와 검증 결과

리뷰 요청의 기본 원칙:

- 요약보다 diff를 우선 전달합니다.
- 요약은 참고용이고 실제 변경 코드를 우선 분석하도록 요청합니다.
- 가능하면 Claude 출력이 그대로 Codex 입력이 되도록 정리합니다.

## 검증

중요한 변경을 마치기 전에 아래를 실행합니다.

- `./gradlew compileJava`
- `./gradlew test`

Windows PowerShell에서는 아래를 사용합니다.

- `.\gradlew compileJava`
- `.\gradlew test`

## 과거 규칙 참고

기존 `.claude/` 아래 규칙 파일은 더 이상 source of truth가 아닙니다. 공통 기준 문서는 `docs/`입니다.
