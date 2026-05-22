# Architecture

이 문서는 백엔드 구조와 의존성 방향에 대한 공통 기준입니다.

## 목표 흐름

권장 의존성 흐름:

`Controller -> UseCase -> Query/Command -> Port -> Adapter -> Mapper/Repository -> DB 또는 외부 시스템`

현재 프로젝트에는 위 흐름을 완전히 따르지 않는 레거시 코드가 남아 있습니다. 새 작업은 이 구조에서 더 멀어지는 것이 아니라, 이 구조에 더 가까워지도록 진행합니다.

## 계층별 책임

### Controller

- HTTP 요청을 받고 전송 계층 수준의 입력을 검증합니다.
- request DTO를 command 또는 단순 인자로 변환합니다.
- use case를 호출합니다.
- domain 결과를 response DTO 또는 view model로 변환합니다.

비즈니스 계산, 집계 로직, 시간 계산, URL 조립 규칙, persistence 호출을 여기 넣지 않습니다.

아래는 Controller에 두지 않습니다.

- 비즈니스 로직
- 집계 및 계산 로직
- `private static` 메서드 정의
- `private` helper 메서드에 과도하게 쌓인 업무 규칙
- `XXControllerHelper` 같은 보조 클래스 분산

### UseCase

- 비즈니스 흐름을 조율합니다.
- domain 객체, query, command, port를 조합합니다.
- domain 모델 또는 use case 수준 결과를 반환합니다.

기본 원칙은 UseCase가 Query/Command를 조합하는 것이며, Port를 직접 주입하는 방식은 지양이 아니라 금지 기준으로 봅니다.

아래 의존성은 직접 두지 않는 것을 원칙으로 합니다.

- API request/response DTO
- `HttpServletRequest`, `HttpServletResponse`, servlet session
- `ResponseStatusException` 같은 웹 전용 예외
- query/command 계층이 있어야 하는 상황에서의 MyBatis mapper, Spring Data repository 직접 호출
- Port 직접 주입

### Domain

- domain 상태와 행위를 가집니다.
- 불변 조건을 지킵니다.
- 비즈니스 의미를 메서드와 타입으로 표현합니다.

아래에는 의존하지 않습니다.

- controller 또는 application DTO
- JPA entity 클래스
- servlet 또는 framework 전송 타입
- 화면 표시용 포맷 규칙

아래 표현용 필드는 Domain에 두지 않습니다.

- `displayName`, `formatted*`, `*Label`
- `customerName`, `storeName` 같은 타 도메인 조합 필드
- `isEditable`, `canDelete` 같은 UI 제어용 필드

이런 값은 response DTO 또는 view model에서 조합합니다.

### Query 와 Command

- `Query`는 use case가 필요한 조회를 담당합니다.
- `Command`는 use case가 필요한 상태 변경을 담당합니다.
- orchestration 계층과 persistence/외부 어댑터 계층 사이의 안정적인 경계 역할을 합니다.

비즈니스 로직은 Query/Command에 넣지 않고, 데이터 접근과 상태 변경 수행에 집중합니다.

## 의존성 방향

권장:

- controller -> usecase
- usecase -> query/command
- query/command -> port
- adapter -> mapper/repository
- entity <-> domain 변환은 persistence 경계에서만 수행

지양:

- controller -> mapper/repository
- usecase -> controller DTO
- domain -> DTO/entity/framework 타입
- usecase -> servlet/web 예외

## Persistence 전략

아래 경우에는 JPA를 사용합니다.

- 명령이 단순한 경우
- 조회 형태가 고정되어 있는 경우
- entity 생명주기 관리의 이점이 있는 경우

아래 경우에는 MyBatis를 사용합니다.

- 조회 조건이 동적인 경우
- join이 복잡한 경우
- 집계성 또는 리포트성 조회가 필요한 경우
- entity 생명주기보다 SQL 제어가 더 중요한 경우

JPA repository 선택 기준:

- 조건이 단순하고 이름이 자연스러우면 메서드 네이밍 기반 조회를 사용합니다.
- 조건이 많아 메서드명이 과하게 길어지면 `@Query`를 사용합니다.
- `LIKE`, `BETWEEN`처럼 쿼리 표현이 중요한 경우 `@Query`를 우선합니다.

Adapter 규칙:

- Adapter는 조회 누락 상황에서 `orElse(null)` 또는 빈 컬렉션을 반환할 수 있습니다.
- Adapter에서 비즈니스 예외를 직접 던지지 않습니다. 예외 판단은 Query/Command 또는 UseCase에서 합니다.
- Entity에 `@Setter`를 사용하지 않습니다.

Entity 변환 규칙:

- 각 Entity는 `static from(domain)` 형태의 domain -> entity 변환 메서드를 가집니다.
- 각 Entity는 `toDomain()` 형태의 entity -> domain 변환 메서드를 가집니다.
- domain 과 entity 간 변환은 persistence 경계에서만 수행합니다.

## 권장 패키지 구조

```text
{domain}/
  api/
    dto/request/
    dto/response/
  application/usecase/
  domain/
    model/
    command/
    query/
    port/
    enums/
  exception/
  persistence/
    mybatis/
      adapter/
      mapper/
    jpa/
      adapter/
      entity/
      repository/
```

## 리팩터링 방향

레거시 코드를 건드릴 때는 아래 방향을 우선합니다.

- DTO 변환을 domain 모델 밖으로 이동
- mapper 또는 repository 직접 호출을 use case 밖으로 이동하고 query/command로 감싸기
- controller helper 로직을 use case 또는 별도 domain/application service로 이동
- 수정 전보다 의존성 방향이 더 깨끗해지도록 유지

## 트랜잭션 규칙

기본 원칙은 UseCase에서 트랜잭션 경계를 선언하는 것입니다.

- 일반 CRUD는 UseCase에 `@Transactional`
- 조회 전용은 UseCase에 `@Transactional(readOnly = true)`
- 배치나 대량 처리처럼 독립 트랜잭션이 필요하면 persistence 계층에서 `REQUIRES_NEW`를 사용할 수 있습니다.
- 외부 API 호출과 보상 처리가 섞이는 경우에는 UseCase에서 트랜잭션 경계를 명시적으로 관리합니다.
