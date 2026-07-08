package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import com.licenta.horeca.service.TableSessionService;
import com.licenta.horeca.service.TrafficEventService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TableSessionServiceTest {
    private static final String SESSION_CODE = "TEST123";
    private static final String TABLE_THREE_SESSION_PREFIX = "MASA-3-";

    @Test
    void getActiveSessionsShouldReturnActiveSessions() {
        TableSessionRepository tableSessionRepository =
                mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository =
                mock(RestaurantTableRepository.class);
        TrafficEventService trafficEventService = mock(TrafficEventService.class);

        TableSession session = new TableSession();
        session.setSessionCode(SESSION_CODE);
        session.setActive(true);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(List.of(session));

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository, restaurantTableRepository, trafficEventService);

        List<TableSession> result = tableSessionService.getActiveSessions();

        assertEquals(1, result.size());
        assertEquals(SESSION_CODE, result.get(0).getSessionCode());
        assertTrue(result.get(0).isActive());

        verify(tableSessionRepository).findByActiveTrue();
    }

    @Test
    void createSessionForTableShouldCreateActiveSession() {
        TableSessionRepository tableSessionRepository =
                mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository =
                mock(RestaurantTableRepository.class);
        TrafficEventService trafficEventService = mock(TrafficEventService.class);

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(3);
        table.setCapacity(6);
        table.setActive(true);

        when(restaurantTableRepository.findById(3L)).thenReturn(Optional.of(table));
        when(tableSessionRepository.save(any(TableSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository, restaurantTableRepository, trafficEventService);

        TableSession result = tableSessionService.createSessionForTable(3L);

        assertNotNull(result);
        assertTrue(result.isActive());
        assertNotNull(result.getSessionCode());
        assertTrue(result.getSessionCode().startsWith(TABLE_THREE_SESSION_PREFIX));
        assertEquals(table, result.getRestaurantTable());
        assertNotNull(result.getStartedAt());

        ArgumentCaptor<TableSession> captor =
                ArgumentCaptor.forClass(TableSession.class);

        verify(tableSessionRepository).save(captor.capture());

        TableSession savedSession = captor.getValue();

        assertTrue(savedSession.isActive());
        assertEquals(table, savedSession.getRestaurantTable());
        assertTrue(
                savedSession.getSessionCode().startsWith(TABLE_THREE_SESSION_PREFIX));
        assertNotNull(savedSession.getStartedAt());
    }

    @Test
    void createSessionForTableShouldThrowExceptionWhenTableDoesNotExist() {
        TableSessionRepository tableSessionRepository =
                mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository =
                mock(RestaurantTableRepository.class);
        TrafficEventService trafficEventService = mock(TrafficEventService.class);

        when(restaurantTableRepository.findById(99L)).thenReturn(Optional.empty());

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository, restaurantTableRepository, trafficEventService);

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> tableSessionService.createSessionForTable(99L));

        assertEquals("Masa nu exista.", exception.getMessage());

        verify(tableSessionRepository, never()).save(any(TableSession.class));
    }
}