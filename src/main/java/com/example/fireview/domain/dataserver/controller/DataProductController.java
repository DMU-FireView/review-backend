package com.example.fireview.domain.dataserver.controller;

import com.example.fireview.domain.dataserver.dto.response.DataProductResponse;
import com.example.fireview.domain.dataserver.service.DataProductService;
import com.example.fireview.global.exception.CustomException;
import com.example.fireview.global.exception.ErrorCode;
import com.example.fireview.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Data 서버 기반 상품 조회 (v2).
 *
 * <p>기존 {@code /api/products/**} 와 **병행 운영**한다. 한 번에 바꾸면 되돌릴 수 없으므로
 * 프론트가 화면 단위로 옮긴다. 기존 경로는 Spring DB 의 더미를 계속 내려준다.
 */
@Tag(name = "상품 (Data 서버)", description = "Data 서버 기반 상품·리뷰 조회. 기존 /api/products 를 대체할 경로")
@RestController
@RequestMapping("/api/v2/products")
@RequiredArgsConstructor
public class DataProductController {

    private final DataProductService dataProductService;

    /**
     * 상품 상세 + 리뷰 첫 페이지
     * GET /api/v2/products/{platform}/{productId}
     */
    @Operation(summary = "상품 상세 조회 (Data 서버)", description = """
            Data 서버가 소유한 상품과 리뷰를 가져온다. Spring 은 조합만 한다.

            **`collectionStatus` 로 화면을 가른다.** 이 값을 무시하면 사용자가 빈 화면을 보고
            고장났다고 느낀다.

            | collectionStatus | HTTP | product | 화면 |
            |---|---|---|---|
            | `FRESH` | 200 | 있음 | 그대로 표시 |
            | `STALE` | 200 | 있음 | 그대로 표시 + "갱신 중" 정도 |
            | `QUEUED` | 200 | **null** | 로딩 화면. `job.id` 로 완료를 기다린다 |
            | `UNAVAILABLE` | 200 | **null** | 오류 안내. 상품이 없는 게 아니라 못 가져온 것 |

            처음 보는 상품은 **반드시 `QUEUED` 를 한 번 거친다.** Data 서버가 그때 수집을
            시작하므로, 로딩 화면 없이 바로 상세를 열면 빈 화면이 된다.

            `STALE` 은 실패가 아니다. 쓸 수 있는 데이터가 들어 있고, 최신을 기다리면 화면이
            크롤링 속도에 묶인다.

            **`springProductId` 는 null 일 수 있다.** 찜·장바구니에 쓸 Spring 쪽 번호인데,
            아직 아무도 찜하지 않은 상품은 번호가 없다. 상품을 열어보기만 해도 번호를 만들면
            빈 행이 계속 쌓이므로 그렇게 하지 않는다.

            **`analysis` 는 현재 항상 null 이다.** 신뢰도 분석(RTI·등급·사유)은 Data 서버도
            AI 서버도 아직 제공하지 않는다. 자리만 잡아둔 것이다.

            리뷰는 cursor 페이지네이션이다. 응답의 `reviews.nextCursor` 를 다음 요청의
            `cursor` 에 그대로 넣는다. null 이면 마지막 페이지다.
            """)
    @GetMapping("/{platform}/{productId}")
    public ApiResponse<DataProductResponse> getProduct(
            @PathVariable String platform,
            @PathVariable String productId,
            @RequestParam(required = false) String cursor) {

        if (platform.isBlank() || productId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return ApiResponse.success(dataProductService.getProduct(platform, productId, cursor));
    }

    /**
     * 수집 job 상태
     * GET /api/v2/products/collection-jobs/{jobId}
     */
    @Operation(summary = "수집 job 상태 조회", description = """
            `collectionStatus=QUEUED` 를 받았을 때 `job.id` 로 완료를 기다린다.

            `status` 가 `succeeded` 또는 `partial` 이 되면 상품 상세를 다시 호출한다.
            `partial` 은 상품만 수집되고 리뷰가 실패한 상태다 — 상품은 보여줄 수 있다.
            `failed` 면 재시도해도 같을 가능성이 높으므로 `lastError` 를 안내한다.

            폴링 간격은 2~3초를 권한다. 크롤링이라 수 초~수십 초가 걸린다.
            """)
    @GetMapping("/collection-jobs/{jobId}")
    public ApiResponse<DataProductResponse.CollectionJobStatus> getJob(@PathVariable long jobId) {
        return ApiResponse.success(dataProductService.getJob(jobId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND)));
    }
}
