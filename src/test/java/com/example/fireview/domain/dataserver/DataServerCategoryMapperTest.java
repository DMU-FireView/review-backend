package com.example.fireview.domain.dataserver;

import com.example.fireview.domain.product.entity.Category;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 운영 Data 서버에서 실제로 받은 카테고리·상품명으로 확인한다.
 */
class DataServerCategoryMapperTest {

    @ParameterizedTest(name = "[{index}] {0} / {1} → {2}")
    @CsvSource(delimiter = '|', nullValues = "NULL", value = {
            // 상세 응답의 카테고리 경로 (컬리·무신사·11번가)
            "면/즉석식품/통조림 > 라면 > 봉지라면 | [삼양] 삼양라면 5입 | FOOD_PROCESSED",
            "가공식품>컵라면>일반컵라면 | 농심 신라면컵 65g, 30개 | FOOD_PROCESSED",
            "커피/차 > 커피 > 인스턴트 커피 | [모모스커피] 스페셜티 아메리카노 스틱 커피 | FOOD_BEVERAGE",
            "가전제품 > 음향가전 > 이어폰/헤드셋 | [블라우풍트] 블루투스 귀걸이형 이어폰 | DIGITAL_AV",
            "Digital/Tech > 음향가전 > 이어폰 | NM IN-EAR HEADPHONES | DIGITAL_AV",
            "음향가전>이어폰>무선 이어폰 | 애플 에어팟 5 | DIGITAL_AV",
            "Sportsshoes > 러닝화 > 미드화 | 디비에이트 나이트로 엘리트 4 | ACC_SHOES",
            "패션/잡화 > 슈즈 > 스니커즈 | [베어파우] JASPER 운동화 4종 택1 | ACC_SHOES",
            "패션/잡화 > 의류 > 티셔츠 | [베네통] 여성 긴팔 티셔츠 4종 택1 | FASHION_WOMEN",
            "Sportswear > 상의 > 반소매 티셔츠 | [SWT24047-01] 히밥 티셔츠 (라면) 화이트 | FASHION_SPORTS",
            "Life > 식기/그릇 > 식기/공기/그릇 | 스폰지밥 비키니시티 주민들 라면 그릇 | LIVING_KITCHEN",
            "Clothing > 티셔츠 > 반소매 티셔츠 | 3-스트라이프 슬림 티셔츠 - 아우라커피 | FASHION_WOMEN",
            "뷰티 인디 > 인디 썬케어 > 인디 썬크림 | [풀리] 쌀 세라 수분 선크림 50ml | BEAUTY_SKINCARE",
            "Beauty > 선케어 > 선크림 | 구달 어성초 선크림 | BEAUTY_SKINCARE",
            "스킨케어>스킨/토너>스킨/토너 | 토너 | BEAUTY_SKINCARE",
            // 올리브영 검색 응답의 넓은 값
            "향수/디퓨저,맨즈에딧 | 오드 퍼퓸 50ml | LUXURY_BEAUTY",
            "더모 코스메틱,클렌징,클렌징,클렌징 | 약산성 클렌징폼 | BEAUTY_CLEANSING",
            "구강용품,구강용품 | 미백 치약 | LIVING_DAILY",
            "메이크업 | 벨벳 립 틴트 | BEAUTY_MAKEUP",
            // 넓은 값이면 상품명이 더 구체적이다
            "홈리빙/가전 | 스탠리 퀜처 텀블러 887ml | LIVING_KITCHEN",
            "푸드 | 맥심 커피믹스 100T | FOOD_BEVERAGE",
            // 카테고리 없이 상품명만 (컬리·무신사·11번가 검색 응답)
            "NULL | [달바] 워터풀 에센스 선크림 50ml 1+1 세트 | BEAUTY_SKINCARE",
            "NULL | 차량용 고속 충전기 | AUTO_CAR",
            "NULL | 캠핑 의자 경량 | SPORTS_OUTDOOR",
            "NULL | 강아지 샴푸 500ml | PET_DOG",
            "NULL | 브라운 오버핏 맨투맨 남성 | FASHION_MEN",
            "NULL | [공구] 유기농 바나나 1kg | FOOD_FRESH",
            "NULL | 핫도그 10개입 | NULL",
            "NULL | 바닐라 아이스크림 | FOOD_SNACK",
            // 검증 중 발견한 오분류
            "NULL | 케라시스 애플 사이다 비니거 샴푸1L 2개 | BEAUTY_HAIR",
            "NULL | [제주우유] 저지우유 750mL | FOOD_FRESH",
            "NULL | 케라시스 퍼퓸 체리블라썸 1Lx3개 (샴푸2 + 린스1) | BEAUTY_HAIR",
            "NULL | 블랑101 퍼퓸 세탁세제 + 섬유유연제 1.6L | LIVING_DAILY",
            "NULL | [더블유드레스룸] 라이프퍼퓸 향수 100ml | LUXURY_BEAUTY",
            "NULL | [삼익가구] 리클라이너 체어 컴퓨터 의자 | FURNITURE_MAIN",
            "NULL | [레토] 써클 LED스탠드 책상 스탠드 조명 | FURNITURE_DECO",
            "NULL | 에이밍 컨트롤 골프티 Control Tee 4개세트 | SPORTS_GOLF",
            "NULL | 옷이 예뻐지는 기능성 세탁세제 어두운 의류용 | LIVING_DAILY",
            "NULL | 헤라 선메이트 프로텍터 SPF50+ 50ml | BEAUTY_SKINCARE",
            "NULL | Lettering Raglan Long Sleeve Tee - Brown | FASHION_WOMEN",
            "NULL | 레플리카 로스트 에덴 EDP 100ML | LUXURY_BEAUTY",
            "취미/팬시 | QCY 블루투스 이어폰 HT12 | DIGITAL_AV",
            "헬스/건강용품 | 아디다스 프리미엄 요가매트 5mm | SPORTS_FITNESS",
    })
    void 실제_쇼핑몰_값을_분류한다(String category, String name, String expected) {
        Category result = DataServerCategoryMapper.classify(category, name);

        assertThat(result).isEqualTo(expected == null ? null : Category.valueOf(expected));
    }

    @Test
    void 근거가_없으면_null이다() {
        // 기타로 넣으면 엉뚱한 분류가 진짜처럼 보인다
        assertThat(DataServerCategoryMapper.classify(null, null)).isNull();
        assertThat(DataServerCategoryMapper.classify("  ", "ABC-123 블랙")).isNull();
    }

    @Test
    void 쇼핑몰_경로가_상품명보다_우선한다() {
        // 이름엔 "커피"가 있지만 무신사가 의류로 분류했다
        assertThat(DataServerCategoryMapper.classify(
                "Clothing > 바지 > 트레이닝 팬츠", "체크 트라우저 루즈 팬츠 - 아우라커피"))
                .isEqualTo(Category.FASHION_SPORTS);
    }
}
