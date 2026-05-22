# Conventions

이 문서는 Claude와 Codex에서 함께 따를 네이밍 및 클린코드 규칙을 정의합니다.

## 네이밍

### 클래스 이름

- Web controller: `{Domain}Controller`
- REST controller: `{Domain}ApiController`
- Use case: `{Domain}UseCase`
- Port: `{Domain}Port`
- JPA port: `{Domain}JpaPort`
- MyBatis adapter: `{Domain}MyBatisAdapter`
- JPA adapter: `{Domain}JpaAdapter`
- JPA entity: `{Domain}Entity`
- Request DTO: `Create{Domain}Request`, `Modify{Domain}Request`
- Response DTO: `{Domain}Response`
- Command: `Create{Domain}`, `Modify{Domain}`, `Delete{Domain}`
- Query: `Get{Domain}By{Condition}`, `Find{Domain}By{Condition}`, `Get{Domain}List`

### 메서드 이름

- 반드시 존재해야 하는 조회: `get...`
- 없을 수도 있는 조회: `find...`
- 생성/수정/삭제: `create`, `modify`, `delete`
- Boolean 반환: `is...`, `has...`, `can...`
- Domain 행위: `approve`, `cancel`, `activate`

## Query 의미 규칙

- `Get*`은 대상이 반드시 존재해야 함을 의미합니다. 없으면 예외를 던집니다.
- `Find*`는 없을 수 있음을 의미합니다. 계약에 맞게 `null`, `Optional`, 빈 컬렉션 중 하나를 반환합니다.
- 없을 때 조용히 빈 결과를 반환하는 메서드에 `Get*` 이름을 붙이지 않습니다.

## DTO 규칙

- request DTO는 controller 또는 API 패키지 가까이에 둡니다.
- request DTO는 비즈니스 로직이 깊어지기 전에 command로 변환합니다.
- response DTO는 controller 경계 가까이에서 조립합니다.
- response DTO 생성은 `static from(domain)` 형태를 우선합니다.
- request DTO에는 가능하면 `toCommand()`를 제공합니다.

## Domain 규칙

- 가능하면 불변 객체로 구성합니다.
- 일반 흐름에서 setter 중심 상태 변경을 지양합니다.
- 파싱, 포맷팅, 전송용 변환 로직은 domain 모델 밖에 둡니다.
- domain 클래스에 `toLegacyBean()` 같은 전송용 변환 메서드를 추가하지 않습니다.
- 데이터 꺼내서 바깥에서 판단하는 방식보다, 객체가 스스로 판단하는 TDA 원칙을 우선합니다.
- 객체 생성은 Builder 패턴을 우선하고, 상태 변경은 의미 있는 행위 메서드로 표현합니다.

예시:

```java
// 지양
if (user.getAge() >= 19) {
    admit();
}

// 권장
if (user.isAdult()) {
    admit();
}
```

```java
// 지양
order.setStatus(OrderStatus.CANCELLED);

// 권장
order.cancel();
```

```java
// 지양
Coach coach = new Coach();
coach.setName("홍길동");
coach.setStatus(CoachStatus.ACTIVE);

// 권장
Coach coach = Coach.builder()
    .name("홍길동")
    .status(CoachStatus.ACTIVE)
    .build();
```

## 예외 처리

- use case는 domain 전용 또는 application 전용 예외를 던집니다.
- controller 또는 global exception handler가 이를 HTTP 응답이나 페이지 결과로 변환합니다.
- use case 내부에서 `ResponseStatusException` 사용을 지양합니다.
- `Result.fail()` 같은 DTO 실패 표현보다 예외 기반 흐름을 우선합니다.

## 일반 클린코드 규칙

- 매직 스트링과 매직 넘버를 피하고, 상수나 enum으로 추출합니다.
- 짧고 모호한 이름보다 의도가 드러나는 이름을 우선합니다.
- 반복되는 정책 로직은 별도 service, query, command로 분리합니다.
- helper 메서드는 해당 책임이 있는 계층 가까이에 둡니다. controller에 private helper가 과하게 많다면 다른 계층으로 옮길 후보입니다.
- 설정으로 빼야 할 값은 하드코딩하지 않습니다.
- `var`는 사용하지 않고 명시적 타입 선언을 우선합니다.
- 컬렉션 조회에서 `null`보다 `List.of()`, `Set.of()`, `Map.of()` 같은 빈 객체를 우선합니다.
- Optional은 `orElse(null)` 뒤 null 체크로 풀지 말고, `ifPresent`, `map`, `orElseThrow`를 우선합니다.
- `e.printStackTrace()` 대신 `log.error("message", e)` 형태를 사용합니다.

예시:

```java
// 지양
public List<Order> findByCustomerId(Long id) {
    return null;
}

// 권장
public List<Order> findByCustomerId(Long id) {
    return List.of();
}
```

```java
// 지양
Order order = optional.orElse(null);
if (order != null) {
    process(order);
}

// 권장
optional.ifPresent(this::process);
Order order = optional.orElseThrow(() ->
    new OrderException(OrderErrorCode.ORDER_NOT_FOUND));
```
