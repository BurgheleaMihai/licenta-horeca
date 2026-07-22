package com.licenta.horeca.auth.dto;

public class LoginRequest {
    private String email;
    private String password;

    public LoginRequest() {
        // Constructor gol pentru ca Spring sa transforme JSON in obiect Java
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
