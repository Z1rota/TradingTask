package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.order.CreateOrderRequest;
import dev.zirota.tradingtask.dto.order.OrderResponse;
import dev.zirota.tradingtask.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt,
                                @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String key,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(Long.parseLong(jwt.getSubject()), key, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        orderService.cancel(Long.parseLong(jwt.getSubject()), id);
    }
}
