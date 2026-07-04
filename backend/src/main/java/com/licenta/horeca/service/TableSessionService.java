package com.licenta.horeca.service;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableSessionService {

    private final TableSessionRepository tableSessionRepository;
    private final RestaurantTableRepository restaurantTableRepository;
    private final TrafficEventService trafficEventService;

    public TableSessionService(
            TableSessionRepository tableSessionRepository,
            RestaurantTableRepository restaurantTableRepository,
            TrafficEventService trafficEventService
    ) {
        this.tableSessionRepository = tableSessionRepository;
        this.restaurantTableRepository = restaurantTableRepository;
        this.trafficEventService = trafficEventService;
    }

    public List<TableSession> getActiveSessions() {
        return tableSessionRepository.findByActiveTrue();
    }

    @Transactional
    public TableSession createSessionForTable(Long tableId) {
        RestaurantTable restaurantTable = restaurantTableRepository
                .findById(tableId)
                .orElseThrow(() -> new RuntimeException("Masa nu exista."));

        TableSession tableSession = new TableSession();
        tableSession.setRestaurantTable(restaurantTable);
        tableSession.setSessionCode(generateSessionCode(restaurantTable));
        tableSession.setActive(true);
        tableSession.setStartedAt(LocalDateTime.now());

        TableSession savedSession = tableSessionRepository.save(tableSession);

        trafficEventService.saveEvent(TrafficEventType.ENTRY);

        return savedSession;
    }

    @Transactional
    public TableSession closeSession(Long sessionId) {
        TableSession tableSession = tableSessionRepository
                .findById(sessionId)
                .orElseThrow(() -> new RuntimeException("Sesiunea nu exista."));

        if (!tableSession.isActive()) {
            throw new RuntimeException("Sesiunea este deja inchisa.");
        }

        tableSession.setActive(false);
        tableSession.setEndedAt(LocalDateTime.now());

        TableSession savedSession = tableSessionRepository.save(tableSession);

        trafficEventService.saveEvent(TrafficEventType.EXIT);

        return savedSession;
    }

    private String generateSessionCode(RestaurantTable restaurantTable) {
        return "MASA-"
                + restaurantTable.getTableNumber()
                + "-"
                + System.currentTimeMillis();
    }
}