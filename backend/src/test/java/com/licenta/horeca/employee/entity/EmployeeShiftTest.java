package com.licenta.horeca.employee.entity;

import com.licenta.horeca.employee.enums.ShiftEndReason;
import com.licenta.horeca.employee.enums.ShiftStartSource;
import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.entity.User;
import com.licenta.horeca.user.enums.RoleType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmployeeShiftTest {

    private User employee;

    private User manager;

    private LocalDateTime plannedStartAt;

    private LocalDateTime plannedEndAt;

    @BeforeEach
    void setUp() {
        Role employeeRole = new Role(RoleType.KITCHEN);

        Role managerRole = new Role(RoleType.MANAGER);

        employee = new User("Angajat Test", "employee@test.com", "encoded-password", employeeRole);

        manager = new User("Manager Test", "manager@test.com", "encoded-password", managerRole);

        plannedStartAt = LocalDateTime.of(2026, 7, 20, 8, 0);

        plannedEndAt = LocalDateTime.of(2026, 7, 20, 16, 0);
    }

    @Test
    void constructorShouldCreatePlannedShift() {
        EmployeeShift shift = createPlannedShift();

        assertSame(employee, shift.getEmployee());

        assertEquals(RoleType.KITCHEN, shift.getShiftRole());

        assertEquals(plannedStartAt, shift.getPlannedStartAt());

        assertEquals(plannedEndAt, shift.getPlannedEndAt());

        assertSame(manager, shift.getCreatedBy());

        assertTrue(shift.isPlanned());
        assertFalse(shift.isActive());
        assertFalse(shift.isClosed());

        assertNull(shift.getStartedAt());
        assertNull(shift.getEndedAt());
        assertNull(shift.getStartSource());
        assertNull(shift.getEndReason());
        assertNull(shift.getStartedBy());
        assertNull(shift.getEndedBy());
    }

    @Test
    void constructorShouldRejectMissingEmployee() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new EmployeeShift(null, RoleType.KITCHEN, plannedStartAt, plannedEndAt, manager));

        assertEquals("Angajatul este obligatoriu.", exception.getMessage());
    }

    @Test
    void constructorShouldRejectInvalidInterval() {
        LocalDateTime invalidEndAt = plannedStartAt.minusHours(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> new EmployeeShift(employee, RoleType.KITCHEN, plannedStartAt, invalidEndAt, manager));

        assertEquals("Ora de final trebuie sa fie ulterioara orei de inceput.", exception.getMessage());
    }

    @Test
    void startShouldActivatePlannedShift() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime actualStartAt = plannedStartAt.minusMinutes(10);

        shift.start(actualStartAt, ShiftStartSource.SCHEDULED_LOGIN, employee);

        assertFalse(shift.isPlanned());
        assertTrue(shift.isActive());
        assertFalse(shift.isClosed());

        assertEquals(actualStartAt, shift.getStartedAt());

        assertEquals(ShiftStartSource.SCHEDULED_LOGIN, shift.getStartSource());

        assertSame(employee, shift.getStartedBy());
    }

    @Test
    void startShouldRejectAlreadyStartedShift() {
        EmployeeShift shift = createActiveShift();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shift.start(plannedStartAt, ShiftStartSource.SCHEDULED_LOGIN, employee));

        assertEquals("Tura nu mai poate fi pornita.", exception.getMessage());
    }

    @Test
    void closeShouldFinishActiveShift() {
        EmployeeShift shift = createActiveShift();

        LocalDateTime actualEndAt = plannedEndAt.minusMinutes(15);

        shift.close(actualEndAt, manager, ShiftEndReason.MANUAL_MANAGER);

        assertFalse(shift.isPlanned());
        assertFalse(shift.isActive());
        assertTrue(shift.isClosed());

        assertEquals(actualEndAt, shift.getEndedAt());

        assertEquals(ShiftEndReason.MANUAL_MANAGER, shift.getEndReason());

        assertSame(manager, shift.getEndedBy());
    }

    @Test
    void closeShouldRejectPlannedShift() {
        EmployeeShift shift = createPlannedShift();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shift.close(plannedEndAt, manager, ShiftEndReason.MANUAL_MANAGER));

        assertEquals("Tura nu este activa.", exception.getMessage());
    }

    @Test
    void closeShouldRejectEndBeforeStart() {
        EmployeeShift shift = createActiveShift();

        LocalDateTime invalidEndAt = plannedStartAt.minusMinutes(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shift.close(invalidEndAt, manager, ShiftEndReason.MANUAL_MANAGER));

        assertEquals("Momentul inchiderii turei este invalid.", exception.getMessage());
    }

    @Test
    void rescheduleShouldChangePlannedInterval() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime newStartAt = plannedStartAt.plusHours(2);

        LocalDateTime newEndAt = plannedEndAt.plusHours(2);

        shift.reschedule(newStartAt, newEndAt);

        assertEquals(newStartAt, shift.getPlannedStartAt());

        assertEquals(newEndAt, shift.getPlannedEndAt());

        assertTrue(shift.isPlanned());
    }

    @Test
    void rescheduleShouldRejectActiveShift() {
        EmployeeShift shift = createActiveShift();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> shift.reschedule(plannedStartAt.plusHours(1), plannedEndAt.plusHours(1)));

        assertEquals("Doar o tura planificata poate fi modificata.", exception.getMessage());
    }

    @Test
    void cancelShouldKeepShiftInHistory() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime cancelledAt = plannedStartAt.minusHours(1);

        shift.cancel(cancelledAt, manager);

        assertFalse(shift.isPlanned());
        assertFalse(shift.isActive());
        assertTrue(shift.isClosed());

        assertEquals(cancelledAt, shift.getEndedAt());

        assertEquals(ShiftEndReason.CANCELLED, shift.getEndReason());

        assertSame(manager, shift.getEndedBy());
    }

    @Test
    void cancelShouldSupportAccountDeactivation() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime cancelledAt = plannedStartAt.minusHours(1);

        shift.cancel(cancelledAt, null, ShiftEndReason.ACCOUNT_DEACTIVATED);

        assertTrue(shift.isClosed());

        assertEquals(ShiftEndReason.ACCOUNT_DEACTIVATED, shift.getEndReason());

        assertEquals(cancelledAt, shift.getEndedAt());

        assertNull(shift.getEndedBy());
    }

    @Test
    void cancelShouldSupportRoleChange() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime cancelledAt = plannedStartAt.minusHours(1);

        shift.cancel(cancelledAt, null, ShiftEndReason.ROLE_CHANGED);

        assertTrue(shift.isClosed());

        assertEquals(ShiftEndReason.ROLE_CHANGED, shift.getEndReason());

        assertEquals(cancelledAt, shift.getEndedAt());

        assertNull(shift.getEndedBy());
    }

    @Test
    void manualCancellationShouldRequireUser() {
        EmployeeShift shift = createPlannedShift();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shift.cancel(plannedStartAt.minusHours(1), null, ShiftEndReason.CANCELLED));

        assertEquals("Utilizatorul care anuleaza tura este obligatoriu.", exception.getMessage());
    }

    @Test
    void markMissedShouldFinishUnstartedShiftAtPlannedEnd() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime processedAt = plannedEndAt.plusMinutes(5);

        shift.markMissed(processedAt);

        assertFalse(shift.isPlanned());
        assertFalse(shift.isActive());
        assertTrue(shift.isClosed());

        assertNull(shift.getStartedAt());

        assertEquals(plannedEndAt, shift.getEndedAt());

        assertEquals(ShiftEndReason.MISSED, shift.getEndReason());

        assertNull(shift.getEndedBy());
    }

    @Test
    void markMissedShouldRejectProcessingBeforePlannedEnd() {
        EmployeeShift shift = createPlannedShift();

        LocalDateTime processedAt = plannedEndAt.minusMinutes(1);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> shift.markMissed(processedAt));

        assertEquals("Tura nu poate fi marcata ca absenta inainte de finalul planificat.", exception.getMessage());
    }

    private EmployeeShift createPlannedShift() {
        return new EmployeeShift(employee, RoleType.KITCHEN, plannedStartAt, plannedEndAt, manager);
    }

    private EmployeeShift createActiveShift() {
        EmployeeShift shift = createPlannedShift();

        shift.start(plannedStartAt, ShiftStartSource.SCHEDULED_LOGIN, employee);

        return shift;
    }
}