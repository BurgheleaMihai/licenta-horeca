package com.licenta.horeca.service;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.TrafficEventRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TrafficEventService {

    private final TrafficEventRepository trafficEventRepository;

    public TrafficEventService(TrafficEventRepository trafficEventRepository) {
        this.trafficEventRepository = trafficEventRepository;
    }

    public TrafficEvent saveEvent(TrafficEventType type) {
        TrafficEvent event = new TrafficEvent(type);
        return trafficEventRepository.save(event);
    }

    public List<TrafficEvent> getAllEvents() {
        return trafficEventRepository.findAll();
    }

    public long getEntryCount() {
        return trafficEventRepository.countByType(TrafficEventType.ENTRY);
    }

    public long getExitCount() {
        return trafficEventRepository.countByType(TrafficEventType.EXIT);
    }

    public long getEstimatedOccupancy() {
        long occupancy = getEntryCount() - getExitCount();

        if (occupancy < 0) {
            return 0;
        }

        return occupancy;
    }
}
