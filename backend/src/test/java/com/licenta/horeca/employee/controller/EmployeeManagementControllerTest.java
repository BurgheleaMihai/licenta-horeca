package com.licenta.horeca.employee.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.employee.dto.user.CreateUserRequest;
import com.licenta.horeca.employee.dto.user.UpdateUserRequest;
import com.licenta.horeca.employee.dto.user.UserResponse;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.employee.service.EmployeeManagementService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EmployeeManagementController.class)
@Import(SecurityConfig.class)
class EmployeeManagementControllerTest {

    private static final Long EMPLOYEE_ID = 1L;

    private static final String EMPLOYEE_NAME =
            "Ospatar Test";

    private static final String UPDATED_EMPLOYEE_NAME =
            "Ospatar Actualizat";

    private static final String EMPLOYEE_EMAIL =
            "waiter@test.com";

    private static final String UPDATED_EMPLOYEE_EMAIL =
            "waiter.updated@test.com";

    private static final String EMPLOYEE_PASSWORD =
            "Parola123!";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmployeeManagementService employeeManagementService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void getAllEmployeesShouldReturnEmployeeList()
            throws Exception {

        UserResponse waiter =
                createResponse(
                        1L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        UserResponse manager =
                createResponse(
                        2L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        false
                );

        when(employeeManagementService.getAllUsers())
                .thenReturn(
                        List.of(
                                waiter,
                                manager
                        )
                );

        mockMvc
                .perform(
                        get("/api/employees")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].fullName")
                                .value("Ospatar Test")
                )
                .andExpect(
                        jsonPath("$[0].email")
                                .value("waiter@test.com")
                )
                .andExpect(
                        jsonPath("$[0].role")
                                .value("WAITER")
                )
                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$[1].role")
                                .value("MANAGER")
                )
                .andExpect(
                        jsonPath("$[1].active")
                                .value(false)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void getEmployeeShouldReturnEmployee()
            throws Exception {

        UserResponse response =
                createResponse(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        RoleType.WAITER,
                        true
                );

        when(
                employeeManagementService.getUserById(
                        EMPLOYEE_ID
                )
        ).thenReturn(response);

        mockMvc
                .perform(
                        get(
                                "/api/employees/{id}",
                                EMPLOYEE_ID
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(EMPLOYEE_ID)
                )
                .andExpect(
                        jsonPath("$.fullName")
                                .value(EMPLOYEE_NAME)
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(EMPLOYEE_EMAIL)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("WAITER")
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void createEmployeeShouldReturnCreatedEmployee()
            throws Exception {

        CreateUserRequest request =
                new CreateUserRequest();

        request.setFullName(EMPLOYEE_NAME);
        request.setEmail(EMPLOYEE_EMAIL);
        request.setPassword(EMPLOYEE_PASSWORD);
        request.setRole(RoleType.WAITER);

        UserResponse response =
                createResponse(
                        EMPLOYEE_ID,
                        EMPLOYEE_NAME,
                        EMPLOYEE_EMAIL,
                        RoleType.WAITER,
                        true
                );

        when(
                employeeManagementService.createUser(
                        any(CreateUserRequest.class)
                )
        ).thenReturn(response);

        mockMvc
                .perform(
                        post("/api/employees")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(EMPLOYEE_ID)
                )
                .andExpect(
                        jsonPath("$.fullName")
                                .value(EMPLOYEE_NAME)
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("WAITER")
                )
                .andExpect(
                        jsonPath("$.active")
                                .value(true)
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void updateEmployeeShouldReturnUpdatedEmployee()
            throws Exception {

        UpdateUserRequest request =
                new UpdateUserRequest();

        request.setFullName(
                UPDATED_EMPLOYEE_NAME
        );

        request.setEmail(
                UPDATED_EMPLOYEE_EMAIL
        );

        request.setRole(
                RoleType.MANAGER
        );

        UserResponse response =
                createResponse(
                        EMPLOYEE_ID,
                        UPDATED_EMPLOYEE_NAME,
                        UPDATED_EMPLOYEE_EMAIL,
                        RoleType.MANAGER,
                        true
                );

        when(
                employeeManagementService.updateUser(
                        any(Long.class),
                        any(UpdateUserRequest.class)
                )
        ).thenReturn(response);

        mockMvc
                .perform(
                        put(
                                "/api/employees/{id}",
                                EMPLOYEE_ID
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(EMPLOYEE_ID)
                )
                .andExpect(
                        jsonPath("$.fullName")
                                .value(
                                        UPDATED_EMPLOYEE_NAME
                                )
                )
                .andExpect(
                        jsonPath("$.email")
                                .value(
                                        UPDATED_EMPLOYEE_EMAIL
                                )
                )
                .andExpect(
                        jsonPath("$.role")
                                .value("MANAGER")
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void changeStatusShouldReturnOk()
            throws Exception {

        mockMvc
                .perform(
                        patch(
                                "/api/employees/{id}/status",
                                EMPLOYEE_ID
                        )
                                .param(
                                        "active",
                                        "false"
                                )
                )
                .andExpect(status().isOk());

        verify(employeeManagementService)
                .changeStatus(
                        EMPLOYEE_ID,
                        false
                );
    }

    @Test
    @WithMockUser(
            username = "admin@test.com",
            roles = "ADMIN"
    )
    void changeRoleShouldReturnOk()
            throws Exception {

        mockMvc
                .perform(
                        patch(
                                "/api/employees/{id}/role",
                                EMPLOYEE_ID
                        )
                                .param(
                                        "role",
                                        "MANAGER"
                                )
                )
                .andExpect(status().isOk());

        verify(employeeManagementService)
                .changeRole(
                        EMPLOYEE_ID,
                        RoleType.MANAGER
                );
    }

    @Test
    @WithMockUser(
            username = "manager@test.com",
            roles = "MANAGER"
    )
    void managerShouldNotAccessEmployeeManagement()
            throws Exception {

        mockMvc
                .perform(
                        get("/api/employees")
                )
                .andExpect(
                        status().isForbidden()
                );
    }


    private UserResponse createResponse(
            Long id,
            String fullName,
            String email,
            RoleType role,
            boolean active
    ) {
        UserResponse response =
                new UserResponse();

        response.setId(id);
        response.setFullName(fullName);
        response.setEmail(email);
        response.setRole(role);
        response.setActive(active);

        return response;
    }

    @Test
    void unauthenticatedUserShouldReceiveUnauthorized()
            throws Exception {

        mockMvc
                .perform(
                        get("/api/employees")
                )
                .andExpect(
                        status().isUnauthorized()
                );
    }
}
