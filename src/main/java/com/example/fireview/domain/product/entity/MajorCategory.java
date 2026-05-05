package com.example.fireview.domain.product.entity;

/**
 * 상품 대분류 (Naver category1 기준)
 */
public enum MajorCategory {
    DIGITAL("디지털/가전"),
    FASHION_CLOTHES("패션의류"),
    FASHION_ACCESSORIES("패션잡화"),
    BEAUTY("뷰티"),
    FOOD("식품"),
    LIVING("생활/주방"),
    FURNITURE("가구/인테리어"),
    SPORTS("스포츠/레저"),
    AUTO("자동차/공구"),
    BABY("출산/유아동"),
    PET("반려동물"),
    BOOKS("도서/문구/취미"),
    TRAVEL("여행/서비스"),
    LUXURY("명품/브랜드"),
    ETC("기타");

    private final String displayName;

    MajorCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
