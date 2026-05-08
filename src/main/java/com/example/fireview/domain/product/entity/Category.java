package com.example.fireview.domain.product.entity;

/**
 * 상품 중분류 (Naver category2 기준)
 * 각 값은 부모 MajorCategory(대분류)를 보유한다.
 */
public enum Category {

    // ── 디지털/가전 ──────────────────────────────────────
    DIGITAL_MOBILE("모바일/태블릿",       MajorCategory.DIGITAL),
    DIGITAL_PC("PC/주변기기",            MajorCategory.DIGITAL),
    DIGITAL_AV("영상/음향",              MajorCategory.DIGITAL),
    DIGITAL_HOME_APPLIANCE("생활가전",   MajorCategory.DIGITAL),
    DIGITAL_KITCHEN("주방가전",          MajorCategory.DIGITAL),

    // ── 패션의류 ─────────────────────────────────────────
    FASHION_WOMEN("여성의류",            MajorCategory.FASHION_CLOTHES),
    FASHION_MEN("남성의류",              MajorCategory.FASHION_CLOTHES),
    FASHION_UNDERWEAR("언더웨어/홈웨어", MajorCategory.FASHION_CLOTHES),
    FASHION_SPORTS("스포츠의류",         MajorCategory.FASHION_CLOTHES),

    // ── 패션잡화 ─────────────────────────────────────────
    ACC_SHOES("신발",                    MajorCategory.FASHION_ACCESSORIES),
    ACC_BAG("가방",                      MajorCategory.FASHION_ACCESSORIES),
    ACC_WALLET("지갑/벨트",              MajorCategory.FASHION_ACCESSORIES),
    ACC_ACCESSORY("액세서리",            MajorCategory.FASHION_ACCESSORIES),

    // ── 뷰티 ─────────────────────────────────────────────
    BEAUTY_SKINCARE("스킨케어",          MajorCategory.BEAUTY),
    BEAUTY_MAKEUP("메이크업",            MajorCategory.BEAUTY),
    BEAUTY_CLEANSING("클렌징",           MajorCategory.BEAUTY),
    BEAUTY_HAIR("헤어케어",              MajorCategory.BEAUTY),
    BEAUTY_BODY("바디케어",              MajorCategory.BEAUTY),

    // ── 식품 ─────────────────────────────────────────────
    FOOD_FRESH("신선식품",               MajorCategory.FOOD),
    FOOD_PROCESSED("가공식품",           MajorCategory.FOOD),
    FOOD_SNACK("간식/디저트",            MajorCategory.FOOD),
    FOOD_BEVERAGE("음료",                MajorCategory.FOOD),
    FOOD_HEALTH("건강식품",              MajorCategory.FOOD),

    // ── 생활/주방 ─────────────────────────────────────────
    LIVING_DAILY("생활용품",             MajorCategory.LIVING),
    LIVING_KITCHEN("주방용품",           MajorCategory.LIVING),
    LIVING_STORAGE("수납/정리",          MajorCategory.LIVING),
    LIVING_SAFETY("안전/공구",           MajorCategory.LIVING),

    // ── 가구/인테리어 ────────────────────────────────────
    FURNITURE_MAIN("가구",               MajorCategory.FURNITURE),
    FURNITURE_BEDDING("침구",            MajorCategory.FURNITURE),
    FURNITURE_DECO("홈데코",             MajorCategory.FURNITURE),
    FURNITURE_DIY("DIY/시공",            MajorCategory.FURNITURE),

    // ── 스포츠/레저 ──────────────────────────────────────
    SPORTS_FITNESS("헬스/요가",          MajorCategory.SPORTS),
    SPORTS_OUTDOOR("등산/캠핑",          MajorCategory.SPORTS),
    SPORTS_BIKE("자전거/보드",           MajorCategory.SPORTS),
    SPORTS_GOLF("골프",                  MajorCategory.SPORTS),

    // ── 자동차/공구 ──────────────────────────────────────
    AUTO_CAR("자동차용품",               MajorCategory.AUTO),
    AUTO_MOTO("오토바이용품",            MajorCategory.AUTO),
    AUTO_TOOL("공구/산업용품",           MajorCategory.AUTO),

    // ── 출산/유아동 ──────────────────────────────────────
    BABY_NEWBORN("출산/육아",            MajorCategory.BABY),
    BABY_GOODS("유아용품",               MajorCategory.BABY),
    BABY_CLOTHES("유아동 의류",          MajorCategory.BABY),
    BABY_TOY("장난감/교구",              MajorCategory.BABY),

    // ── 반려동물 ─────────────────────────────────────────
    PET_DOG("강아지용품",                MajorCategory.PET),
    PET_CAT("고양이용품",                MajorCategory.PET),
    PET_OTHER("소동물/어항",             MajorCategory.PET),

    // ── 도서/문구/취미 ───────────────────────────────────
    BOOKS_BOOK("도서",                   MajorCategory.BOOKS),
    BOOKS_STATIONERY("문구/사무",        MajorCategory.BOOKS),
    BOOKS_HOBBY("취미",                  MajorCategory.BOOKS),
    BOOKS_TICKET("티켓/굿즈",            MajorCategory.BOOKS),

    // ── 여행/서비스 ──────────────────────────────────────
    TRAVEL_GOODS("여행용품",             MajorCategory.TRAVEL),
    TRAVEL_LODGING("숙박/티켓",          MajorCategory.TRAVEL),
    TRAVEL_RENTAL("렌탈/구독",           MajorCategory.TRAVEL),

    // ── 명품/브랜드 ──────────────────────────────────────
    LUXURY_ACCESSORIES("명품잡화",       MajorCategory.LUXURY),
    LUXURY_FASHION("브랜드패션",         MajorCategory.LUXURY),
    LUXURY_BEAUTY("프리미엄뷰티",        MajorCategory.LUXURY),

    // ── 기타 ─────────────────────────────────────────────
    ETC("기타",                          MajorCategory.ETC);

    private final String displayName;
    private final MajorCategory major;

    Category(String displayName, MajorCategory major) {
        this.displayName = displayName;
        this.major = major;
    }

    public String getDisplayName() {
        return displayName;
    }

    public MajorCategory getMajor() {
        return major;
    }
}
