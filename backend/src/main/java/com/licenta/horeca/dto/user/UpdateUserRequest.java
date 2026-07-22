package com.licenta.horeca.dto.user;

import com.licenta.horeca.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class UpdateUserRequest {

    @NotBlank(message = "Numele este obligatoriu.")
    private String fullName;

    @Email(message = "Email invalid.")
    @NotBlank(message = "Emailul este obligatoriu.")
    private String email;

    @NotNull(message = "Rolul este obligatoriu.")
    private RoleType role;

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }
}