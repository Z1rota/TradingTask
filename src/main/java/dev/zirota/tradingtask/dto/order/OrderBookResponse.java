package dev.zirota.tradingtask.dto.order;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.List;

public record OrderBookResponse(List<Level> bids, List<Level> asks) {

    public record Level(BigDecimal price, BigInteger quantity) { }
}
