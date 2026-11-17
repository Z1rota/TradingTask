package dev.zirota.tradingtask.controller;

import dev.zirota.tradingtask.dto.order.CreateOrderRequest;
import dev.zirota.tradingtask.dto.order.OrderResponse;
import dev.zirota.tradingtask.dto.order.ReplaceOrderRequest;
import dev.zirota.tradingtask.entity.OrderStatus;
import dev.zirota.tradingtask.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping
    public Page<OrderResponse> findAll(@AuthenticationPrincipal Jwt jwt,
                                       @RequestParam(required = false) String seccode,
                                       @RequestParam(required = false) Long accountId,
                                       @RequestParam(required = false) OrderStatus status,
                                       @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.DESC)
                                       Pageable pageable) {
        return orderService.findAll(Long.parseLong(jwt.getSubject()), seccode, accountId, status, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse create(@AuthenticationPrincipal Jwt jwt,
                                @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String key,
                                @Valid @RequestBody CreateOrderRequest request) {
        return orderService.create(Long.parseLong(jwt.getSubject()), key, request);
    }

    @PostMapping("/{id}/replace")
    @ResponseStatus(HttpStatus.CREATED)
    public OrderResponse replace(@AuthenticationPrincipal Jwt jwt, @PathVariable long id,
                                 @RequestHeader("Idempotency-Key") @NotBlank @Size(max = 128) String key,
                                 @Valid @RequestBody ReplaceOrderRequest request) {
        return orderService.replace(Long.parseLong(jwt.getSubject()), id, key, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable long id) {
        orderService.cancel(Long.parseLong(jwt.getSubject()), id);
    }
}
