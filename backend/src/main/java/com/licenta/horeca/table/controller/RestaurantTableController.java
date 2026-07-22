package com.licenta.horeca.table.controller;

import com.licenta.horeca.table.entity.RestaurantTable;
import com.licenta.horeca.table.service.RestaurantTableService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tables")
@CrossOrigin(origins = "http://localhost:5173")
public class RestaurantTableController {
    private final RestaurantTableService restaurantTableService;

    public RestaurantTableController(
            RestaurantTableService restaurantTableService) {
        this.restaurantTableService = restaurantTableService;
    }

    @GetMapping
    public List<RestaurantTable> getAllTables() {
        return restaurantTableService.getAllTables();
    }

    @GetMapping("/active")
    public List<RestaurantTable> getActiveTables() {
        return restaurantTableService.getActiveTables();
    }

    @GetMapping("/{tableId}")
    public RestaurantTable getTableById(@PathVariable Long tableId) {
        return restaurantTableService.getTableById(tableId);
    }
}