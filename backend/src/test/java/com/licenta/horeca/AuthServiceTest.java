package com.licenta.horeca;

import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.entity.Role;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.repository.UserRepository;
import com.licenta.horeca.service.AuthService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String MANAGER_NAME = "Manager Test";
    private static final String MANAGER_EMAIL = "manager@test.com";
    private static final String MANAGER_TEST_SECRET = "1234";
    private static final String INVALID_TEST_SECRET = "wrong-password";
    private static final String INVALID_CREDENTIALS_MESSAGE = "Email sau parola incorecta.";
    private static final String INACTIVE_USER_MESSAGE = "Utilizatorul este inactiv.";

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithValidCredentialsReturnsLoginResponse() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User(MANAGER_NAME, MANAGER_EMAIL, MANAGER_TEST_SECRET, role);

        LoginRequest request = new LoginRequest();
        request.setEmail(MANAGER_EMAIL);
        request.setPassword(MANAGER_TEST_SECRET);

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        LoginResponse response = authService.login(request);

        assertEquals(MANAGER_NAME, response.getFullName());
        assertEquals(MANAGER_EMAIL, response.getEmail());
        assertEquals("MANAGER", response.getRole());
    }

    @Test
    void loginWithInvalidEmailThrowsException() {
        LoginRequest request = new LoginRequest();
        request.setEmail("wrong@test.com");
        request.setPassword(MANAGER_TEST_SECRET);

        when(userRepository.findByEmail("wrong@test.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals(INVALID_CREDENTIALS_MESSAGE, exception.getMessage());
    }

    @Test
    void loginWithInvalidPasswordThrowsException() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User(MANAGER_NAME, MANAGER_EMAIL, MANAGER_TEST_SECRET, role);

        LoginRequest request = new LoginRequest();
        request.setEmail(MANAGER_EMAIL);
        request.setPassword(INVALID_TEST_SECRET);

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals(INVALID_CREDENTIALS_MESSAGE, exception.getMessage());
    }

    @Test
    void loginWithInactiveUserThrowsException() {
        Role role = new Role(RoleType.MANAGER);
        User user = new User(MANAGER_NAME, MANAGER_EMAIL, MANAGER_TEST_SECRET, role);
        user.setActive(false);

        LoginRequest request = new LoginRequest();
        request.setEmail(MANAGER_EMAIL);
        request.setPassword(MANAGER_TEST_SECRET);

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> authService.login(request)
        );

        assertEquals(INACTIVE_USER_MESSAGE, exception.getMessage());
    }
}