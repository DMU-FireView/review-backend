package com.example.fireview.domain.auth.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

/**
 * OAuth2 authorization request 저장/조회 로깅 래퍼
 *
 * authorization_request_not_found 원인 추적용:
 * - saveAuthorizationRequest: 어느 세션에 state를 저장했는지 로그
 * - removeAuthorizationRequest: 어느 세션에서 state를 찾으려 했는지 로그
 *
 * 원인 파악 후 이 클래스 제거 가능.
 */
@Slf4j
@Component
public class LoggingOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private final HttpSessionOAuth2AuthorizationRequestRepository delegate =
            new HttpSessionOAuth2AuthorizationRequestRepository();

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        OAuth2AuthorizationRequest result = delegate.loadAuthorizationRequest(request);
        HttpSession session = request.getSession(false);
        log.info("[OAuth2Repo] load - sessionId={}, found={}",
                session != null ? session.getId() : "(세션없음)",
                result != null);
        return result;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        delegate.saveAuthorizationRequest(authorizationRequest, request, response);
        HttpSession session = request.getSession(false);
        log.info("[OAuth2Repo] save - sessionId={}, state={}, saved={}",
                session != null ? session.getId() : "(세션없음)",
                authorizationRequest != null ? authorizationRequest.getState() : "null",
                authorizationRequest != null);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                  HttpServletResponse response) {
        HttpSession session = request.getSession(false);
        String sessionId = session != null ? session.getId() : "(세션없음)";
        String stateParam = request.getParameter("state");

        OAuth2AuthorizationRequest result = delegate.removeAuthorizationRequest(request, response);

        log.info("[OAuth2Repo] remove - sessionId={}, stateParam={}, found={}",
                sessionId, stateParam, result != null);

        if (result == null && session != null) {
            // 세션에 어떤 속성이 있는지 추가 로그
            java.util.Enumeration<String> attrs = session.getAttributeNames();
            java.util.List<String> attrList = new java.util.ArrayList<>();
            while (attrs.hasMoreElements()) attrList.add(attrs.nextElement());
            log.warn("[OAuth2Repo] state 없음 - 세션 속성 목록: {}", attrList);
        }

        return result;
    }
}
