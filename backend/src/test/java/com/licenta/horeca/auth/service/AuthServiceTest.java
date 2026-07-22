package com.licenta.horeca.auth.service;

import com.licenta.horeca.auth.dto.LoginRequest;
import com.licenta.horeca.auth.dto.LoginResponse;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.entity.User;
import com.licenta.horeca.user.enums.RoleType;
import com.licenta.horeca.user.repository.UserRepository;
import com.licenta.horeca.employee.service.EmployeeShiftService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final Long MANAGER_ID =
            1L;

    private static final String MANAGER_NAME =
            "Manager Test";

    private static final String MANAGER_EMAIL =
            "manager@test.com";

    private static final String MANAGER_TEST_SECRET =
            "1234";

    private static final String ENCODED_MANAGER_TEST_SECRET =
            "$2a$10$encodedPasswordForTesting";

    private static final String INVALID_TEST_SECRET =
            "wrong-password";

    private static final String JWT_TEST_TOKEN =
            "jwt-test-token";

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Email sau parola incorecta.";

    private static final String INACTIVE_USER_MESSAGE =
            "Utilizatorul este inactiv.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private EmployeeShiftService employeeShiftService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginWithValidCredentialsReturnsLoginResponse() {

        User user =
                createManagerUser(true);

        LoginRequest request =
                createLoginRequest(
                        MANAGER_EMAIL,
                        MANAGER_TEST_SECRET
                );

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        MANAGER_TEST_SECRET,
                        ENCODED_MANAGER_TEST_SECRET
                )
        ).thenReturn(true);

        when(
                jwtService.generateToken(
                        any(UserDetails.class)
                )
        ).thenReturn(JWT_TEST_TOKEN);

        LoginResponse response =
                authService.login(request);

        assertEquals(
                MANAGER_ID,
                response.getUserId()
        );

        assertEquals(
                MANAGER_NAME,
                response.getFullName()
        );

        assertEquals(
                MANAGER_EMAIL,
                response.getEmail()
        );

        assertEquals(
                "MANAGER",
                response.getRole()
        );

        assertEquals(
                JWT_TEST_TOKEN,
                response.getToken()
        );

        /*
         * AuthService apeleaza mecanismul turelor dupa
         * autentificarea reusita. Pentru MANAGER, service-ul
         * nu va crea o tura operationala.
         */
        verify(employeeShiftService)
                .ensureShiftForLogin(
                        MANAGER_ID
                );
    }

    @Test
    void loginWithInvalidEmailThrowsException() {

        LoginRequest request =
                createLoginRequest(
                        "wrong@test.com",
                        MANAGER_TEST_SECRET
                );

        when(userRepository.findByEmail("wrong@test.com"))
                .thenReturn(Optional.empty());

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                INVALID_CREDENTIALS_MESSAGE,
                exception.getMessage()
        );

        verifyNoInteractions(
                passwordEncoder,
                jwtService,
                employeeShiftService
        );
    }

    @Test
    void loginWithInvalidPasswordThrowsException() {

        User user =
                createManagerUser(true);

        LoginRequest request =
                createLoginRequest(
                        MANAGER_EMAIL,
                        INVALID_TEST_SECRET
                );

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        when(
                passwordEncoder.matches(
                        INVALID_TEST_SECRET,
                        ENCODED_MANAGER_TEST_SECRET
                )
        ).thenReturn(false);

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                INVALID_CREDENTIALS_MESSAGE,
                exception.getMessage()
        );

        verifyNoInteractions(
                jwtService,
                employeeShiftService
        );
    }

    @Test
    void loginWithInactiveUserThrowsException() {

        User user =
                createManagerUser(false);

        LoginRequest request =
                createLoginRequest(
                        MANAGER_EMAIL,
                        MANAGER_TEST_SECRET
                );

        when(userRepository.findByEmail(MANAGER_EMAIL))
                .thenReturn(Optional.of(user));

        RuntimeException exception =
                assertThrows(
                        RuntimeException.class,
                        () -> authService.login(request)
                );

        assertEquals(
                INACTIVE_USER_MESSAGE,
                exception.getMessage()
        );

        verifyNoInteractions(
                passwordEncoder,
                jwtService,
                employeeShiftService
        );
    }

    private User createManagerUser(
            boolean active
    ) {
        Role role =
                new Role(
                        RoleType.MANAGER
                );

        User user =
                new User(
                        MANAGER_NAME,
                        MANAGER_EMAIL,
                        ENCODED_MANAGER_TEST_SECRET,
                        role
                );

        ReflectionTestUtils.setField(
                user,
                "id",
                MANAGER_ID
        );

        user.setActive(active);

        return user;
    }

    private LoginRequest createLoginRequest(
            String email,
            String password
    ) {
        LoginRequest request =
                new LoginRequest();

        request.setEmail(email);
        request.setPassword(password);

        return request;
    }
}
