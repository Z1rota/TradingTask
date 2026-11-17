package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Instrument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    boolean existsBySeccode(String seccode);

    Optional<Instrument> findBySeccode(String seccode);
}
