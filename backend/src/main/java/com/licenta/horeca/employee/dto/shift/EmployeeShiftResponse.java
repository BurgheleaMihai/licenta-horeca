package com.licenta.horeca.employee.dto.shift;

import com.licenta.horeca.employee.enums.ShiftEndReason;
import com.licenta.horeca.employee.enums.ShiftStartSource;
import com.licenta.horeca.user.enums.RoleType;

import java.time.LocalDateTime;

public class EmployeeShiftResponse {

    private Long id;

    private Long employeeId;

    private String employeeName;

    private RoleType shiftRole;

    private LocalDateTime startedAt;

    private LocalDateTime endedAt;

    private boolean open;

    private Long durationMinutes;

    private Long startedByUserId;

    private String startedByName;

    private Long endedByUserId;

    private String endedByName;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    private ShiftStartSource startSource;

    private ShiftEndReason endReason;

    private Long createdByUserId;

    private String createdByName;

    public EmployeeShiftResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(Long employeeId) {
        this.employeeId = employeeId;
    }

    public String getEmployeeName() {
        return employeeName;
    }

    public void setEmployeeName(String employeeName) {
        this.employeeName = employeeName;
    }

    public RoleType getShiftRole() {
        return shiftRole;
    }

    public void setShiftRole(RoleType shiftRole) {
        this.shiftRole = shiftRole;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isOpen() {
        return open;
    }

    public void setOpen(boolean open) {
        this.open = open;
    }

    public Long getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(Long durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public Long getStartedByUserId() {
        return startedByUserId;
    }

    public void setStartedByUserId(Long startedByUserId) {
        this.startedByUserId = startedByUserId;
    }

    public String getStartedByName() {
        return startedByName;
    }

    public void setStartedByName(String startedByName) {
        this.startedByName = startedByName;
    }

    public Long getEndedByUserId() {
        return endedByUserId;
    }

    public void setEndedByUserId(Long endedByUserId) {
        this.endedByUserId = endedByUserId;
    }

    public String getEndedByName() {
        return endedByName;
    }

    public void setEndedByName(String endedByName) {
        this.endedByName = endedByName;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public void setPlannedStartAt(LocalDateTime plannedStartAt) {
        this.plannedStartAt = plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public void setPlannedEndAt(LocalDateTime plannedEndAt) {
        this.plannedEndAt = plannedEndAt;
    }

    public ShiftStartSource getStartSource() {
        return startSource;
    }

    public void setStartSource(ShiftStartSource startSource) {
        this.startSource = startSource;
    }

    public ShiftEndReason getEndReason() {
        return endReason;
    }

    public void setEndReason(ShiftEndReason endReason) {
        this.endReason = endReason;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public void setCreatedByUserId(Long createdByUserId) {
        this.createdByUserId = createdByUserId;
    }

    public String getCreatedByName() {
        return createdByName;
    }

    public void setCreatedByName(String createdByName) {
        this.createdByName = createdByName;
    }
}