package com.example.fireview.domain.auth.service;

import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;

/**
 * 리프레시 토큰 저장소(운영은 Redis)에 닿지 못했다.
 *
 * <p>503 {@code AUTH_SESSION_UNAVAILABLE} 로 나간다. 401 로 내보내면 프론트가 로그아웃시키고
 * 쿠키까지 지워져, 저장소가 돌아와도 살아 있던 세션을 쓸 수 없게 된다. 503 이면 쿠키는 그대로
 * 남고 나중에 다시 시도할 수 있다.
 */
public class RefreshTokenStoreUnavailableException extends CustomException {

    public RefreshTokenStoreUnavailableException(Throwable cause) {
        super(ErrorCode.AUTH_SESSION_UNAVAILABLE);
        initCause(cause);
    }
}
