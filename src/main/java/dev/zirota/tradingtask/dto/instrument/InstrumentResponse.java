package dev.zirota.tradingtask.dto.instrument;

import dev.zirota.tradingtask.entity.Instrument;

import java.math.BigDecimal;

public record InstrumentResponse(
        String seccode, String type, int lotSize, BigDecimal minPriceStep, String quoteCurrency
) {

    public static InstrumentResponse from(Instrument instrument) {
        return new InstrumentResponse(instrument.getSeccode(), instrument.getType(), instrument.getLotSize(),
                instrument.getMinPriceStep(), instrument.getQuoteCurrency());
    }
}
