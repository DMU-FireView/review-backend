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
import com.example.fireview.domain.user.entity.Role;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

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

    @Override
    public void run(String... args) {
        if (productRepository.count() > 0) return;
        log.info("샘플 데이터 초기화 시작...");

        createUsers();
        List<Product> products = createProducts();
        createReviews(products);
        createKeywords();

        log.info("샘플 데이터 초기화 완료.");
    }

    private void createUsers() {
        userRepository.save(User.builder()
                .email("admin@fireview.com")
                .password(passwordEncoder.encode("password1!"))
                .nickname("관리자")
                .role(Role.ADMIN)
                .onboardingCompleted(true)
                .build());

        userRepository.save(User.builder()
                .email("user@fireview.com")
                .password(passwordEncoder.encode("password1!"))
                .nickname("테스트유저")
                .role(Role.USER)
                .onboardingCompleted(false)
                .build());
    }

    private List<Product> createProducts() {
        return productRepository.saveAll(List.of(
                Product.builder().name("삼성 갤럭시 S25 Ultra").imageUrl("https://via.placeholder.com/300x300?text=Galaxy+S25").price(1499000L).category(Category.ELECTRONICS).avgRti(82.5).reviewCount(0).avgRating(4.5).build(),
                Product.builder().name("LG 그램 17인치 노트북").imageUrl("https://via.placeholder.com/300x300?text=LG+Gram").price(1890000L).category(Category.ELECTRONICS).avgRti(88.0).reviewCount(0).avgRating(4.7).build(),
                Product.builder().name("애플 에어팟 프로 2세대").imageUrl("https://via.placeholder.com/300x300?text=AirPods+Pro").price(359000L).category(Category.ELECTRONICS).avgRti(91.0).reviewCount(0).avgRating(4.8).build(),
                Product.builder().name("나이키 에어맥스 270").imageUrl("https://via.placeholder.com/300x300?text=Nike+270").price(179000L).category(Category.FASHION).avgRti(78.5).reviewCount(0).avgRating(4.3).build(),
                Product.builder().name("뉴발란스 574 클래식").imageUrl("https://via.placeholder.com/300x300?text=NB+574").price(129000L).category(Category.FASHION).avgRti(85.0).reviewCount(0).avgRating(4.6).build(),
                Product.builder().name("설화수 자음생 크림 60ml").imageUrl("https://via.placeholder.com/300x300?text=Sulwhasoo").price(185000L).category(Category.COSMETICS).avgRti(43.0).reviewCount(0).avgRating(3.8).build(),
                Product.builder().name("아이오페 레티놀 엑스퍼트 0.1%").imageUrl("https://via.placeholder.com/300x300?text=IOPE").price(62000L).category(Category.COSMETICS).avgRti(55.0).reviewCount(0).avgRating(4.1).build(),
                Product.builder().name("다이슨 에어랩 멀티 스타일러").imageUrl("https://via.placeholder.com/300x300?text=Dyson+Airwrap").price(689000L).category(Category.HOME_APPLIANCE).avgRti(67.0).reviewCount(0).avgRating(4.2).build()
        ));
    }

    private void createReviews(List<Product> products) {
        record SampleReview(String content, int rating, String reviewer, boolean verified, int hourOffset) {}

        List<SampleReview> goodReviews = List.of(
                new SampleReview("실제로 써보니 정말 만족스럽습니다. 배송도 빠르고 제품 품질이 기대 이상이에요. 화면이 선명하고 배터리도 하루 종일 쓸 수 있어서 좋습니다.", 5, "구매자A", true, 14),
                new SampleReview("두 달 정도 사용해봤는데 품질이 정말 좋네요. 처음 살 때 걱정했는데 완전히 만족합니다. 다음에도 이 브랜드로 구매할 것 같아요.", 4, "구매자B", true, 10),
                new SampleReview("가격 대비 성능이 훌륭합니다. 디자인도 마음에 들고 실용적으로 잘 쓰고 있어요. 주변 지인들에게도 추천했습니다.", 4, "구매자C", true, 15),
                new SampleReview("제품 받고 바로 써봤는데 생각보다 훨씬 좋아요. 특히 성능 면에서 기대를 뛰어넘었습니다. 포장도 꼼꼼하게 되어있었어요.", 5, "구매자D", true, 11)
        );

        List<SampleReview> suspiciousReviews = List.of(
                new SampleReview("완전대박 인생템 강추강추!!! 무조건 사야됨 역대급이에요!!", 5, "계정123", false, 3),
                new SampleReview("강추 완전최고 강추 완전최고 재구매확정 강추", 5, "신규유저X", false, 2),
                new SampleReview("짱 좋아요", 5, "리뷰어01", false, 3),
                new SampleReview("돈값해요 완벽 실망없음 무조건구매 혜자템!!", 5, "계정456", false, 4)
        );

        for (Product product : products) {
            int goodCount = product.getAvgRti() >= 70 ? 4 : 2;
            int badCount = product.getAvgRti() < 60 ? 3 : 1;

            for (int i = 0; i < goodCount; i++) {
                SampleReview s = goodReviews.get(i % goodReviews.size());
                LocalDateTime writtenAt = LocalDateTime.now().minusDays(30 + i * 5).withHour(s.hourOffset());
                RtiEngineService.RtiResult result = rtiEngine.calculate(s.content(), s.reviewer() + product.getId(), writtenAt, s.verified());
                reviewRepository.save(Review.builder()
                        .product(product)
                        .reviewerNickname(s.reviewer())
                        .reviewerId(s.reviewer() + "_" + product.getId())
                        .content(s.content())
                        .rating(s.rating())
                        .rtiScore(result.score())
                        .trustGrade(result.grade())
                        .reasons(result.reasons())
                        .writtenAt(writtenAt)
                        .isVerifiedPurchase(s.verified())
                        .build());
            }

            for (int i = 0; i < badCount; i++) {
                SampleReview s = suspiciousReviews.get(i % suspiciousReviews.size());
                LocalDateTime writtenAt = LocalDateTime.now().minusDays(5 + i).withHour(s.hourOffset());
                RtiEngineService.RtiResult result = rtiEngine.calculate(s.content(), s.reviewer() + product.getId(), writtenAt, s.verified());
                reviewRepository.save(Review.builder()
                        .product(product)
                        .reviewerNickname(s.reviewer())
                        .reviewerId(s.reviewer() + "_" + product.getId())
                        .content(s.content())
                        .rating(s.rating())
                        .rtiScore(result.score())
                        .trustGrade(result.grade())
                        .reasons(result.reasons())
                        .writtenAt(writtenAt)
                        .isVerifiedPurchase(s.verified())
                        .build());
            }
        }
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
                SearchKeyword.builder().keyword("아이오페").searchCount(5800L).build()
        );
        keywordRepository.saveAll(keywords);
    }
}