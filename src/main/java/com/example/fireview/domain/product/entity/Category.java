package com.example.fireview.domain.product.entity;

public enum Category {
    ELECTRONICS("전자기기"),
    FASHION("패션"),
    COSMETICS("화장품"),
    HOME_APPLIANCE("생활가전"),
    FOOD("식품"),
    SPORTS("스포츠"),
    BOOKS("도서"),
    BABY("유아"),
    PET("반려동물"),
    ETC("기타");

    private final String displayName;

    Category(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}