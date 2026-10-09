package com.example.fireview.domain.auth.service;

import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;

/**
 * 리프레시 토큰이 없거나 만료·폐기·재사용된 경우.
 *
 * <p>{@link CustomException} 과 같은 401 공통 포맷으로 나가지만, 응답에 쿠키 삭제 헤더를
 * 붙여야 해서 따로 구분한다({@code AuthController} 의 예외 핸들러가 받는다).
 */
public class InvalidRefreshTokenException extends CustomException {

    public InvalidRefreshTokenException() {
        super(ErrorCode.REFRESH_TOKEN_INVALID);
    }
}
