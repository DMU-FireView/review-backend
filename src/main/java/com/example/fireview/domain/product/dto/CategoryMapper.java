package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.entity.Category;

/**
 * 네이버 쇼핑 API의 category1/2 → 내부 Category(중분류) 변환.
 *
 * 우선순위: category1 + category2 조합 → category1 단독 fallback → ETC
 */
public class CategoryMapper {

    private CategoryMapper() {}

    /**
     * category1(대분류), category2(중분류)를 조합해 내부 Category를 반환한다.
     */
    public static Category fromNaver(String category1, String category2) {
        String c1 = category1 == null ? "" : category1.trim();
        String c2 = category2 == null ? "" : category2.trim();

        return switch (c1) {
            case "디지털/가전"                          -> mapDigital(c2);
            case "패션의류"                             -> mapFashionClothes(c2);
            case "패션잡화"                             -> mapFashionAcc(c2);
            case "화장품/미용", "뷰티"                  -> mapBeauty(c2);
            case "식품"                                 -> mapFood(c2);
            case "생활/건강", "생활/주방"               -> mapLiving(c2);
            case "가구/인테리어"                        -> mapFurniture(c2);
            case "스포츠/레저"                          -> mapSports(c2);
            case "자동차/공구"                          -> mapAuto(c2);
            case "출산/육아", "출산/유아동"             -> mapBaby(c2);
            case "반려동물용품", "반려동물"             -> mapPet(c2);
            case "도서/음반/DVD", "도서/문구/취미"      -> mapBooks(c2);
            case "여행/서비스"                          -> mapTravel(c2);
            case "명품/브랜드"                          -> mapLuxury(c2);
            default                                     -> Category.ETC;
        };
    }

    /** category1만 있을 때 사용 (하위 호환) */
    public static Category fromNaver(String category1) {
        return fromNaver(category1, "");
    }

    // ── 디지털/가전 ───────────────────────────────────────────────────────
    private static Category mapDigital(String c2) {
        return switch (c2) {
            case "휴대폰/스마트폰", "태블릿PC", "스마트워치", "모바일/태블릿",
                 "휴대폰액세서리", "스마트폰액세서리"  -> Category.DIGITAL_MOBILE;
            case "노트북", "데스크탑", "모니터", "PC주변기기", "PC/주변기기",
                 "저장장치", "키보드", "마우스"         -> Category.DIGITAL_PC;
            case "TV", "프로젝터", "이어폰/헤드폰", "스피커", "영상/음향",
                 "오디오"                              -> Category.DIGITAL_AV;
            case "청소기", "공기청정기", "선풍기", "제습기", "가습기",
                 "생활가전"                            -> Category.DIGITAL_HOME_APPLIANCE;
            case "전자레인지", "에어프라이어", "커피머신", "밥솥", "믹서기",
                 "주방가전"                            -> Category.DIGITAL_KITCHEN;
            default -> Category.DIGITAL_MOBILE;
        };
    }

    // ── 패션의류 ──────────────────────────────────────────────────────────
    private static Category mapFashionClothes(String c2) {
        return switch (c2) {
            case "여성의류", "원피스", "티셔츠", "셔츠", "니트", "스커트",
                 "아우터", "여성팬츠"                  -> Category.FASHION_WOMEN;
            case "남성의류", "남성티셔츠", "남성셔츠", "남성니트", "남성팬츠",
                 "정장"                                -> Category.FASHION_MEN;
            case "언더웨어/홈웨어", "속옷", "잠옷", "실내복",
                 "양말"                                -> Category.FASHION_UNDERWEAR;
            case "스포츠의류", "트레이닝복", "레깅스",
                 "수영복"                              -> Category.FASHION_SPORTS;
            default -> Category.FASHION_WOMEN;
        };
    }

