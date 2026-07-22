package com.licenta.horeca.traffic.service;

import com.licenta.horeca.traffic.entity.TrafficEvent;
import com.licenta.horeca.traffic.enums.TrafficEventType;
import com.licenta.horeca.traffic.repository.TrafficEventRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrafficEventService {

    private final TrafficEventRepository trafficEventRepository;

    public TrafficEventService(TrafficEventRepository trafficEventRepository){
        this.trafficEventRepository = trafficEventRepository;
    }

    public TrafficEvent saveEvent(TrafficEventType type){
        TrafficEvent event = new TrafficEvent(type);
        return trafficEventRepository.save(event);
    }

    public List<TrafficEvent> getAllEvents() {
        return trafficEventRepository.findAll();
    }

    public long getEntryCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = LocalDate.now().plusDays(1).atStartOfDay();

        return trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                TrafficEventType.ENTRY, startOfDay, startOfNextDay);
    }

    public long getExitCount() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime startOfNextDay = LocalDate.now().plusDays(1).atStartOfDay();

        return trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                        TrafficEventType.EXIT, startOfDay, startOfNextDay);
    }

    public long getEstimatedOccupancy() {
        long occupancy = getEntryCount() - getExitCount();
        if (occupancy < 0) {
            return 0;
        }
        return occupancy;
    }
}