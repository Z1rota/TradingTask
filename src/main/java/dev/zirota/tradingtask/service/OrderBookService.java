package dev.zirota.tradingtask.service;

import dev.zirota.tradingtask.dto.order.OrderBookResponse;
import dev.zirota.tradingtask.entity.Instrument;
import dev.zirota.tradingtask.entity.OrderSide;
import dev.zirota.tradingtask.repository.InstrumentRepository;
import dev.zirota.tradingtask.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderBookService {

    private final InstrumentRepository instrumentRepository;
    private final OrderRepository orderRepository;

    @Transactional(readOnly = true)
    public OrderBookResponse findBySeccode(String seccode) {
        Instrument instrument = instrumentRepository.findBySeccode(seccode.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Instrument not found"));

        List<OrderBookResponse.Level> bids = new ArrayList<>();
        List<OrderBookResponse.Level> asks = new ArrayList<>();
        for (var level : orderRepository.findOrderBook(instrument.getId())) {
            var response = new OrderBookResponse.Level(level.getPrice(), level.getQuantity());
            if (level.getSide() == OrderSide.BUY) {
                bids.add(response);
            } else {
                asks.add(response);
            }
        }
        return new OrderBookResponse(bids, asks);
    }
}