    // ── 패션잡화 ──────────────────────────────────────────────────────────
    private static Category mapFashionAcc(String c2) {
        return switch (c2) {
            case "신발", "스니커즈", "구두", "샌들", "부츠", "슬리퍼" -> Category.ACC_SHOES;
            case "가방", "백팩", "숄더백", "토트백", "크로스백",
                 "캐리어"                              -> Category.ACC_BAG;
            case "지갑/벨트", "지갑", "벨트", "카드지갑" -> Category.ACC_WALLET;
            case "액세서리", "모자", "시계", "선글라스", "주얼리",
                 "머플러"                              -> Category.ACC_ACCESSORY;
            default -> Category.ACC_BAG;
        };
    }

    // ── 뷰티 ─────────────────────────────────────────────────────────────
    private static Category mapBeauty(String c2) {
        return switch (c2) {
            case "스킨케어", "스킨/토너", "에센스", "크림", "선케어",
                 "마스크팩"                            -> Category.BEAUTY_SKINCARE;
            case "메이크업", "베이스", "립", "아이메이크업", "치크",
                 "네일"                                -> Category.BEAUTY_MAKEUP;
            case "클렌징", "클렌징폼", "클렌징오일", "리무버" -> Category.BEAUTY_CLEANSING;
            case "헤어케어", "샴푸", "트리트먼트", "헤어에센스",
                 "염색약"                              -> Category.BEAUTY_HAIR;
            case "바디케어", "바디워시", "바디로션", "핸드케어",
                 "풋케어"                              -> Category.BEAUTY_BODY;
            default -> Category.BEAUTY_SKINCARE;
        };
    }

    // ── 식품 ─────────────────────────────────────────────────────────────
    private static Category mapFood(String c2) {
        return switch (c2) {
            case "신선식품", "과일", "채소", "정육", "수산",
                 "계란/유제품"                         -> Category.FOOD_FRESH;
            case "가공식품", "즉석식품", "통조림", "면류",
                 "소스/양념"                           -> Category.FOOD_PROCESSED;
            case "간식/디저트", "과자", "초콜릿", "빵", "떡",
                 "아이스크림"                          -> Category.FOOD_SNACK;
            case "음료", "생수", "커피", "차", "탄산", "주스" -> Category.FOOD_BEVERAGE;
            case "건강식품", "영양제", "단백질", "홍삼", "유산균" -> Category.FOOD_HEALTH;
            default -> Category.FOOD_PROCESSED;
        };
    }

    // ── 생활/주방 ─────────────────────────────────────────────────────────
    private static Category mapLiving(String c2) {
        return switch (c2) {
            case "생활용품", "세제", "휴지", "청소용품", "욕실용품" -> Category.LIVING_DAILY;
            case "주방용품", "냄비", "프라이팬", "식기", "보관용기" -> Category.LIVING_KITCHEN;
            case "수납/정리", "리빙박스", "옷걸이", "선반", "압축팩" -> Category.LIVING_STORAGE;
            case "안전/공구", "공구", "멀티탭", "방범용품",
                 "재난용품"                            -> Category.LIVING_SAFETY;
            default -> Category.LIVING_DAILY;
        };
    }

    // ── 가구/인테리어 ─────────────────────────────────────────────────────
    private static Category mapFurniture(String c2) {
        return switch (c2) {
            case "가구", "침대", "소파", "책상", "의자", "수납장" -> Category.FURNITURE_MAIN;
            case "침구", "이불", "베개", "매트리스커버", "토퍼" -> Category.FURNITURE_BEDDING;
            case "홈데코", "조명", "커튼", "러그", "액자", "시계" -> Category.FURNITURE_DECO;
            case "DIY/시공", "벽지", "바닥재", "페인트" -> Category.FURNITURE_DIY;
            default -> Category.FURNITURE_MAIN;
        };
    }

    // ── 스포츠/레저 ───────────────────────────────────────────────────────
    private static Category mapSports(String c2) {
        return switch (c2) {
            case "헬스/요가", "덤벨", "운동기구", "보호대", "매트" -> Category.SPORTS_FITNESS;
            case "등산/캠핑", "텐트", "침낭", "캠핑가구", "등산화" -> Category.SPORTS_OUTDOOR;
            case "자전거/보드", "자전거", "킥보드", "헬멧",
                 "보호장비"                            -> Category.SPORTS_BIKE;
            case "골프", "골프클럽", "골프공", "골프웨어",
                 "거리측정기"                          -> Category.SPORTS_GOLF;
            default -> Category.SPORTS_FITNESS;
        };
    }

