package com.licenta.horeca.employee.controller;

import com.licenta.horeca.employee.dto.user.CreateUserRequest;
import com.licenta.horeca.employee.dto.user.UpdateUserRequest;
import com.licenta.horeca.employee.dto.user.UserResponse;
import com.licenta.horeca.employee.service.EmployeeManagementService;
import com.licenta.horeca.user.enums.RoleType;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/employees")
public class EmployeeManagementController {

    private final EmployeeManagementService employeeManagementService;

    public EmployeeManagementController(
            EmployeeManagementService employeeManagementService
    ) {
        this.employeeManagementService =
                employeeManagementService;
    }

    @GetMapping
    public List<UserResponse> getAllEmployees() {
        return employeeManagementService.getAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponse getEmployee(
            @PathVariable Long id
    ) {
        return employeeManagementService.getUserById(id);
    }

    @PostMapping
    public UserResponse createEmployee(
            @Valid @RequestBody CreateUserRequest request
    ) {
        return employeeManagementService.createUser(request);
    }

    @PutMapping("/{id}")
    public UserResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserRequest request
    ) {
        return employeeManagementService.updateUser(
                id,
                request
        );
    }

    @PatchMapping("/{id}/status")
    public void changeStatus(
            @PathVariable Long id,
            @RequestParam boolean active
    ) {
        employeeManagementService.changeStatus(
                id,
                active
        );
    }

    @PatchMapping("/{id}/role")
    public void changeRole(
            @PathVariable Long id,
            @RequestParam RoleType role
    ) {
        employeeManagementService.changeRole(
                id,
                role
        );
    }
}