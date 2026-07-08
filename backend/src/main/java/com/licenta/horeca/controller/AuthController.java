package com.licenta.horeca.controller;

import com.licenta.horeca.dto.LoginRequest;
import com.licenta.horeca.dto.LoginResponse;
import com.licenta.horeca.service.AuthService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
}