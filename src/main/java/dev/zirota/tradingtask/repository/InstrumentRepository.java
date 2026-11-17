package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Instrument;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {

    boolean existsBySeccode(String seccode);

    Optional<Instrument> findBySeccode(String seccode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select i from Instrument i where i.seccode = :seccode")
    Optional<Instrument> findBySeccodeForUpdate(String seccode);
}
