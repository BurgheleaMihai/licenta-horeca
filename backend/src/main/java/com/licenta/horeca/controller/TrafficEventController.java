package com.licenta.horeca.controller;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.service.TrafficEventService;
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
