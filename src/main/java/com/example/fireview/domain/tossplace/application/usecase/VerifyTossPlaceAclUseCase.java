package com.example.fireview.domain.tossplace.application.usecase;

import com.example.fireview.domain.tossplace.api.dto.request.TossPlaceAclRequest;
import com.example.fireview.domain.tossplace.api.dto.response.TossPlaceAclResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class VerifyTossPlaceAclUseCase {

    private final String configuredAppId;
    private final String configuredSerialNumber;

    public VerifyTossPlaceAclUseCase(
            @Value("${tossplace.app-id}") String configuredAppId,
            @Value("${tossplace.serial-number}") String configuredSerialNumber) {
        this.configuredAppId = configuredAppId;
        this.configuredSerialNumber = configuredSerialNumber;
    }

    public TossPlaceAclResponse verify(TossPlaceAclRequest request) {
        if (!configuredAppId.equals(request.appId())) {
            log.warn("TossPlace ACL denied: unknown appId={}", request.appId());
            return TossPlaceAclResponse.deny();
        }

        if (!configuredSerialNumber.equals(request.serialNumber())) {
            log.warn("TossPlace ACL denied: unregistered serialNumber={}", request.serialNumber());
            return TossPlaceAclResponse.deny();
        }

        log.info("TossPlace ACL allowed: appId={}, serialNumber={}", request.appId(), request.serialNumber());
        return TossPlaceAclResponse.allow();
    }
}
