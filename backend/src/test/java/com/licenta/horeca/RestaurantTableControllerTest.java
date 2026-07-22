package com.licenta.horeca;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.controller.RestaurantTableController;
import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.service.RestaurantTableService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RestaurantTableController.class)
@Import(SecurityConfig.class)
@WithMockUser(
        username = "waiter@test.com",
        roles = "WAITER"
)
class RestaurantTableControllerTest {

    private static final String TABLES_ENDPOINT =
            "/api/tables";

    private static final String ACTIVE_TABLES_ENDPOINT =
            "/api/tables/active";

    private static final String TABLE_BY_ID_ENDPOINT =
            "/api/tables/1";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RestaurantTableService restaurantTableService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllTablesShouldReturnAllTables()
            throws Exception {

        RestaurantTable table1 =
                new RestaurantTable(1, 4);

        RestaurantTable table2 =
                new RestaurantTable(2, 2);

        when(restaurantTableService.getAllTables())
                .thenReturn(
                        List.of(
                                table1,
                                table2
                        )
                );

        mockMvc
                .perform(
                        get(TABLES_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].tableNumber")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].capacity")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$[1].tableNumber")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[1].capacity")
                                .value(2)
                );
    }

    @Test
    void getActiveTablesShouldReturnActiveTables()
            throws Exception {

        RestaurantTable table =
                new RestaurantTable(1, 4);

        table.setActive(true);

        when(restaurantTableService.getActiveTables())
                .thenReturn(
                        List.of(table)
                );

        mockMvc
                .perform(
                        get(ACTIVE_TABLES_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].tableNumber")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                );
    }

    @Test
    void getTableByIdShouldReturnTable()
            throws Exception {

        RestaurantTable table =
                new RestaurantTable(1, 4);

        table.setActive(true);

        when(restaurantTableService.getTableById(1L))
                .thenReturn(table);

        mockMvc
                .perform(
                        get(TABLE_BY_ID_ENDPOINT)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.tableNumber")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.capacity")
                                .value(4)
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );
    }
}
