package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.trade.TradeResponse;
import dev.zirota.tradingtask.service.TradeService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/trades")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @GetMapping
    public Page<TradeResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) String seccode,
                                       @RequestParam(required = false) Long accountId,
                                       @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return tradeService.findAll(Long.parseLong(jwt.getSubject()), seccode, accountId, pageable);
    }
}
