package com.licenta.horeca.service;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

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

    @Transactional(readOnly = true)
    public TableSession validateSessionCode(String sessionCode) {

        if (sessionCode == null || sessionCode.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Codul sesiunii este obligatoriu."
            );
        }

        TableSession tableSession = tableSessionRepository
                .findBySessionCode(sessionCode.trim())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Codul sesiunii nu exista."
                ));

        if (!tableSession.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.GONE,
                    "Sesiunea nu mai este activa."
            );
        }

        return tableSession;
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