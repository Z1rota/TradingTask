package dev.zirota.tradingtask.repository;

import dev.zirota.tradingtask.entity.Position;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PositionRepository extends JpaRepository<Position, Long> {

    @EntityGraph(attributePaths = {"instrument", "account"})
    List<Position> findAllByAccountUserId(long userId, Sort sort);
}
