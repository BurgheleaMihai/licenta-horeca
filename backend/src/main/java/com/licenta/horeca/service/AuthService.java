package com.licenta.horeca.service;

import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;

    public AuthService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Email sau parola incorecta."));

        if (!user.isActive()) {
            throw new RuntimeException("Utilizatorul este inactiv.");
        }

        if (!user.getPassword().equals(request.getPassword())) {
            throw new RuntimeException("Email sau parola incorecta.");
        }

        return new LoginResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getRole().getName().name()
        );
    }
}