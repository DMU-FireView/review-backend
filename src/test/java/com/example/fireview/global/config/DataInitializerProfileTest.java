package com.example.fireview.global.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 시드 데이터가 운영에서 돌지 않는지 고정한다.
 *
 * <p>실제로 터졌던 버그다. {@code @Profile({"!prod", "!test"})} 는 배열이라 OR 로 묶인다.
 * prod 로 띄우면 {@code !prod}=false, {@code !test}=true → OR 결과 true → 시드가 돌았다.
 * 그 결과 운영 DB 에 더미 상품 33건과, 비밀번호가 공개 저장소에 적힌 관리자 계정이 생겼다.
 *
 * <p>프로파일 표현식을 Spring 이 실제로 해석하는 방식 그대로 평가해서 확인한다.
 * 애너테이션 문자열만 비교하면 표기를 바꿀 때 의미까지 맞는지는 못 잡는다.
 */
class DataInitializerProfileTest {

    private static boolean activeWith(String... profiles) {
        Profile annotation = DataInitializer.class.getAnnotation(Profile.class);
        assertThat(annotation).as("@Profile 이 사라지면 운영에서 시드가 돈다").isNotNull();

        MockEnvironment env = new MockEnvironment();
        env.setProperty(StandardEnvironment.ACTIVE_PROFILES_PROPERTY_NAME, String.join(",", profiles));
        return env.matchesProfiles(annotation.value());
    }

    @Test
    void 운영에서는_시드가_돌지_않는다() {
        assertThat(activeWith("prod")).isFalse();
    }

    @Test
    void 테스트에서도_돌지_않는다() {
        assertThat(activeWith("test")).isFalse();
    }

    @Test
    void 운영과_테스트가_함께_켜져도_돌지_않는다() {
        assertThat(activeWith("prod", "test")).isFalse();
    }

    @Test
    void 로컬_개발에서는_돈다() {
        assertThat(activeWith("local")).isTrue();
        assertThat(activeWith("dev")).isTrue();
    }
}
