package com.licenta.horeca.traffic.controller;

import com.licenta.horeca.traffic.entity.TrafficEvent;
import com.licenta.horeca.traffic.enums.TrafficEventType;
import com.licenta.horeca.traffic.service.TrafficEventService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/traffic")
@CrossOrigin(origins = "http://localhost:5173")
public class TrafficEventController {
    private final TrafficEventService trafficEventService;

    public TrafficEventController(TrafficEventService trafficEventService) {
        this.trafficEventService = trafficEventService;
    }

    @PostMapping("/entry")
    public TrafficEvent registerEntry() {
        return trafficEventService.saveEvent(TrafficEventType.ENTRY);
    }

    @PostMapping("/exit")
    public TrafficEvent registerExit() {
        return trafficEventService.saveEvent(TrafficEventType.EXIT);
    }

    @GetMapping
    public List<TrafficEvent> getAllEvents() {
        return trafficEventService.getAllEvents();
    }

    @GetMapping("/summary")
    public Map<String, Long> getSummary() {
        return Map.of("entries", trafficEventService.getEntryCount(), "exits",
                trafficEventService.getExitCount(), "estimatedOccupancy",
                trafficEventService.getEstimatedOccupancy());
    }
}
