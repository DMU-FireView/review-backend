package com.example.fireview.global.config;

import com.example.fireview.domain.dashboard.entity.SearchKeyword;
import com.example.fireview.domain.dashboard.repository.SearchKeywordRepository;
import com.example.fireview.domain.product.entity.Category;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.domain.review.entity.Review;
import com.example.fireview.domain.review.entity.TrustGrade;
import com.example.fireview.domain.review.repository.ReviewRepository;
import com.example.fireview.domain.review.service.RtiEngineService;
import com.example.fireview.domain.user.entity.OAuthProvider;
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 시연용 더미 데이터 초기화
 *
 * - 상품 30개 이상 (카테고리별 다양)
 * - 상품당 리뷰 20~50개
 * - 날짜 분포: 최근 30일 (추이 그래프 렌더링 가능)
 * - RTI 분포: safe 60% / warn 30% / danger 10%
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ReviewRepository reviewRepository;
    private final SearchKeywordRepository keywordRepository;
    private final PasswordEncoder passwordEncoder;
    private final RtiEngineService rtiEngine;

    private static final Random RANDOM = new Random(42); // 재현 가능한 랜덤

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) return;
        log.info("샘플 데이터 초기화 시작...");

        createUsers();
        List<Product> products = createProducts();
        createReviews(products);
        createKeywords();

        log.info("샘플 데이터 초기화 완료. 상품={}, 리뷰={}", productRepository.count(), reviewRepository.count());
    }

    private void createUsers() {
        userRepository.save(User.builder()
                .email("admin@fireview.com")
                .password(passwordEncoder.encode("password1!"))
                .nickname("관리자")
                .role(Role.ADMIN)
                .provider(OAuthProvider.LOCAL)
                .onboardingCompleted(true)
                .build());

        userRepository.save(User.builder()
                .email("user@fireview.com")
                .password(passwordEncoder.encode("password1!"))
                .nickname("테스트유저")
                .role(Role.USER)
                .provider(OAuthProvider.LOCAL)
                .onboardingCompleted(false)
                .build());
    }

    private List<Product> createProducts() {
        List<Product> products = new ArrayList<>();

        // 전자기기 (7개)
        products.add(product("삼성 갤럭시 S25 Ultra", "https://via.placeholder.com/300?text=Galaxy+S25", 1499000L, Category.ELECTRONICS, 82.5, 4.5));
        products.add(product("LG 그램 17인치 노트북", "https://via.placeholder.com/300?text=LG+Gram", 1890000L, Category.ELECTRONICS, 88.0, 4.7));
        products.add(product("애플 에어팟 프로 2세대", "https://via.placeholder.com/300?text=AirPods+Pro", 359000L, Category.ELECTRONICS, 91.0, 4.8));
        products.add(product("소니 WH-1000XM5 헤드폰", "https://via.placeholder.com/300?text=Sony+WH", 429000L, Category.ELECTRONICS, 87.5, 4.6));
        products.add(product("애플 아이패드 프로 11인치", "https://via.placeholder.com/300?text=iPad+Pro", 1299000L, Category.ELECTRONICS, 85.0, 4.5));
        products.add(product("삼성 갤럭시 워치7", "https://via.placeholder.com/300?text=Galaxy+Watch", 299000L, Category.ELECTRONICS, 43.0, 3.6));
        products.add(product("레노버 씽크패드 X1 카본", "https://via.placeholder.com/300?text=ThinkPad", 2190000L, Category.ELECTRONICS, 78.0, 4.3));

        // 패션 (5개)
        products.add(product("나이키 에어맥스 270", "https://via.placeholder.com/300?text=Nike+270", 179000L, Category.FASHION, 78.5, 4.3));
        products.add(product("뉴발란스 574 클래식", "https://via.placeholder.com/300?text=NB+574", 129000L, Category.FASHION, 85.0, 4.6));
        products.add(product("아디다스 울트라부스트 22", "https://via.placeholder.com/300?text=Ultraboost", 219000L, Category.FASHION, 55.0, 4.0));
        products.add(product("자라 오버핏 트렌치코트", "https://via.placeholder.com/300?text=Zara+Coat", 159000L, Category.FASHION, 38.0, 3.4));
        products.add(product("유니클로 울트라라이트 다운", "https://via.placeholder.com/300?text=Uniqlo+Down", 89000L, Category.FASHION, 81.0, 4.4));

        // 화장품 (5개)
        products.add(product("설화수 자음생 크림 60ml", "https://via.placeholder.com/300?text=Sulwhasoo", 185000L, Category.COSMETICS, 43.0, 3.8));
        products.add(product("아이오페 레티놀 엑스퍼트 0.1%", "https://via.placeholder.com/300?text=IOPE", 62000L, Category.COSMETICS, 55.0, 4.1));
        products.add(product("라네즈 워터슬리핑마스크 70ml", "https://via.placeholder.com/300?text=Laneige", 38000L, Category.COSMETICS, 83.0, 4.5));
        products.add(product("에스티로더 갈색병 50ml", "https://via.placeholder.com/300?text=EsteeLauder", 145000L, Category.COSMETICS, 29.0, 3.1));
        products.add(product("닥터지 브라이트닝 필링젤", "https://via.placeholder.com/300?text=DrG", 25000L, Category.COSMETICS, 77.0, 4.2));

        // 생활가전 (4개)
        products.add(product("다이슨 에어랩 멀티 스타일러", "https://via.placeholder.com/300?text=Dyson+Airwrap", 689000L, Category.HOME_APPLIANCE, 67.0, 4.2));
        products.add(product("LG 코드제로 A9S 무선청소기", "https://via.placeholder.com/300?text=LG+Cordless", 899000L, Category.HOME_APPLIANCE, 84.0, 4.5));
        products.add(product("삼성 비스포크 큐브 공기청정기", "https://via.placeholder.com/300?text=Bespoke+Air", 549000L, Category.HOME_APPLIANCE, 72.0, 4.1));
        products.add(product("필립스 에어프라이어 5.6L", "https://via.placeholder.com/300?text=Philips+AF", 159000L, Category.HOME_APPLIANCE, 88.5, 4.7));

        // 식품 (3개)
        products.add(product("동원 참치 85g 20개입", "https://via.placeholder.com/300?text=Dongwon+Tuna", 28000L, Category.FOOD, 90.0, 4.8));
        products.add(product("정관장 홍삼정 에브리타임", "https://via.placeholder.com/300?text=KGC+Everytime", 98000L, Category.FOOD, 52.0, 4.0));
        products.add(product("농심 신라면 40봉", "https://via.placeholder.com/300?text=Shin+Ramen", 18500L, Category.FOOD, 86.0, 4.6));

        // 스포츠 (3개)
        products.add(product("나이키 에어줌 페가수스 40", "https://via.placeholder.com/300?text=Pegasus+40", 149000L, Category.SPORTS, 80.0, 4.4));
        products.add(product("언더아머 UA 차지 어설트 4", "https://via.placeholder.com/300?text=UA+Charge", 119000L, Category.SPORTS, 62.0, 4.0));
        products.add(product("요넥스 나노플렉스 800 배드민턴 라켓", "https://via.placeholder.com/300?text=Yonex", 189000L, Category.SPORTS, 84.0, 4.5));

        // 도서 (2개)
        products.add(product("클린 코드 (로버트 C. 마틴)", "https://via.placeholder.com/300?text=Clean+Code", 33000L, Category.BOOKS, 94.0, 4.9));
        products.add(product("자바 ORM 표준 JPA 프로그래밍", "https://via.placeholder.com/300?text=JPA+Book", 45000L, Category.BOOKS, 92.0, 4.8));

        // 유아 (2개)
        products.add(product("하기스 신생아 기저귀 84매", "https://via.placeholder.com/300?text=Huggies", 32000L, Category.BABY, 88.0, 4.7));
        products.add(product("피셔프라이스 신생아 바운서", "https://via.placeholder.com/300?text=FisherPrice", 89000L, Category.BABY, 75.0, 4.2));

        // 반려동물 (2개)
        products.add(product("로얄캐닌 어덜트 고양이 사료 4kg", "https://via.placeholder.com/300?text=Royal+Canin", 58000L, Category.PET, 86.0, 4.6));
        products.add(product("강아지 자동 급수기 2.5L", "https://via.placeholder.com/300?text=Pet+Fountain", 35000L, Category.PET, 47.0, 3.7));

        return productRepository.saveAll(products);
    }

    private Product product(String name, String imageUrl, Long price, Category category, double avgRti, double avgRating) {
        return Product.builder()
                .name(name)
                .imageUrl(imageUrl)
                .price(price)
                .category(category)
                .avgRti(avgRti)
                .reviewCount(0)
                .avgRating(avgRating)
                .build();
    }

    /** 상품별 RTI 수준에 따른 리뷰 생성 */
    private void createReviews(List<Product> products) {
        // 30일치 추이 데이터 생성 가능하도록 날짜 분산
        for (Product product : products) {
            int reviewCount = 20 + RANDOM.nextInt(31); // 20~50개
            createReviewsForProduct(product, reviewCount);
        }
    }

    private void createReviewsForProduct(Product product, int count) {
        double avgRti = product.getAvgRti();

        // 상품 RTI에 따라 등급 분포 결정
        // safe 60% / warn 30% / danger 10% 목표
        int safeCount  = (int) Math.round(count * 0.60);
        int warnCount  = (int) Math.round(count * 0.30);
        int dangerCount = count - safeCount - warnCount;

        // 상품 avg RTI가 낮으면 danger 비중 높임
        if (avgRti < 50) {
            dangerCount = (int) Math.round(count * 0.35);
            warnCount   = (int) Math.round(count * 0.40);
            safeCount   = count - warnCount - dangerCount;
        } else if (avgRti < 70) {
            warnCount  = (int) Math.round(count * 0.45);
            dangerCount = (int) Math.round(count * 0.20);
            safeCount  = count - warnCount - dangerCount;
        }

        List<Review> batch = new ArrayList<>();

        // SAFE 리뷰 생성
        for (int i = 0; i < safeCount; i++) {
            batch.add(buildReview(product, pickSafeContent(), "trusted_" + i + "_p" + product.getId(),
                    4 + RANDOM.nextInt(2), true, daysBefore(25 + RANDOM.nextInt(6))));
        }
        // WARN 리뷰 생성
        for (int i = 0; i < warnCount; i++) {
            batch.add(buildReview(product, pickWarnContent(), "newbie_" + i + "_p" + product.getId(),
                    3 + RANDOM.nextInt(3), false, daysBefore(10 + RANDOM.nextInt(11))));
        }
        // DANGER 리뷰 생성
        for (int i = 0; i < dangerCount; i++) {
            batch.add(buildReview(product, pickDangerContent(), "fake_" + i + "_p" + product.getId(),
                    5, false, daysBefore(1 + RANDOM.nextInt(5))));
        }

        reviewRepository.saveAll(batch);

        // 리뷰 수 업데이트
        product.updateReviewCount(count);
        productRepository.save(product);
    }

    private Review buildReview(Product product, String content, String reviewerId,
                                int rating, boolean verified, LocalDateTime writtenAt) {
        RtiEngineService.RtiResult result = rtiEngine.calculate(content, reviewerId, writtenAt, verified);
        return Review.builder()
                .product(product)
                .reviewerNickname(reviewerId.split("_")[0])
                .reviewerId(reviewerId)
                .content(content)
                .rating(rating)
                .rtiScore(result.score())
                .trustGrade(result.grade())
                .reasons(result.reasons())
                .writtenAt(writtenAt)
                .isVerifiedPurchase(verified)
                .build();
    }

    private LocalDateTime daysBefore(int days) {
        int hourVariance = RANDOM.nextInt(18) + 6; // 6~23시
        return LocalDateTime.now().minusDays(days).withHour(hourVariance).withMinute(RANDOM.nextInt(60));
    }

    // === 리뷰 내용 풀 ===

    private static final String[] SAFE_CONTENTS = {
            "실제로 써보니 정말 만족스럽습니다. 배송도 빠르고 제품 품질이 기대 이상이에요. 화면이 선명하고 배터리도 하루 종일 쓸 수 있어서 좋습니다.",
            "두 달 정도 사용해봤는데 품질이 정말 좋네요. 처음 살 때 걱정했는데 완전히 만족합니다. 다음에도 이 브랜드로 구매할 것 같아요.",
            "가격 대비 성능이 훌륭합니다. 디자인도 마음에 들고 실용적으로 잘 쓰고 있어요. 주변 지인들에게도 추천했습니다.",
            "제품 받고 바로 써봤는데 생각보다 훨씬 좋아요. 특히 성능 면에서 기대를 뛰어넘었습니다. 포장도 꼼꼼하게 되어있었어요.",
            "오래 사용해보고 리뷰 남깁니다. 내구성이 좋고 사용감이 편합니다. 처음엔 가격이 부담됐는데 충분히 값어치를 합니다.",
            "색상이 사진과 동일하고 소재도 좋습니다. 세탁 후에도 형태가 유지되어서 만족합니다. 사이즈 표를 잘 참고해서 구매하세요.",
            "정품 인증도 되고 배송도 예상보다 빨랐어요. 제품 자체 품질은 흠잡을 곳이 없습니다. 이 가격에 이 품질은 정말 좋은 것 같아요.",
            "여러 브랜드 써봤는데 이 제품이 제일 마음에 들어요. 기능성도 뛰어나고 디자인도 세련됐습니다. 강력 추천합니다!",
            "친구 추천으로 구매했는데 역시 좋네요. 사용하면 할수록 만족도가 높아집니다. 다음에도 재구매할 예정입니다.",
            "처음 사용해봤는데 품질이 정말 좋습니다. 가격도 합리적이고 배송도 빨랐어요. 만족스러운 쇼핑이었습니다."
    };

    private static final String[] WARN_CONTENTS = {
            "나쁘지는 않은데 가격 대비 좀 아쉬운 면이 있어요. 기대했던 것보다는 조금 실망했습니다.",
            "제품 자체는 괜찮은데 포장이 좀 불량이었어요. 재구매 의사는 있지만 가격이 좀 내려갔으면 좋겠어요.",
            "배송이 늦었고 제품도 생각보다 작았어요. 그래도 기능은 잘 됩니다.",
            "색상이 사진과 조금 다르네요. 실제 사용감은 그럭저럭 괜찮습니다.",
            "다른 분들 후기보고 구매했는데 개인차가 있는 것 같아요. 저한테는 보통입니다.",
            "가성비를 생각하면 나쁘지 않아요. 다만 더 좋은 제품들도 있어서 추천하기는 애매합니다."
    };

    private static final String[] DANGER_CONTENTS = {
            "완전대박 인생템 강추강추!!! 무조건 사야됨 역대급이에요!! 진짜 최고최고!!",
            "강추 완전최고 강추 완전최고 재구매확정 강추강추강추!!",
            "짱 좋아요 최고에요 강추합니다 짱짱짱!!!",
            "돈값해요 완벽 실망없음 무조건구매 혜자템!! 강추강추!!",
            "이 제품 안 사면 후회해요!! 무조건 사세요!! 최고최고최고!!",
            "완전 강추!! 역대급 제품!! 이거 사면 인생이 바뀜!! 강추!!",
            "오늘산게 횡재 최고템 강추 무조건추천 짱짱!!!"
    };

    private String pickSafeContent() {
        return SAFE_CONTENTS[RANDOM.nextInt(SAFE_CONTENTS.length)];
    }

    private String pickWarnContent() {
        return WARN_CONTENTS[RANDOM.nextInt(WARN_CONTENTS.length)];
    }

    private String pickDangerContent() {
        return DANGER_CONTENTS[RANDOM.nextInt(DANGER_CONTENTS.length)];
    }

    private void createKeywords() {
        List<SearchKeyword> keywords = List.of(
                SearchKeyword.builder().keyword("갤럭시").searchCount(15200L).build(),
                SearchKeyword.builder().keyword("나이키").searchCount(12800L).build(),
                SearchKeyword.builder().keyword("에어팟").searchCount(11500L).build(),
                SearchKeyword.builder().keyword("다이슨").searchCount(9300L).build(),
                SearchKeyword.builder().keyword("설화수").searchCount(8700L).build(),
                SearchKeyword.builder().keyword("LG그램").searchCount(7100L).build(),
                SearchKeyword.builder().keyword("뉴발란스").searchCount(6500L).build(),
                SearchKeyword.builder().keyword("아이오페").searchCount(5800L).build(),
                SearchKeyword.builder().keyword("홍삼").searchCount(4900L).build(),
                SearchKeyword.builder().keyword("로얄캐닌").searchCount(3200L).build()
        );
        keywordRepository.saveAll(keywords);
    }
}
