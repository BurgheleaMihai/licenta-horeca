package com.licenta.horeca.service;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TableSessionService {

    private final TableSessionRepository tableSessionRepository;
    private final RestaurantTableRepository restaurantTableRepository;

    public TableSessionService(
            TableSessionRepository tableSessionRepository,
            RestaurantTableRepository restaurantTableRepository
    ) {
        this.tableSessionRepository = tableSessionRepository;
        this.restaurantTableRepository = restaurantTableRepository;
    }

    public List<TableSession> getActiveSessions() {
        return tableSessionRepository.findByActiveTrue();
    }

    public TableSession createSessionForTable(Long tableId) {
        RestaurantTable restaurantTable = restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Masa nu exista."));

        TableSession tableSession = new TableSession();
        tableSession.setRestaurantTable(restaurantTable);
        tableSession.setSessionCode(generateSessionCode(restaurantTable));
        tableSession.setActive(true);
        tableSession.setStartedAt(LocalDateTime.now());

        return tableSessionRepository.save(tableSession);
    }

    private String generateSessionCode(RestaurantTable restaurantTable) {
        return "MASA-" + restaurantTable.getTableNumber() + "-" + System.currentTimeMillis();
    }
}