package com.example.fireview.domain.tossplace.api.dto.response;

import com.example.fireview.domain.tossplace.domain.enums.AclResult;

public record TossPlaceAclResponse(AclResult result) {

    public static TossPlaceAclResponse allow() {
        return new TossPlaceAclResponse(AclResult.ALLOW);
    }

    public static TossPlaceAclResponse deny() {
        return new TossPlaceAclResponse(AclResult.DENY);
    }
}
