package com.licenta.horeca;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.service.RestaurantTableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RestaurantTableServiceTest {

    @Mock
    private RestaurantTableRepository restaurantTableRepository;

    @InjectMocks
    private RestaurantTableService restaurantTableService;

    @Test
    void getAllTablesShouldReturnAllTables() {
        RestaurantTable table1 = new RestaurantTable(1, 4);
        RestaurantTable table2 = new RestaurantTable(2, 2);

        when(restaurantTableRepository.findAll())
                .thenReturn(List.of(table1, table2));

        List<RestaurantTable> tables = restaurantTableService.getAllTables();

        assertEquals(2, tables.size());
        assertEquals(1, tables.get(0).getTableNumber());
        assertEquals(2, tables.get(1).getTableNumber());

        verify(restaurantTableRepository, times(1)).findAll();
    }

    @Test
    void getActiveTablesShouldReturnOnlyActiveTables() {
        RestaurantTable table1 = new RestaurantTable(1, 4);
        RestaurantTable table2 = new RestaurantTable(2, 2);

        when(restaurantTableRepository.findByActiveTrue())
                .thenReturn(List.of(table1, table2));

        List<RestaurantTable> tables = restaurantTableService.getActiveTables();

        assertEquals(2, tables.size());
        assertTrue(tables.stream().allMatch(RestaurantTable::isActive));

        verify(restaurantTableRepository, times(1)).findByActiveTrue();
    }

    @Test
    void getTableByIdShouldReturnTableWhenExists() {
        RestaurantTable table = new RestaurantTable(1, 4);

        when(restaurantTableRepository.findById(1L))
                .thenReturn(Optional.of(table));

        RestaurantTable result = restaurantTableService.getTableById(1L);

        assertEquals(1, result.getTableNumber());
        assertEquals(4, result.getCapacity());

        verify(restaurantTableRepository, times(1)).findById(1L);
    }

    @Test
    void getTableByIdShouldThrowExceptionWhenTableDoesNotExist() {
        when(restaurantTableRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> restaurantTableService.getTableById(99L)
        );

        assertTrue(exception.getMessage().contains("Masa nu exista"));

        verify(restaurantTableRepository, times(1)).findById(99L);
    }
}