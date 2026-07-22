package com.licenta.horeca.repository;

import com.licenta.horeca.entity.TableSession;
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