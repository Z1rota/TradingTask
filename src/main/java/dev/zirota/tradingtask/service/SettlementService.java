package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.entity.Account;
import dev.zirota.tradingtask.entity.Order;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.repository.AccountRepository;
import dev.zirota.tradingtask.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class SettlementService {

    private final AccountRepository accountRepository;
    private final PositionRepository positionRepository;

    public void settle(Order buy, Order sell, BigDecimal price, long lots) {
        if (buy.getSide() != OrderSide.BUY || sell.getSide() != OrderSide.SELL) {
            throw new IllegalArgumentException("Expected BUY and SELL orders");
        }

        if (!buy.getInstrument().getId().equals(sell.getInstrument().getId())) {
            throw new IllegalArgumentException("Order instruments must match");
        }

        if (buy.getUser().getId().equals(sell.getUser().getId())) {
            throw new IllegalArgumentException("Self-trading is prohibited");
        }

        if (lots <= 0 || lots > buy.getRemainingQuantity() || lots > sell.getRemainingQuantity()) {
            throw new IllegalArgumentException("Invalid execution quantity");
        }

        if (price.compareTo(buy.getLimitPrice()) > 0 || price.compareTo(sell.getLimitPrice()) < 0) {
            throw new IllegalArgumentException("Execution price violates order limits");
        }

        long units = Math.multiplyExact(lots, buy.getInstrument().getLotSize());
        BigDecimal payment = price.multiply(BigDecimal.valueOf(units));
        BigDecimal released = buy.getLimitPrice().multiply(BigDecimal.valueOf(units));
        if (accountRepository.settleBuy(buy.getAccount().getId(), payment, released) != 1
                || positionRepository.settleSell(sell.getAccount().getId(), sell.getInstrument().getId(), units) != 1) {
            throw new IllegalStateException("Reserved assets are inconsistent");
        }
        if (accountRepository.addBalance(sell.getAccount().getId(), payment, Account.MAX_BALANCE) != 1
                || positionRepository.addQuantity(buy.getAccount().getId(), buy.getInstrument().getId(), units) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Settlement capacity exceeded");
        }
    }

    public void releaseRemainder(Order order) {
        long units = Math.multiplyExact(order.getRemainingQuantity(), order.getInstrument().getLotSize());
        int changed;
        if (order.getSide() == OrderSide.BUY) {
            BigDecimal amount = order.getLimitPrice().multiply(BigDecimal.valueOf(units));
            changed = accountRepository.releaseBalance(order.getAccount().getId(), amount);
        } else {
            changed = positionRepository.releaseQuantity(order.getAccount().getId(), order.getInstrument().getId(), units);
        }
        if (changed != 1) {
            throw new IllegalStateException("Reserved assets are inconsistent");
        }
    }
}
