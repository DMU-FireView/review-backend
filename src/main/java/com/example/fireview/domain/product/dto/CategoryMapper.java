package com.example.fireview.domain.product.dto;

import com.example.fireview.domain.product.entity.Category;

/**
 * 네이버 쇼핑 API의 category1 문자열을 내부 Category enum으로 변환.
 *
 * 네이버 category1 예시:
 *   "디지털/가전", "패션의류", "화장품/미용", "가구/인테리어",
 *   "식품", "스포츠/레저", "도서/음반/DVD", "출산/육아", "반려동물용품"
 */
public class CategoryMapper {

    private CategoryMapper() {}

    public static Category fromNaver(String naverCategory1) {
        if (naverCategory1 == null || naverCategory1.isBlank()) return Category.ETC;

        return switch (naverCategory1.trim()) {
            case "디지털/가전"                    -> Category.ELECTRONICS;
            case "패션의류", "패션잡화"            -> Category.FASHION;
            case "화장품/미용"                    -> Category.COSMETICS;
            case "가구/인테리어", "생활/건강"       -> Category.HOME_APPLIANCE;
            case "식품"                           -> Category.FOOD;
            case "스포츠/레저"                    -> Category.SPORTS;
            case "도서/음반/DVD"                  -> Category.BOOKS;
            case "출산/육아"                      -> Category.BABY;
            case "반려동물용품"                   -> Category.PET;
            default                              -> Category.ETC;
        };
    }
}
