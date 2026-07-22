package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.RestaurantTableRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import com.licenta.horeca.service.TableSessionService;
import com.licenta.horeca.service.TrafficEventService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class TableSessionServiceTest {

    @Mock
    private TableSessionRepository
            tableSessionRepository;

    @Mock
    private RestaurantTableRepository
            restaurantTableRepository;

    @Mock
    private TrafficEventService
            trafficEventService;

    private TableSessionService
            tableSessionService;

    @BeforeEach
    void setUp() {
        tableSessionService =
                new TableSessionService(
                        tableSessionRepository,
                        restaurantTableRepository,
                        trafficEventService
                );
    }

    @Test
    void getActiveSessionsShouldReturnRepositoryResult() {
        TableSession firstSession =
                createSession(true);

        TableSession secondSession =
                createSession(true);

        List<TableSession> activeSessions =
                List.of(
                        firstSession,
                        secondSession
                );

        when(
                tableSessionRepository
                        .findByActiveTrue()
        ).thenReturn(activeSessions);

        List<TableSession> result =
                tableSessionService
                        .getActiveSessions();

        assertSame(activeSessions, result);
        assertEquals(2, result.size());
        assertSame(
                firstSession,
                result.get(0)
        );
        assertSame(
                secondSession,
                result.get(1)
        );

        verify(
                tableSessionRepository
        ).findByActiveTrue();

        verifyNoInteractions(
                restaurantTableRepository,
                trafficEventService
        );
    }

    @Test
    void createSessionForTableShouldThrowWhenTableDoesNotExist() {
        when(
                restaurantTableRepository
                        .findById(99L)
        ).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                tableSessionService
                                        .createSessionForTable(
                                                99L
                                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "Masa nu exista.",
                exception.getReason()
        );

        verify(
                restaurantTableRepository
        ).findById(99L);

        verify(
                tableSessionRepository,
                never()
        ).existsByRestaurantTable_IdAndActiveTrue(
                anyLong()
        );

        verify(
                tableSessionRepository,
                never()
        ).save(any(TableSession.class));

        verifyNoInteractions(
                trafficEventService
        );
    }

    @Test
    void createSessionForTableShouldRejectTableWithActiveSession() {
        RestaurantTable restaurantTable =
                new RestaurantTable(5, 4);

        when(
                restaurantTableRepository
                        .findById(5L)
        ).thenReturn(
                Optional.of(restaurantTable)
        );

        when(
                tableSessionRepository
                        .existsByRestaurantTable_IdAndActiveTrue(
                                5L
                        )
        ).thenReturn(true);

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                tableSessionService
                                        .createSessionForTable(
                                                5L
                                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Masa este deja deschisa.",
                exception.getReason()
        );

        verify(
                restaurantTableRepository
        ).findById(5L);

        verify(
                tableSessionRepository
        ).existsByRestaurantTable_IdAndActiveTrue(
                5L
        );

        verify(
                tableSessionRepository,
                never()
        ).save(any(TableSession.class));

        verifyNoInteractions(
                trafficEventService
        );
    }

    @Test
    void createSessionForTableShouldCreateSessionAndSaveEntryEvent() {
        RestaurantTable restaurantTable =
                new RestaurantTable(7, 4);

        TableSession repositoryResult =
                new TableSession();

        when(
                restaurantTableRepository
                        .findById(5L)
        ).thenReturn(
                Optional.of(restaurantTable)
        );

        when(
                tableSessionRepository
                        .existsByRestaurantTable_IdAndActiveTrue(
                                5L
                        )
        ).thenReturn(false);

        when(
                tableSessionRepository.save(
                        any(TableSession.class)
                )
        ).thenReturn(repositoryResult);

        long beforeCreation =
                System.currentTimeMillis();

        LocalDateTime beforeStartedAt =
                LocalDateTime.now();

        TableSession result =
                tableSessionService
                        .createSessionForTable(
                                5L
                        );

        LocalDateTime afterStartedAt =
                LocalDateTime.now();

        long afterCreation =
                System.currentTimeMillis();

        assertSame(repositoryResult, result);

        ArgumentCaptor<TableSession>
                sessionCaptor =
                ArgumentCaptor.forClass(
                        TableSession.class
                );

        verify(
                tableSessionRepository
        ).save(sessionCaptor.capture());

        TableSession sessionPassedToRepository =
                sessionCaptor.getValue();

        assertSame(
                restaurantTable,
                sessionPassedToRepository
                        .getRestaurantTable()
        );

        assertTrue(
                sessionPassedToRepository
                        .isActive()
        );

        assertNotNull(
                sessionPassedToRepository
                        .getStartedAt()
        );

        assertNull(
                sessionPassedToRepository
                        .getEndedAt()
        );

        assertFalse(
                sessionPassedToRepository
                        .getStartedAt()
                        .isBefore(
                                beforeStartedAt
                        )
        );

        assertFalse(
                sessionPassedToRepository
                        .getStartedAt()
                        .isAfter(
                                afterStartedAt
                        )
        );

        String sessionCode =
                sessionPassedToRepository
                        .getSessionCode();

        assertNotNull(sessionCode);

        assertTrue(
                sessionCode.matches(
                        "^MASA-7-\\d+$"
                ),
                "Codul trebuie sa respecte formatul "
                        + "MASA-numar-timestamp."
        );

        String timestampText =
                sessionCode.substring(
                        sessionCode
                                .lastIndexOf('-')
                                + 1
                );

        long timestamp =
                Long.parseLong(
                        timestampText
                );

        assertTrue(
                timestamp >= beforeCreation
                        && timestamp
                        <= afterCreation
        );

        verify(
                restaurantTableRepository
        ).findById(5L);

        verify(
                tableSessionRepository
        ).existsByRestaurantTable_IdAndActiveTrue(
                5L
        );

        verify(
                trafficEventService
        ).saveEvent(
                TrafficEventType.ENTRY
        );

        InOrder operationOrder =
                inOrder(
                        tableSessionRepository,
                        trafficEventService
                );

        operationOrder.verify(
                tableSessionRepository
        ).save(any(TableSession.class));

        operationOrder.verify(
                trafficEventService
        ).saveEvent(
                TrafficEventType.ENTRY
        );
    }

    @Test
    void createSessionForTableShouldNotSaveEntryEventWhenSessionSaveFails() {
        RestaurantTable restaurantTable =
                new RestaurantTable(2, 2);

        when(
                restaurantTableRepository
                        .findById(2L)
        ).thenReturn(
                Optional.of(restaurantTable)
        );

        when(
                tableSessionRepository
                        .existsByRestaurantTable_IdAndActiveTrue(
                                2L
                        )
        ).thenReturn(false);

        when(
                tableSessionRepository.save(
                        any(TableSession.class)
                )
        ).thenThrow(
                new RuntimeException(
                        "Eroare la salvarea sesiunii."
                )
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                tableSessionService
                                        .createSessionForTable(
                                                2L
                                        )
                );

        assertEquals(
                "Eroare la salvarea sesiunii.",
                exception.getMessage()
        );

        verify(
                restaurantTableRepository
        ).findById(2L);

        verify(
                tableSessionRepository
        ).existsByRestaurantTable_IdAndActiveTrue(
                2L
        );

        verify(
                tableSessionRepository
        ).save(any(TableSession.class));

        verifyNoInteractions(
                trafficEventService
        );
    }

    @Test
    void closeSessionShouldThrowWhenSessionDoesNotExist() {
        when(
                tableSessionRepository
                        .findById(99L)
        ).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                tableSessionService
                                        .closeSession(
                                                99L
                                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "Sesiunea nu exista.",
                exception.getReason()
        );

        verify(
                tableSessionRepository
        ).findById(99L);

        verify(
                tableSessionRepository,
                never()
        ).save(any(TableSession.class));

        verifyNoInteractions(
                trafficEventService
        );
    }

    @Test
    void closeSessionShouldRejectAlreadyClosedSession() {
        TableSession closedSession =
                createSession(false);

        LocalDateTime originalEndedAt =
                LocalDateTime.now()
                        .minusMinutes(10);

        closedSession.setEndedAt(
                originalEndedAt
        );

        when(
                tableSessionRepository
                        .findById(10L)
        ).thenReturn(
                Optional.of(closedSession)
        );

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () ->
                                tableSessionService
                                        .closeSession(
                                                10L
                                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Masa este deja inchisa.",
                exception.getReason()
        );

        assertFalse(
                closedSession.isActive()
        );

        assertEquals(
                originalEndedAt,
                closedSession.getEndedAt()
        );

        verify(
                tableSessionRepository
        ).findById(10L);

        verify(
                tableSessionRepository,
                never()
        ).save(any(TableSession.class));

        verifyNoInteractions(
                trafficEventService
        );
    }

    @Test
    void closeSessionShouldCloseSessionAndSaveExitEvent() {
        TableSession activeSession =
                createSession(true);

        assertNull(
                activeSession.getEndedAt()
        );

        when(
                tableSessionRepository
                        .findById(10L)
        ).thenReturn(
                Optional.of(activeSession)
        );

        when(
                tableSessionRepository.save(
                        activeSession
                )
        ).thenReturn(activeSession);

        LocalDateTime beforeClosing =
                LocalDateTime.now();

        TableSession result =
                tableSessionService
                        .closeSession(10L);

        LocalDateTime afterClosing =
                LocalDateTime.now();

        assertSame(activeSession, result);
        assertFalse(result.isActive());
        assertNotNull(result.getEndedAt());

        assertFalse(
                result.getEndedAt()
                        .isBefore(
                                beforeClosing
                        )
        );

        assertFalse(
                result.getEndedAt()
                        .isAfter(
                                afterClosing
                        )
        );

        verify(
                tableSessionRepository
        ).findById(10L);

        verify(
                tableSessionRepository
        ).save(activeSession);

        verify(
                trafficEventService
        ).saveEvent(
                TrafficEventType.EXIT
        );

        InOrder operationOrder =
                inOrder(
                        tableSessionRepository,
                        trafficEventService
                );

        operationOrder.verify(
                tableSessionRepository
        ).save(activeSession);

        operationOrder.verify(
                trafficEventService
        ).saveEvent(
                TrafficEventType.EXIT
        );
    }

    @Test
    void closeSessionShouldNotSaveExitEventWhenSessionSaveFails() {
        TableSession activeSession =
                createSession(true);

        when(
                tableSessionRepository
                        .findById(10L)
        ).thenReturn(
                Optional.of(activeSession)
        );

        when(
                tableSessionRepository.save(
                        activeSession
                )
        ).thenThrow(
                new RuntimeException(
                        "Eroare la inchiderea sesiunii."
                )
        );

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () ->
                                tableSessionService
                                        .closeSession(
                                                10L
                                        )
                );

        assertEquals(
                "Eroare la inchiderea sesiunii.",
                exception.getMessage()
        );

        verify(
                tableSessionRepository
        ).findById(10L);

        verify(
                tableSessionRepository
        ).save(activeSession);

        verifyNoInteractions(
                trafficEventService
        );
    }

    private TableSession createSession(
            boolean active
    ) {
        RestaurantTable restaurantTable =
                new RestaurantTable(1, 4);

        TableSession tableSession =
                new TableSession();

        tableSession.setRestaurantTable(
                restaurantTable
        );

        tableSession.setSessionCode(
                "MASA-1-12345"
        );

        tableSession.setStartedAt(
                LocalDateTime.now()
                        .minusMinutes(30)
        );

        tableSession.setActive(active);

        return tableSession;
    }
}