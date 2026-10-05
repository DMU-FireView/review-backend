package com.example.fireview.domain.wishlist.dto;

import com.example.fireview.domain.cart.dto.CartItemResponse;
import com.example.fireview.domain.cart.entity.CartItem;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.wishlist.entity.Wishlist;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * 번호표만 있는 상품(분석 전, 카테고리 없음)을 찜·장바구니에 담았을 때 터지지 않는지 본다.
 *
 * <p>두 DTO 모두 {@code product.getCategory().getDisplayName()} 과
 * {@code rtiGrade().name()} 을 그대로 불렀다. Data 서버 상품은 둘 다 비어 있어서,
 * 찜을 한 번 거는 순간 목록 조회가 통째로 500 이 된다.
 */
class WishlistCartNullSafetyTest {

    private static Product tagOnly() {
        return Product.builder()
                .id(123L).name("토리든 마스크팩").platform("KURLY")
                .dataPlatform("kurly").dataProductId("1000146248")
                .price(17000L)
                .build();   // category·avgRti 없음
    }

    private static User user() {
        return User.builder().id(1L).email("u@test.com").nickname("tester").build();
    }

    @Test
    void 분석_전_상품을_찜해도_목록이_터지지_않는다() {
        Wishlist wishlist = Wishlist.builder().id(1L).user(user()).product(tagOnly()).build();

        assertThatCode(() -> WishlistResponse.from(wishlist)).doesNotThrowAnyException();

        WishlistResponse res = WishlistResponse.from(wishlist);
        assertThat(res.categoryDisplayName()).isNull();
        assertThat(res.rtiGrade()).isNull();
        assertThat(res.avgRti()).isNull();
        assertThat(res.name()).isEqualTo("토리든 마스크팩");
    }

    @Test
    void 분석_전_상품을_장바구니에_담아도_터지지_않는다() {
        CartItem item = CartItem.builder().id(1L).user(user()).product(tagOnly()).quantity(2).build();

        assertThatCode(() -> CartItemResponse.from(item)).doesNotThrowAnyException();

        CartItemResponse res = CartItemResponse.from(item);
        assertThat(res.categoryDisplayName()).isNull();
        assertThat(res.rtiGrade()).isNull();
        assertThat(res.quantity()).isEqualTo(2);
    }
}
