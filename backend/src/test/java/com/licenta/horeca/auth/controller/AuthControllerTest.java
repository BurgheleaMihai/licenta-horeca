package com.licenta.horeca.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.auth.dto.LoginRequest;
import com.licenta.horeca.auth.dto.LoginResponse;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.auth.service.AuthService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String LOGIN_ENDPOINT = "/api/auth/login";

    private static final String MANAGER_NAME = "Manager Test";

    private static final String MANAGER_EMAIL = "manager@test.com";

    private static final String MANAGER_SECRET = "1234";

    private static final String MANAGER_ROLE = "MANAGER";

    private static final String JWT_TEST_TOKEN = "jwt-test-token";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void loginShouldReturnLoginResponse() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail(MANAGER_EMAIL);
        request.setPassword(MANAGER_SECRET);

        LoginResponse response = new LoginResponse(1L, MANAGER_NAME, MANAGER_EMAIL, MANAGER_ROLE, JWT_TEST_TOKEN);

        when(authService.login(any(LoginRequest.class))).thenReturn(response);

        mockMvc.perform(post(LOGIN_ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath("$.userId").value(1)).andExpect(jsonPath("$.fullName").value(MANAGER_NAME)).andExpect(jsonPath("$.email").value(MANAGER_EMAIL)).andExpect(jsonPath("$.role").value(MANAGER_ROLE)).andExpect(jsonPath("$.token").value(JWT_TEST_TOKEN));
    }
}
