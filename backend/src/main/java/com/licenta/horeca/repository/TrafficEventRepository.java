package com.licenta.horeca.repository;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrafficEventRepository extends JpaRepository<TrafficEvent, Long> {

    long countByType(TrafficEventType type);
}
