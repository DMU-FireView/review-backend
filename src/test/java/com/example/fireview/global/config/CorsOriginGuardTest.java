package com.example.fireview.global.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** allowCredentials=true 와 와일드카드 출처를 함께 쓰지 못하게 막는지 본다 */
class CorsOriginGuardTest {

    @Test
    void 와일드카드_출처는_기동을_막는다() {
        assertThatThrownBy(() -> SecurityConfig.requireExplicitOrigins(List.of("https://re-view.kr", "*")))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> SecurityConfig.requireExplicitOrigins(List.of(" https://* ")))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void 호스트가_정해진_패턴은_허용한다() {
        List<String> origins = List.of("http://localhost:*", "http://127.0.0.1:*",
                "https://re-view.kr", "https://www.re-view.kr");

        assertThat(SecurityConfig.requireExplicitOrigins(origins)).isEqualTo(origins);
    }
}
