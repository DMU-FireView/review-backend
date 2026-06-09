package com.example.fireview.domain.admin.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminReviewStatusUpdateRequest(
        @NotNull String action,    // HIDE / NORMAL / HOLD / LABEL_CANDIDATE
        String comment
) {}
