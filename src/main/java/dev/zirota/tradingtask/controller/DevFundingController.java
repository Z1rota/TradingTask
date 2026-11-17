package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.portfolio.FundingRequest;
import dev.zirota.tradingtask.service.FundingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dev/funding")
@RequiredArgsConstructor
public class DevFundingController {

    private final FundingService fundingService;

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void fund(@AuthenticationPrincipal Jwt jwt,
                     @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String key,
                     @Valid @RequestBody FundingRequest request) {
        fundingService.fund(Long.parseLong(jwt.getSubject()), key, request);
    }
}
