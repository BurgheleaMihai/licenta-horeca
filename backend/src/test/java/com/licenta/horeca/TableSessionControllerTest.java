package com.licenta.horeca;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licenta.horeca.controller.TableSessionController;
import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.service.TableSessionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TableSessionController.class)
class TableSessionControllerTest {
    private static final String SESSION_CODE = "MASA-3-TEST";
    private static final String ACTIVE_SESSIONS_ENDPOINT =
            "/api/table-sessions/active";
    private static final String CREATE_SESSION_ENDPOINT =
            "/api/table-sessions/table/3";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TableSessionService tableSessionService;

    @Test
    void getActiveSessionsShouldReturnActiveSessions() throws Exception {
        RestaurantTable table = new RestaurantTable(3, 6);
        TableSession session = new TableSession(table, SESSION_CODE);
        session.setActive(true);

        when(tableSessionService.getActiveSessions()).thenReturn(List.of(session));

        mockMvc.perform(get(ACTIVE_SESSIONS_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].sessionCode").value(SESSION_CODE))
                .andExpect(jsonPath("$[0].active").value(true));
    }

    @Test
    void createSessionForTableShouldReturnCreatedSession() throws Exception {
        RestaurantTable table = new RestaurantTable(3, 6);
        TableSession session = new TableSession(table, SESSION_CODE);
        session.setActive(true);

        when(tableSessionService.createSessionForTable(3L)).thenReturn(session);

        mockMvc.perform(post(CREATE_SESSION_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sessionCode").value(SESSION_CODE))
                .andExpect(jsonPath("$.active").value(true));
    }
}
