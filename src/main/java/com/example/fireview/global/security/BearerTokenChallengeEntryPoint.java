package com.example.fireview.global.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.security.oauth2.server.resource.BearerTokenErrorCodes;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 만료·위조·형식 오류 토큰(oauth2ResourceServer 경로)의 401 진입점.
 *
 * <p>RFC 6750 §3 의 {@code WWW-Authenticate: Bearer error="invalid_token", ...} 헤더는
 * {@link BearerTokenAuthenticationEntryPoint} 로 세팅하고, 본문은
 * {@link CustomAuthenticationEntryPoint} 로 써서 토큰 없음과 같은 공통 JSON 을 유지한다.
 *
 * <p>JwtDecoder 의 예외 메시지(만료 시각, 서명 검증 실패 사유 등)가 error_description 에
 * 그대로 실리지 않도록 설명은 고정 문구로 바꾼다. error 코드와 error_uri 는 그대로 둔다.
 */
@Component
@RequiredArgsConstructor
public class BearerTokenChallengeEntryPoint implements AuthenticationEntryPoint {

    static final String INVALID_TOKEN_DESCRIPTION = "The access token is invalid or expired";
    static final String DEFAULT_DESCRIPTION = "The request could not be authenticated";

    private final BearerTokenAuthenticationEntryPoint challengeEntryPoint = new BearerTokenAuthenticationEntryPoint();
    private final CustomAuthenticationEntryPoint bodyEntryPoint;

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        challengeEntryPoint.commence(request, response, withFixedDescription(authException));
        // 상태는 BearerTokenError 에 따라 400 이 될 수 있으므로 여기서 다시 401 로 맞춘다
        bodyEntryPoint.commence(request, response, authException);
    }

    private AuthenticationException withFixedDescription(AuthenticationException authException) {
        if (!(authException instanceof OAuth2AuthenticationException oauth2Exception)) {
            return authException;
        }
        OAuth2Error error = oauth2Exception.getError();
        String description = BearerTokenErrorCodes.INVALID_TOKEN.equals(error.getErrorCode())
                ? INVALID_TOKEN_DESCRIPTION
                : DEFAULT_DESCRIPTION;
        HttpStatus status = error instanceof BearerTokenError bearerError
                ? bearerError.getHttpStatus()
                : HttpStatus.UNAUTHORIZED;
        return new OAuth2AuthenticationException(
                new BearerTokenError(error.getErrorCode(), status, description, error.getUri()));
    }
}
