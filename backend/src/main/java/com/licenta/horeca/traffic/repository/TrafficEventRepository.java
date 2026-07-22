package com.licenta.horeca.traffic.repository;

import com.licenta.horeca.traffic.entity.TrafficEvent;
import com.licenta.horeca.traffic.enums.TrafficEventType;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficEventRepository
        extends JpaRepository<TrafficEvent, Long> {
    long countByType(TrafficEventType type);

    long countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            TrafficEventType type, LocalDateTime startDate, LocalDateTime endDate);
}