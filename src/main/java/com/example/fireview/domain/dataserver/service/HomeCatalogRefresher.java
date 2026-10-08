package com.example.fireview.domain.dataserver.service;

import com.example.fireview.global.config.ExecutorConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 인기 키워드를 주기적으로 검색해 홈 목록을 채운다.
 *
 * <p>등록 상품과 분석은 별도 카탈로그 동기화로 가져오고 이 기능은 새 상품을 발굴한다. 사용자 검색만 기다리면 검색이 없는 동안 홈이 낡고(가격·이미지 갱신도 멈춘다),
 * 마지막 검색 몇 개가 홈을 다 차지한다. 여러 분야 키워드를 몰당 몇 건씩 미리 검색해 둔다.
 *
 * <p>Data 서버 검색은 새 상품을 등록하고 큐 상한 안에서 수집을 예약한다.
 * 그래도 키워드 사이에 간격을 둬 한꺼번에 몰아 보내지 않는다.
 *
 * <p>운영 프로필에서만 켠다({@code app.home-refresh.enabled}). 테스트·로컬에서
 * 외부 호출이 저절로 나가면 안 된다.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.home-refresh.enabled", havingValue = "true")
public class HomeCatalogRefresher {

    /**
     * 기본 키워드. 여러 분야를 고루 — 홈은 대분류별로 번갈아 보여주므로 분야가 겹치지 않게 고른다.
     *
     * <p>설정 파일이 아니라 코드에 둔다. Spring Boot 는 {@code .properties} 를 ISO-8859-1 로
     * 읽어서 한글 기본값이 깨진다(실제로 "선크림"이 깨져 컬리가 원두를 돌려줬다).
     * 바꾸려면 {@code HOME_REFRESH_KEYWORDS} 환경변수로 넣는다.
     */
    static final List<String> DEFAULT_KEYWORDS = List.of(
            "선크림", "마스크팩", "샴푸", "립스틱", "이어폰", "텀블러", "라면",
            "커피", "과자", "영양제", "운동화", "티셔츠", "세제", "이불",
            "토너", "세럼", "클렌징폼", "청바지", "후드티", "백팩",
            "그릭요거트", "밀키트", "샐러드", "닭가슴살", "보조배터리", "물티슈");

    private final DataProductCatalogService catalogService;
    private final Executor executor;
    private final List<String> keywords;
    private final int limitPerPlatform;
    private final long delayMs;
    private final boolean runOnStartup;

    /** 배포 직후 실행과 정기 실행이 겹치지 않게 한다 */
    private final AtomicBoolean running = new AtomicBoolean(false);

    public HomeCatalogRefresher(
            DataProductCatalogService catalogService,
            @Qualifier(ExecutorConfig.DATA_SERVER_EXECUTOR) Executor executor,
            @Value("${app.home-refresh.keywords:}") String keywords,
            @Value("${app.home-refresh.limit-per-platform:3}") int limitPerPlatform,
            @Value("${app.home-refresh.delay-ms:1000}") long delayMs,
            @Value("${app.home-refresh.run-on-startup:true}") boolean runOnStartup) {
        this.catalogService = catalogService;
        this.executor = executor;
        List<String> configured = keywords == null ? List.of() : Arrays.stream(keywords.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).toList();
        this.keywords = configured.isEmpty() ? DEFAULT_KEYWORDS : configured;
        this.limitPerPlatform = limitPerPlatform;
        this.delayMs = delayMs;
        this.runOnStartup = runOnStartup;
        // 키워드를 그대로 찍는다. 깨져 들어오면 여기서 바로 보인다
        log.info("[HomeRefresh] 키워드 {} / 몰당 {}건", this.keywords, limitPerPlatform);
    }

    /** 배포 직후 한 번. 요청 스레드를 붙잡지 않게 뒤에서 돌린다 */
    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (runOnStartup) {
            executor.execute(this::refresh);
        }
    }

    @Scheduled(cron = "${app.home-refresh.cron:0 0 */6 * * *}", zone = "Asia/Seoul")
    public void scheduled() {
        refresh();
    }

    /**
     * 키워드를 차례로 검색한다. 한 키워드가 실패해도 나머지는 계속한다.
     *
     * @return 이번에 받아온 상품 수. 이미 돌고 있으면 -1
     */
    public int refresh() {
        if (!catalogService.isEnabled()) {
            log.debug("[HomeRefresh] Data 서버 검색을 쓸 수 없어 건너뜀");
            return 0;
        }
        if (!running.compareAndSet(false, true)) {
            log.info("[HomeRefresh] 이전 실행이 아직 돌고 있어 건너뜀");
            return -1;
        }
        long started = System.currentTimeMillis();
        int total = 0;
        int failed = 0;
        try {
            for (int i = 0; i < keywords.size(); i++) {
                if (i > 0 && !pause()) break;
                String keyword = keywords.get(i);
                try {
                    total += catalogService.search(keyword, limitPerPlatform).size();
                } catch (RuntimeException e) {
                    failed++;
                    log.warn("[HomeRefresh] 검색 실패 - keyword={}: {}", keyword, e.getMessage());
                }
            }
            log.info("[HomeRefresh] 완료 - 키워드 {}개 / 상품 {}건 / 실패 {} / {}ms",
                    keywords.size(), total, failed, System.currentTimeMillis() - started);
            return total;
        } finally {
            running.set(false);
        }
    }

    /** @return 종료 중이라 멈춰야 하면 false */
    private boolean pause() {
        if (delayMs <= 0) return true;
        try {
            Thread.sleep(delayMs);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}
