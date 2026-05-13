package com.licenta.horeca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.controller.AuthController;
import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
class AuthControllerTest {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";
    private static final String MANAGER_NAME = "Manager Test";
    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final String MANAGER_SECRET = "1234";
    private static final String MANAGER_ROLE = "MANAGER";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @Test
    void loginShouldReturnLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(MANAGER_EMAIL);
        request.setPassword(MANAGER_SECRET);

        LoginResponse response = new LoginResponse(
                1L,
                MANAGER_NAME,
                MANAGER_EMAIL,
                MANAGER_ROLE
        );

        when(authService.login(any(LoginRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post(LOGIN_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.fullName").value(MANAGER_NAME))
                .andExpect(jsonPath("$.email").value(MANAGER_EMAIL))
                .andExpect(jsonPath("$.role").value(MANAGER_ROLE));
    }
}
