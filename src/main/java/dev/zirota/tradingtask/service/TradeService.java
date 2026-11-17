package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.trade.TradeResponse;
import dev.zirota.tradingtask.repository.TradeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TradeService {

    private final TradeRepository tradeRepository;

    @Transactional(readOnly = true)
    public Page<TradeResponse> findAll(long userId, String seccode, Long accountId, Pageable pageable) {
        return tradeRepository.findRegistry(userId, seccode == null ? null : seccode.toUpperCase(),
                accountId, pageable).map(trade -> TradeResponse.from(trade, userId));
    }
}