    // ── 자동차/공구 ───────────────────────────────────────────────────────
    private static Category mapAuto(String c2) {
        return switch (c2) {
            case "자동차용품", "차량용충전기", "거치대", "방향제",
                 "세차용품"                            -> Category.AUTO_CAR;
            case "오토바이용품", "오토바이헬멧", "오토바이장갑" -> Category.AUTO_MOTO;
            case "공구/산업용품", "전동공구", "수공구", "작업복",
                 "측정기"                              -> Category.AUTO_TOOL;
            default -> Category.AUTO_CAR;
        };
    }

    // ── 출산/유아동 ───────────────────────────────────────────────────────
    private static Category mapBaby(String c2) {
        return switch (c2) {
            case "출산/육아", "기저귀", "분유", "젖병", "유모차",
                 "카시트"                              -> Category.BABY_NEWBORN;
            case "유아용품", "이유식", "목욕용품", "안전용품" -> Category.BABY_GOODS;
            case "유아동 의류", "베이비의류", "아동의류",
                 "아동신발"                            -> Category.BABY_CLOTHES;
            case "장난감/교구", "블록", "인형", "보드게임",
                 "학습완구"                            -> Category.BABY_TOY;
            default -> Category.BABY_NEWBORN;
        };
    }

    // ── 반려동물 ──────────────────────────────────────────────────────────
    private static Category mapPet(String c2) {
        return switch (c2) {
            case "강아지용품", "강아지사료", "강아지간식", "배변용품",
                 "하네스"                              -> Category.PET_DOG;
            case "고양이용품", "고양이사료", "고양이간식", "모래",
                 "캣타워"                              -> Category.PET_CAT;
            case "소동물/어항", "햄스터용품", "새용품", "어항" -> Category.PET_OTHER;
            default -> Category.PET_DOG;
        };
    }

    // ── 도서/문구/취미 ────────────────────────────────────────────────────
    private static Category mapBooks(String c2) {
        return switch (c2) {
            case "도서", "소설", "자기계발", "경제경영", "아동도서",
                 "참고서"                              -> Category.BOOKS_BOOK;
            case "문구/사무", "필기구", "노트", "파일", "사무기기" -> Category.BOOKS_STATIONERY;
            case "취미", "악기", "미술용품", "프라모델", "수집품" -> Category.BOOKS_HOBBY;
            case "티켓/굿즈", "공연티켓", "캐릭터굿즈", "팬굿즈" -> Category.BOOKS_TICKET;
            default -> Category.BOOKS_BOOK;
        };
    }

    // ── 여행/서비스 ───────────────────────────────────────────────────────
    private static Category mapTravel(String c2) {
        return switch (c2) {
            case "여행용품", "파우치", "목베개", "어댑터" -> Category.TRAVEL_GOODS;
            case "숙박/티켓", "호텔", "펜션", "입장권", "체험권" -> Category.TRAVEL_LODGING;
            case "렌탈/구독", "가전렌탈", "정기배송", "생활구독" -> Category.TRAVEL_RENTAL;
            default -> Category.TRAVEL_GOODS;
        };
    }

    // ── 명품/브랜드 ───────────────────────────────────────────────────────
    private static Category mapLuxury(String c2) {
        return switch (c2) {
            case "명품잡화", "명품가방", "명품지갑", "명품시계" -> Category.LUXURY_ACCESSORIES;
            case "브랜드패션", "디자이너의류", "브랜드신발",
                 "브랜드잡화"                          -> Category.LUXURY_FASHION;
            case "프리미엄뷰티", "고가화장품", "향수", "뷰티기기" -> Category.LUXURY_BEAUTY;
            default -> Category.LUXURY_ACCESSORIES;
        };
    }
}
