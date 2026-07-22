package com.licenta.horeca.table.service;

import com.licenta.horeca.table.entity.RestaurantTable;
import com.licenta.horeca.table.entity.TableSession;
import com.licenta.horeca.table.repository.RestaurantTableRepository;
import com.licenta.horeca.table.repository.TableSessionRepository;
import com.licenta.horeca.traffic.enums.TrafficEventType;
import com.licenta.horeca.traffic.service.TrafficEventService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableSessionService {

    private final TableSessionRepository
            tableSessionRepository;

    private final RestaurantTableRepository
            restaurantTableRepository;

    private final TrafficEventService
            trafficEventService;

    public TableSessionService(
            TableSessionRepository tableSessionRepository,
            RestaurantTableRepository restaurantTableRepository,
            TrafficEventService trafficEventService
    ) {
        this.tableSessionRepository =
                tableSessionRepository;

        this.restaurantTableRepository =
                restaurantTableRepository;

        this.trafficEventService =
                trafficEventService;
    }

    @Transactional(readOnly = true)
    public List<TableSession> getActiveSessions() {
        return tableSessionRepository
                .findByActiveTrue();
    }

    @Transactional
    public TableSession createSessionForTable(
            Long tableId
    ) {
        RestaurantTable restaurantTable =
                restaurantTableRepository
                        .findById(tableId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Masa nu exista."
                                        )
                        );

        boolean alreadyHasActiveSession =
                tableSessionRepository
                        .existsByRestaurantTable_IdAndActiveTrue(
                                tableId
                        );

        if (alreadyHasActiveSession) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Masa este deja deschisa."
            );
        }

        TableSession tableSession =
                new TableSession();

        tableSession.setRestaurantTable(
                restaurantTable
        );

        tableSession.setSessionCode(
                generateSessionCode(
                        restaurantTable
                )
        );

        tableSession.setStartedAt(
                LocalDateTime.now()
        );

        tableSession.setEndedAt(null);
        tableSession.setActive(true);

        TableSession savedSession =
                tableSessionRepository.save(
                        tableSession
                );

        trafficEventService.saveEvent(
                TrafficEventType.ENTRY
        );

        return savedSession;
    }

    @Transactional
    public TableSession closeSession(
            Long sessionId
    ) {
        TableSession tableSession =
                tableSessionRepository
                        .findById(sessionId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Sesiunea nu exista."
                                        )
                        );

        if (!tableSession.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Masa este deja inchisa."
            );
        }

        tableSession.setActive(false);

        tableSession.setEndedAt(
                LocalDateTime.now()
        );

        TableSession savedSession =
                tableSessionRepository.save(
                        tableSession
                );

        trafficEventService.saveEvent(
                TrafficEventType.EXIT
        );

        return savedSession;
    }

    private String generateSessionCode(
            RestaurantTable restaurantTable
    ) {
        return "MASA-"
                + restaurantTable.getTableNumber()
                + "-"
                + System.currentTimeMillis();
    }
}