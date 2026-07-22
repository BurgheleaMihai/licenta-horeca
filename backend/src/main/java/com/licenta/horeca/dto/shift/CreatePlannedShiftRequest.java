package com.licenta.horeca.dto.shift;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class CreatePlannedShiftRequest {

    @NotNull(message = "Angajatul este obligatoriu.")
    private Long employeeId;

    @NotNull(message = "Ora de inceput este obligatorie.")
    private LocalDateTime plannedStartAt;

    @NotNull(message = "Ora de final este obligatorie.")
    private LocalDateTime plannedEndAt;

    public CreatePlannedShiftRequest() {
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(
            LocalDateTime plannedStartAt
    ) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(
            LocalDateTime plannedEndAt
    ) {
        this.plannedEndAt = plannedEndAt;
    }
}
