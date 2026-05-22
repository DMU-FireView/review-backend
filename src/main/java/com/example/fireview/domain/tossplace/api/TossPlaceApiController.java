package com.example.fireview.domain.tossplace.api;

import com.example.fireview.domain.tossplace.api.dto.request.TossPlaceAclRequest;
import com.example.fireview.domain.tossplace.api.dto.response.TossPlaceAclResponse;
import com.example.fireview.domain.tossplace.application.usecase.VerifyTossPlaceAclUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tossplace")
@RequiredArgsConstructor
public class TossPlaceApiController {

    private final VerifyTossPlaceAclUseCase verifyTossPlaceAclUseCase;

    @PostMapping("/acl")
    public ResponseEntity<TossPlaceAclResponse> checkAcl(@Valid @RequestBody TossPlaceAclRequest request) {
        TossPlaceAclResponse response = verifyTossPlaceAclUseCase.verify(request);
        return ResponseEntity.ok(response);
    }
}
