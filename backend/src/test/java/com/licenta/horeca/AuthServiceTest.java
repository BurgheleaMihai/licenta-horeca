package com.licenta.horeca.service;

import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.entity.Role;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void login_withValidCredentials_returnsLoginResponse() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User("Manager Test", "manager@test.com", "1234", role);

        LoginRequest request = new LoginRequest();
        request.setEmail("manager@test.com");
        request.setPassword("1234");

        when(userRepository.findByEmail("manager@test.com"))
                .thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertEquals("Manager Test", response.getFullName());
        assertEquals("manager@test.com", response.getEmail());
        assertEquals("MANAGER", response.getRole());
    }

    @Test
    void login_withInvalidEmail_throwsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@test.com");
        request.setPassword("1234");

        when(userRepository.findByEmail("wrong@test.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Email sau parola incorecta.", exception.getMessage());
    }

    @Test
    void login_withInvalidPassword_throwsException() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User("Manager Test", "manager@test.com", "1234", role);

        LoginRequest request = new LoginRequest();
        request.setEmail("manager@test.com");
        request.setPassword("wrong-password");

        when(userRepository.findByEmail("manager@test.com"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Email sau parola incorecta.", exception.getMessage());
    }

    @Test
    void login_withInactiveUser_throwsException() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User("Manager Test", "manager@test.com", "1234", role);
        user.setActive(false);

        LoginRequest request = new LoginRequest();
        request.setEmail("manager@test.com");
        request.setPassword("1234");

        when(userRepository.findByEmail("manager@test.com"))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals("Utilizatorul este inactiv.", exception.getMessage());
    }
}