package com.licenta.horeca.employee.dto.user;

import com.licenta.horeca.user.enums.RoleType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateUserRequest {

    @NotBlank(message = "Numele este obligatoriu.")
    private String fullName;

    @Email(message = "Email invalid.")
    @NotBlank(message = "Emailul este obligatoriu.")
    private String email;

    @NotBlank(message = "Parola este obligatorie.")
    @Size(min = 8, message = "Parola trebuie sa aiba minim 8 caractere.")
    private String password;

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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public RoleType getRole() {
        return role;
    }

    public void setRole(RoleType role) {
        this.role = role;
    }
}