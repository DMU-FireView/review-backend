package com.example.fireview.domain.product.service;

import com.example.fireview.domain.product.dto.ProductResponse;
import com.example.fireview.domain.product.entity.Product;
import com.example.fireview.domain.product.repository.ProductRepository;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public Product findById(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
    }

    public ProductResponse getProduct(Long id) {
        return ProductResponse.from(findById(id));
    }

    @Cacheable(value = "productList", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll().stream()
                .map(ProductResponse::from)
                .toList();
    }

    public List<ProductResponse> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(ProductResponse::from)
                .toList();
    }

    @Transactional
    public void updateAvgRti(Long productId, double newAvgRti) {
        Product product = findById(productId);
        product.setAvgRti(newAvgRti);
        productRepository.save(product);
    }
}