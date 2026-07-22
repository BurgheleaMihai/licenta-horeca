package com.licenta.horeca.auth.service;

import com.licenta.horeca.auth.dto.LoginRequest;
import com.licenta.horeca.auth.dto.LoginResponse;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.employee.service.EmployeeShiftService;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.user.entity.User;
import com.licenta.horeca.user.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private static final String INVALID_CREDENTIALS_MESSAGE =
            "Email sau parola incorecta.";

    private static final String INACTIVE_USER_MESSAGE =
            "Utilizatorul este inactiv.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmployeeShiftService employeeShiftService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            EmployeeShiftService employeeShiftService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.employeeShiftService = employeeShiftService;
    }

    public LoginResponse login(LoginRequest request) {

        User user =
                userRepository.findByEmail(request.getEmail())
                        .orElseThrow(() ->
                                new BusinessException(
                                        INVALID_CREDENTIALS_MESSAGE
                                )
                        );

        if (!user.isActive()) {
            throw new BusinessException(
                    INACTIVE_USER_MESSAGE
            );
        }

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new BusinessException(
                    INVALID_CREDENTIALS_MESSAGE
            );
        }

        /*
         * După validarea credențialelor, sistemul garantează automat
         * existența unei ture pentru rolurile operaționale.
         *
         * Managerul și administratorul nu primesc ture operaționale.
         */
        employeeShiftService.ensureShiftForLogin(
                user.getId()
        );

        String authority =
                "ROLE_" + user
                        .getRole()
                        .getName()
                        .name();

        UserDetails userDetails =
                org.springframework.security.core.userdetails.User
                        .withUsername(user.getEmail())
                        .password(user.getPassword())
                        .authorities(authority)
                        .disabled(!user.isActive())
                        .build();

        String token =
                jwtService.generateToken(userDetails);

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName().name(),
                token
        );
    }
}