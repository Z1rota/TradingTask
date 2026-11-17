package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.trade.TradeResponse;
import dev.zirota.tradingtask.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public Page<TradeResponse> findAll(long userId, String seccode, Long accountId, Pageable pageable) {
        return tradeRepository.findRegistry(userId, seccode == null ? null : seccode.toUpperCase(),
                accountId, registryPageable(pageable)).map(trade -> TradeResponse.from(trade, userId));
    }

    private Pageable registryPageable(Pageable pageable) {
        List<Sort.Order> orders = pageable.getSort().stream().map(order -> {
            String property = switch (order.getProperty()) {
                case "id", "executedAt", "price", "quantity" -> order.getProperty();
                case "seccode" -> "instrument.seccode";
                default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Unsupported trade sort field: " + order.getProperty());
            };
            return order.withProperty(property);
        }).toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(orders));
    }
}
