package com.licenta.horeca;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import com.licenta.horeca.service.TableSessionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TableSessionServiceTest {

    private static final String SESSION_CODE = "TEST123";
    private static final String TABLE_THREE_SESSION_PREFIX = "MASA-3-";

    @Test
    void getActiveSessionsShouldReturnActiveSessions() {
        TableSessionRepository tableSessionRepository = mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository = mock(RestaurantTableRepository.class);

        TableSession session = new TableSession();
        session.setSessionCode(SESSION_CODE);
        session.setActive(true);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(List.of(session));

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository,
                restaurantTableRepository
        );

        List<TableSession> result = tableSessionService.getActiveSessions();

        assertEquals(1, result.size());
        assertEquals(SESSION_CODE, result.get(0).getSessionCode());
        assertTrue(result.get(0).isActive());

        verify(tableSessionRepository).findByActiveTrue();
    }

    @Test
    void createSessionForTableShouldCreateActiveSession() {
        TableSessionRepository tableSessionRepository = mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository = mock(RestaurantTableRepository.class);

        RestaurantTable table = new RestaurantTable();
        table.setTableNumber(3);
        table.setCapacity(6);
        table.setActive(true);

        when(restaurantTableRepository.findById(3L))
                .thenReturn(Optional.of(table));

        when(tableSessionRepository.save(any(TableSession.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository,
                restaurantTableRepository
        );

        TableSession result = tableSessionService.createSessionForTable(3L);

        assertNotNull(result);
        assertTrue(result.isActive());
        assertNotNull(result.getSessionCode());
        assertTrue(result.getSessionCode().startsWith(TABLE_THREE_SESSION_PREFIX));
        assertEquals(table, result.getRestaurantTable());
        assertNotNull(result.getStartedAt());

        ArgumentCaptor<TableSession> captor = ArgumentCaptor.forClass(TableSession.class);
        verify(tableSessionRepository).save(captor.capture());

        TableSession savedSession = captor.getValue();

        assertTrue(savedSession.isActive());
        assertEquals(table, savedSession.getRestaurantTable());
        assertTrue(savedSession.getSessionCode().startsWith(TABLE_THREE_SESSION_PREFIX));
        assertNotNull(savedSession.getStartedAt());
    }

    @Test
    void createSessionForTableShouldThrowExceptionWhenTableDoesNotExist() {
        TableSessionRepository tableSessionRepository = mock(TableSessionRepository.class);
        RestaurantTableRepository restaurantTableRepository = mock(RestaurantTableRepository.class);

        when(restaurantTableRepository.findById(99L))
                .thenReturn(Optional.empty());

        TableSessionService tableSessionService = new TableSessionService(
                tableSessionRepository,
                restaurantTableRepository
        );

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> tableSessionService.createSessionForTable(99L)
        );

        assertEquals("Masa nu exista.", exception.getMessage());

        verify(tableSessionRepository, never()).save(any(TableSession.class));
    }
}