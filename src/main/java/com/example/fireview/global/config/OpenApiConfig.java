package com.example.fireview.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * OpenAPI 3 스펙 메타 정보.
 *
 * 엔드포인트와 스키마는 컨트롤러·DTO 에서 자동 추출되므로 여기서는 손대지 않는다.
 * 손으로 쓴 문서와 달리 코드가 바뀌면 스펙도 같이 바뀐다.
 *
 * <ul>
 *   <li>Swagger UI : /swagger-ui.html</li>
 *   <li>OpenAPI JSON : /v3/api-docs</li>
 * </ul>
 */
@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI fireViewOpenAPI(@Value("${app.openapi.server-url:}") String serverUrl) {

        Info info = new Info()
                .title("Re:View Service API")
                .version("v1")
                .description("""
                        리뷰 신뢰도 분석 서비스 Re:View 의 서비스 서버(Spring) API.

                        ## 인증
                        로그인 응답의 `accessToken` 을 우측 상단 **Authorize** 버튼에 넣으면
                        인증이 필요한 API 를 그대로 호출해볼 수 있다. (`Bearer ` 접두어는 자동으로 붙는다)

                        ## 공통 응답
                        모든 응답은 `{ success, message, data, errorCode }` 봉투로 감싸진다.
                        값이 `null` 인 필드는 응답에서 생략된다.

                        ## 참고
                        상품·리뷰·분석 조회는 Data 서버 이관 예정이다. 자세한 내용은 `docs/api-spec.md` 참고.
                        """);

        SecurityScheme bearer = new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("로그인 API 응답의 accessToken 값");

        OpenAPI openAPI = new OpenAPI()
                .info(info)
                .components(new Components().addSecuritySchemes(BEARER_SCHEME, bearer))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME));

        // 운영 도메인을 명시하면 Swagger UI 의 Try it out 이 그 주소로 요청한다.
        if (!serverUrl.isBlank()) {
            openAPI.servers(List.of(new Server().url(serverUrl).description("운영")));
        }
        return openAPI;
    }
}
