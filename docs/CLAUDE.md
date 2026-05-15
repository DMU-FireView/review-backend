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
