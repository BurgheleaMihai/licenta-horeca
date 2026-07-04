package com.licenta.horeca.repository;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;

public interface TrafficEventRepository
        extends JpaRepository<TrafficEvent, Long> {

    long countByType(TrafficEventType type);

    long countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            TrafficEventType type,
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}