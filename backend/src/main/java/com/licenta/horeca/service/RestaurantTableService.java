package com.licenta.horeca.service;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.repository.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RestaurantTableService {
    private final RestaurantTableRepository restaurantTableRepository;

    public RestaurantTableService(RestaurantTableRepository restaurantTableRepository) {
        this.restaurantTableRepository = restaurantTableRepository;
    }
    public List<RestaurantTable> getAllTables() {
        return restaurantTableRepository.findAll();
    }
    public List<RestaurantTable> getActiveTables() {
        return restaurantTableRepository.findByActiveTrue();
    }
    public RestaurantTable getTableById(Long tableId) {
        return restaurantTableRepository.findById(tableId)
                .orElseThrow(() -> new RuntimeException("Masa nu exista."));
    }
}
