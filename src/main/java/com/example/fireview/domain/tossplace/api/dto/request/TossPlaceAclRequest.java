package com.example.fireview.domain.tossplace.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TossPlaceAclRequest(
        @NotBlank String appId,
        @NotBlank String serialNumber,
        String merchantId
) {}
