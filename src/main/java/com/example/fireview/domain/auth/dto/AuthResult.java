package com.example.fireview.domain.auth.dto;

/**
 * 로그인·회원가입·refresh 결과. 본문({@link LoginResponse})과 리프레시 토큰 원문을 함께 넘긴다.
 * 토큰을 쿠키로 보낼지 본문으로 보낼지는 컨트롤러가 정한다.
 *
 * <p>{@code refreshToken} 은 리프레시 저장소 장애로 로그인만 성공시킨 경우 null 이다.
 */
public record AuthResult(LoginResponse response, String refreshToken) {

    public static AuthResult withoutRefreshToken(LoginResponse response) {
        return new AuthResult(response, null);
    }

    public boolean hasRefreshToken() {
        return refreshToken != null;
    }
}
