package com.example.fireview.domain.wishlist.service;

import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.service.ProductService;
import com.example.fireview.domain.user.entity.User;
import com.example.fireview.domain.user.service.UserService;
import com.example.fireview.domain.wishlist.dto.WishlistResponse;
import com.example.fireview.domain.wishlist.entity.Wishlist;
import com.example.fireview.domain.wishlist.repository.WishlistRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserService userService;
    private final ProductService productService;

    /** 찜 목록 조회 */
    public List<WishlistResponse> getWishlist(String userEmail) {
        User user = userService.findByEmail(userEmail);
        return wishlistRepository.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(WishlistResponse::from)
                .toList();
    }

    /** 찜 추가 */
    @Transactional
    public WishlistResponse addWishlist(String userEmail, Long productId) {
        User user = userService.findByEmail(userEmail);
        Product product = productService.findById(productId);

        if (wishlistRepository.existsByUser_IdAndProduct_Id(user.getId(), productId)) {
            throw new CustomException(ErrorCode.WISHLIST_ALREADY_EXISTS);
        }

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        return WishlistResponse.from(wishlistRepository.save(wishlist));
    }

    /** 찜 삭제 */
    @Transactional
    public void removeWishlist(String userEmail, Long productId) {
        User user = userService.findByEmail(userEmail);
        if (!wishlistRepository.existsByUser_IdAndProduct_Id(user.getId(), productId)) {
            throw new CustomException(ErrorCode.WISHLIST_NOT_FOUND);
        }
        wishlistRepository.deleteByUser_IdAndProduct_Id(user.getId(), productId);
    }

    /** 찜 여부 확인 */
    public boolean isWishlisted(String userEmail, Long productId) {
        User user = userService.findByEmail(userEmail);
        return wishlistRepository.existsByUser_IdAndProduct_Id(user.getId(), productId);
    }
}
