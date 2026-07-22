package com.licenta.horeca.table.repository;

import com.licenta.horeca.table.entity.TableSession;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TableSessionRepository
        extends JpaRepository<TableSession, Long> {

    Optional<TableSession>
    findBySessionCodeAndActiveTrue(
            String sessionCode
    );

    List<TableSession> findByActiveTrue();

    boolean
    existsByRestaurantTable_IdAndActiveTrue(
            Long tableId
    );
}