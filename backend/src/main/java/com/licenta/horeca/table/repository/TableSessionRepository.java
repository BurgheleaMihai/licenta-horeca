package com.licenta.horeca.table.repository;

import com.licenta.horeca.table.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findBySessionCodeAndActiveTrue(String sessionCode);

    List<TableSession> findByActiveTrue();

    boolean existsByRestaurantTable_IdAndActiveTrue(Long tableId);
}