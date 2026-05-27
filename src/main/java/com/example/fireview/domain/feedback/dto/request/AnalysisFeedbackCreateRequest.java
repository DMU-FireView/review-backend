package com.example.fireview.domain.feedback.dto.request;

import com.example.fireview.domain.feedback.entity.AnalysisFeedbackType;
import com.example.fireview.domain.feedback.entity.UserJudgment;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record AnalysisFeedbackCreateRequest(

        @NotNull(message = "피드백 유형을 선택해주세요.")
        AnalysisFeedbackType feedbackType,

        UserJudgment userJudgment,

        List<String> relatedSignals,

        @Size(max = 2000, message = "상세 내용은 2000자 이내로 작성해주세요.")
        String detail,

        @Size(max = 1000)
        String attachmentUrl,

        @Size(max = 200)
        String replyEmail
) {}
