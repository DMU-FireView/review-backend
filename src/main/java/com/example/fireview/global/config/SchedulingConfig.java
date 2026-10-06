package com.example.fireview.global.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 정기 작업을 켠다. 지금은 홈 자동 채우기({@code HomeCatalogRefresher})뿐이라 그 설정을 따른다.
 *
 * <p>항상 켜 두지 않는 이유: 테스트·로컬에서 외부 서버 호출이 저절로 나가면 안 된다.
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.home-refresh.enabled", havingValue = "true")
public class SchedulingConfig {
}
