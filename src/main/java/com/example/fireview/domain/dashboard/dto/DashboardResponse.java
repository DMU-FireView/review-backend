package com.example.fireview.domain.dashboard.dto;

import com.example.fireview.domain.product.dto.ProductResponse;

import java.util.List;

public record DashboardResponse(
        List<ProductResponse> recommendedProducts,
        List<ProductResponse> recentProducts,
        List<ProductResponse> riskyProducts,
        List<String> popularKeywords
) {}