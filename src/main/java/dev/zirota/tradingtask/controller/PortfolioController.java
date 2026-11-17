package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.portfolio.AccountResponse;
import dev.zirota.tradingtask.dto.portfolio.CreateAccountRequest;
import dev.zirota.tradingtask.dto.portfolio.PositionResponse;
import dev.zirota.tradingtask.service.PortfolioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@AuthenticationPrincipal Jwt jwt,
                                         @Valid @RequestBody CreateAccountRequest request) {
        return portfolioService.createAccount(Long.parseLong(jwt.getSubject()), request);
    }

    @GetMapping("/accounts")
    public List<AccountResponse> findAccounts(@AuthenticationPrincipal Jwt jwt) {
        return portfolioService.findAccounts(Long.parseLong(jwt.getSubject()));
    }

    @GetMapping("/positions")
    public List<PositionResponse> findPositions(@AuthenticationPrincipal Jwt jwt) {
        return portfolioService.findPositions(Long.parseLong(jwt.getSubject()));
    }
}
