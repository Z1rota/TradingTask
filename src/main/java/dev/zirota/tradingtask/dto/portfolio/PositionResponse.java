package dev.zirota.tradingtask.dto.portfolio;

import dev.zirota.tradingtask.entity.Position;

public record PositionResponse(
        Long id, Long accountId, String seccode, long quantity, long reservedQuantity, long availableQuantity
) {

    public static PositionResponse from(Position position) {
        return new PositionResponse(position.getId(), position.getAccount().getId(),
                position.getInstrument().getSeccode(), position.getQuantity(), position.getReservedQuantity(),
                position.getQuantity() - position.getReservedQuantity());
    }
}
