package com.licenta.horeca.repository;

import com.licenta.horeca.entity.RestaurantTable;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RestaurantTableRepository
        extends JpaRepository<RestaurantTable, Long> {
    Optional<RestaurantTable> findByTableNumber(Integer tableNumber);
    List<RestaurantTable> findByActiveTrue();
}