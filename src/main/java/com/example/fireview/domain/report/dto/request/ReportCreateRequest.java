package com.example.fireview.domain.report.dto.request;

import com.example.fireview.domain.report.entity.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(

        @NotNull(message = "신고 사유를 선택해주세요.")
        ReportReason reason,

        @Size(min = 20, max = 500, message = "상세 내용은 20자 이상 500자 이내로 작성해주세요.")
        String detail,

        @Size(max = 1000)
        String attachmentUrl,

        boolean includeAiEvidence
) {}
