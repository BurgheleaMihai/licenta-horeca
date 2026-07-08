package com.licenta.horeca.service;

import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private static final String INVALID_CREDENTIALS_MESSAGE = "Email sau parola incorecta.";
    private static final String INACTIVE_USER_MESSAGE = "Utilizatorul este inactiv.";
    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail()).orElseThrow(() ->
                new BusinessException(INVALID_CREDENTIALS_MESSAGE));
        if (!user.isActive()) {
            throw new BusinessException(INACTIVE_USER_MESSAGE);
        }
        if (!user.getPassword().equals(request.getPassword())) {
            throw new BusinessException(INVALID_CREDENTIALS_MESSAGE);
        }
        return new LoginResponse(user.getId(), user.getFullName(), user.getEmail(), user.getRole().getName().name());
    }
}