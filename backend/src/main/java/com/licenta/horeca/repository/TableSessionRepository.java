package com.licenta.horeca.repository;

import com.licenta.horeca.entity.TableSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TableSessionRepository extends JpaRepository<TableSession, Long> {

    Optional<TableSession> findBySessionCode(String sessionCode);

    Optional<TableSession> findBySessionCodeAndActiveTrue(String sessionCode);

    List<TableSession> findByActiveTrue();
}