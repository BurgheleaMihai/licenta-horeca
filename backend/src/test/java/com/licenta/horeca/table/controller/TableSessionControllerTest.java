package com.licenta.horeca.table.controller;

import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.table.entity.RestaurantTable;
import com.licenta.horeca.table.entity.TableSession;
import com.licenta.horeca.table.service.TableSessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TableSessionController.class)
@Import(SecurityConfig.class)
@WithMockUser(
        username = "waiter@test.com",
        roles = "WAITER"
)
class TableSessionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TableSessionService
            tableSessionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService
            customUserDetailsService;

    @Test
    void getActiveSessionsShouldReturnActiveSessions()
            throws Exception {

        TableSession firstSession =
                createSession(
                        10L,
                        1,
                        "MASA-1-11111",
                        true
                );

        TableSession secondSession =
                createSession(
                        11L,
                        2,
                        "MASA-2-22222",
                        true
                );

        when(
                tableSessionService
                        .getActiveSessions()
        ).thenReturn(
                List.of(
                        firstSession,
                        secondSession
                )
        );

        mockMvc.perform(
                        get(
                                "/api/table-sessions/active"
                        )
                                .accept(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        content()
                                .contentTypeCompatibleWith(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(10)
                )
                .andExpect(
                        jsonPath(
                                "$[0].sessionCode"
                        ).value(
                                "MASA-1-11111"
                        )
                )
                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                )
                .andExpect(
                        jsonPath(
                                "$[0].restaurantTable.tableNumber"
                        ).value(1)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(11)
                )
                .andExpect(
                        jsonPath(
                                "$[1].sessionCode"
                        ).value(
                                "MASA-2-22222"
                        )
                );

        verify(
                tableSessionService
        ).getActiveSessions();
    }

    @Test
    void getActiveSessionsShouldReturnEmptyArray()
            throws Exception {

        when(
                tableSessionService
                        .getActiveSessions()
        ).thenReturn(List.of());

        mockMvc.perform(
                        get(
                                "/api/table-sessions/active"
                        )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );

        verify(
                tableSessionService
        ).getActiveSessions();
    }

    @Test
    void createSessionForTableShouldReturnCreatedSession()
            throws Exception {

        TableSession createdSession =
                createSession(
                        20L,
                        5,
                        "MASA-5-55555",
                        true
                );

        when(
                tableSessionService
                        .createSessionForTable(
                                5L
                        )
        ).thenReturn(createdSession);

        mockMvc.perform(
                        post(
                                "/api/table-sessions/table/{tableId}",
                                5L
                        )
                                .accept(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(20)
                )
                .andExpect(
                        jsonPath(
                                "$.sessionCode"
                        ).value(
                                "MASA-5-55555"
                        )
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.startedAt")
                                .exists()
                )
                .andExpect(
                        jsonPath(
                                "$.restaurantTable.tableNumber"
                        ).value(5)
                );

        verify(
                tableSessionService
        ).createSessionForTable(5L);
    }

    @Test
    void createSessionForTableShouldRejectInvalidTableId()
            throws Exception {

        mockMvc.perform(
                        post(
                                "/api/table-sessions/table/{tableId}",
                                "abc"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verify(
                tableSessionService,
                never()
        ).createSessionForTable(
                anyLong()
        );
    }

    @Test
    void closeSessionShouldReturnClosedSession()
            throws Exception {

        TableSession closedSession =
                createSession(
                        30L,
                        6,
                        "MASA-6-66666",
                        false
                );

        closedSession.setEndedAt(
                LocalDateTime.now()
        );

        when(
                tableSessionService
                        .closeSession(30L)
        ).thenReturn(closedSession);

        mockMvc.perform(
                        put(
                                "/api/table-sessions/{sessionId}/close",
                                30L
                        )
                                .accept(
                                        MediaType
                                                .APPLICATION_JSON
                                )
                )
                .andExpect(
                        status().isOk()
                )
                .andExpect(
                        jsonPath("$.id")
                                .value(30)
                )
                .andExpect(
                        jsonPath(
                                "$.sessionCode"
                        ).value(
                                "MASA-6-66666"
                        )
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.endedAt")
                                .exists()
                );

        verify(
                tableSessionService
        ).closeSession(30L);
    }

    @Test
    void closeSessionShouldRejectInvalidSessionId()
            throws Exception {

        mockMvc.perform(
                        put(
                                "/api/table-sessions/{sessionId}/close",
                                "invalid"
                        )
                )
                .andExpect(
                        status().isBadRequest()
                );

        verify(
                tableSessionService,
                never()
        ).closeSession(anyLong());
    }

    private TableSession createSession(
            Long sessionId,
            int tableNumber,
            String sessionCode,
            boolean active
    ) {
        RestaurantTable restaurantTable =
                new RestaurantTable(
                        tableNumber,
                        4
                );

        ReflectionTestUtils.setField(
                restaurantTable,
                "id",
                (long) tableNumber
        );

        TableSession session =
                new TableSession();

        ReflectionTestUtils.setField(
                session,
                "id",
                sessionId
        );

        session.setRestaurantTable(
                restaurantTable
        );

        session.setSessionCode(
                sessionCode
        );

        session.setStartedAt(
                LocalDateTime.now()
                        .minusMinutes(10)
        );

        session.setActive(active);

        return session;
    }
}
