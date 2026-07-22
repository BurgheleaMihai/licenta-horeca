package com.licenta.horeca.dto.shift;

import jakarta.validation.constraints.NotNull;

public class StartEmployeeShiftRequest {

    @NotNull(message = "Angajatul este obligatoriu.")
    private Long employeeId;

    public StartEmployeeShiftRequest() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }
}
