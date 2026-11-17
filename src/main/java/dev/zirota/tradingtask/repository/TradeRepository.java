package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Trade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeRepository extends JpaRepository<Trade, Long> {
}
