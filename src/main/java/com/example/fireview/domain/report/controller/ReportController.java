package com.example.fireview.domain.report.controller;

import com.example.fireview.domain.report.dto.request.ReportCreateRequest;
import com.example.fireview.domain.report.dto.response.ReportResponse;
import com.example.fireview.domain.report.dto.response.ReportSummaryResponse;
import com.example.fireview.domain.report.service.ReportService;
import com.example.fireview.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

@Tag(name = "신고", description = "리뷰 신고 접수 및 조회")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * 리뷰 신고 제출
     * POST /api/reports/reviews/{reviewId}
     */
    @Operation(summary = "리뷰 신고 (Data 서버 리뷰)", description = """
            `/api/v2/products/**` 로 조회한 리뷰를 신고한다. 리뷰 ID 는 쇼핑몰이 발급한
            원본 값(`reviews.items[].reviewId`)을 그대로 넣는다.

            Spring DB 에 그 리뷰 행이 없으므로 **본문은 저장하지 않는다.** 응답의
            `reviewId` 와 `reviewContent` 는 null 이고, `externalReviewId` 와
            `productExternalId` 가 대신 채워진다. 운영자는 상품으로 들어가 확인한다.

            상품 번호표가 없으면 이 호출에서 발급된다. 아직 수집 전인 상품이면
            `409 PRODUCT_NOT_COLLECTED` 로 거절된다 — 실재하지 않는 리뷰에 대한 신고가
            쌓이지 않게 하기 위한 것이다.

            같은 리뷰를 두 번 신고하면 `409 REPORT_ALREADY_EXISTS` 다.
            """)
    @PostMapping("/external/{platform}/{productId}/reviews/{externalReviewId}")
    public ApiResponse<ReportResponse> createExternalReport(
            @PathVariable String platform,
            @PathVariable String productId,
            @PathVariable String externalReviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportCreateRequest request) {
        return ApiResponse.success("신고가 접수되었습니다.",
                reportService.createExternalReport(platform, productId, externalReviewId,
                        jwt.getSubject(), request));
    }

    @PostMapping("/reviews/{reviewId}")
    public ApiResponse<ReportResponse> createReport(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody ReportCreateRequest request) {
        ReportResponse response = reportService.createReport(reviewId, jwt.getSubject(), request);
        return ApiResponse.success("신고가 접수되었습니다.", response);
    }

    /**
     * 내 신고 목록 조회
     * GET /api/reports/me?page=0&size=10
     */
    @GetMapping("/me")
    public ApiResponse<Page<ReportSummaryResponse>> getMyReports(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        Page<ReportSummaryResponse> response = reportService.getMyReports(jwt.getSubject(), pageable);
        return ApiResponse.success(response);
    }

    /**
     * 내 신고 단건 조회
     * GET /api/reports/me/{reportId}
     */
    @GetMapping("/me/{reportId}")
    public ApiResponse<ReportResponse> getMyReport(
            @PathVariable Long reportId,
            @AuthenticationPrincipal Jwt jwt) {
        ReportResponse response = reportService.getMyReport(reportId, jwt.getSubject());
        return ApiResponse.success(response);
    }
}
