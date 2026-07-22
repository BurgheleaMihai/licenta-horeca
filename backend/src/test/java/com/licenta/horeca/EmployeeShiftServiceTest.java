package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.licenta.horeca.dto.shift.ActiveStaffSummaryResponse;
import com.licenta.horeca.dto.shift.EmployeeShiftResponse;
import com.licenta.horeca.entity.EmployeeShift;
import com.licenta.horeca.entity.Role;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.enums.ShiftEndReason;
import com.licenta.horeca.enums.ShiftStartSource;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.repository.EmployeeShiftRepository;
import com.licenta.horeca.repository.UserRepository;
import com.licenta.horeca.service.EmployeeShiftService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EmployeeShiftServiceTest {

    private static final long UNSCHEDULED_MAX_HOURS =
            16L;

    private static final long MAXIMUM_DURATION_HOURS =
            16L;

    private static final long EARLY_LOGIN_MINUTES =
            60L;

    @Mock
    private EmployeeShiftRepository employeeShiftRepository;

    @Mock
    private UserRepository userRepository;

    private EmployeeShiftService employeeShiftService;

    @BeforeEach
    void setUp() {
        employeeShiftService =
                new EmployeeShiftService(
                        employeeShiftRepository,
                        userRepository,
                        UNSCHEDULED_MAX_HOURS,
                        MAXIMUM_DURATION_HOURS,
                        EARLY_LOGIN_MINUTES
                );
    }

    @Test
    void ensureShiftForLoginShouldIgnoreManager() {
        User manager =
                createUser(
                        1L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        when(userRepository.findByIdForUpdate(1L))
                .thenReturn(Optional.of(manager));

        employeeShiftService.ensureShiftForLogin(1L);

        verifyNoInteractions(
                employeeShiftRepository
        );
    }

    @Test
    void ensureShiftForLoginShouldRejectInactiveEmployee() {
        User waiter =
                createUser(
                        2L,
                        "Ospatar Inactiv",
                        "waiter@test.com",
                        RoleType.WAITER,
                        false
                );

        when(userRepository.findByIdForUpdate(2L))
                .thenReturn(Optional.of(waiter));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> employeeShiftService
                                .ensureShiftForLogin(2L)
                );

        assertEquals(
                "Tura nu poate fi pornita pentru un cont dezactivat.",
                exception.getMessage()
        );

        verifyNoInteractions(
                employeeShiftRepository
        );
    }

    @Test
    void ensureShiftForLoginShouldKeepExistingActiveShift() {
        User waiter =
                createUser(
                        3L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift activeShift =
                createActiveShift(
                        10L,
                        waiter,
                        waiter,
                        now.minusHours(1),
                        now.plusHours(5),
                        ShiftStartSource.UNSCHEDULED_LOGIN
                );

        when(userRepository.findByIdForUpdate(3L))
                .thenReturn(Optional.of(waiter));

        when(
                employeeShiftRepository
                        .findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
                                3L
                        )
        ).thenReturn(Optional.of(activeShift));

        employeeShiftService.ensureShiftForLogin(3L);

        verify(
                employeeShiftRepository,
                never()
        ).save(any(EmployeeShift.class));

        verify(
                employeeShiftRepository,
                never()
        ).findFirstByEmployeeIdAndStartedAtIsNullAndEndedAtIsNullAndPlannedStartAtLessThanEqualAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
                eq(3L),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
    }

    @Test
    void ensureShiftForLoginShouldStartPlannedShift() {
        User kitchenEmployee =
                createUser(
                        4L,
                        "Bucatar Test",
                        "kitchen@test.com",
                        RoleType.KITCHEN,
                        true
                );

        User manager =
                createUser(
                        5L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift plannedShift =
                createPlannedShift(
                        11L,
                        kitchenEmployee,
                        manager,
                        now.minusMinutes(15),
                        now.plusHours(6)
                );

        when(userRepository.findByIdForUpdate(4L))
                .thenReturn(Optional.of(kitchenEmployee));

        when(
                employeeShiftRepository
                        .findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
                                4L
                        )
        ).thenReturn(Optional.empty());

        when(
                employeeShiftRepository
                        .findFirstByEmployeeIdAndStartedAtIsNullAndEndedAtIsNullAndPlannedStartAtLessThanEqualAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
                                eq(4L),
                                any(LocalDateTime.class),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(Optional.of(plannedShift));

        when(employeeShiftRepository.save(plannedShift))
                .thenReturn(plannedShift);

        employeeShiftService.ensureShiftForLogin(4L);

        assertTrue(plannedShift.isActive());

        assertEquals(
                ShiftStartSource.SCHEDULED_LOGIN,
                plannedShift.getStartSource()
        );

        assertSame(
                kitchenEmployee,
                plannedShift.getStartedBy()
        );

        verify(employeeShiftRepository)
                .save(plannedShift);
    }

    @Test
    void ensureShiftForLoginShouldCreateUnscheduledShift() {
        User barEmployee =
                createUser(
                        6L,
                        "Barman Test",
                        "bar@test.com",
                        RoleType.BAR,
                        true
                );

        when(userRepository.findByIdForUpdate(6L))
                .thenReturn(Optional.of(barEmployee));

        when(
                employeeShiftRepository
                        .findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
                                6L
                        )
        ).thenReturn(Optional.empty());

        when(
                employeeShiftRepository
                        .findFirstByEmployeeIdAndStartedAtIsNullAndEndedAtIsNullAndPlannedStartAtLessThanEqualAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
                                eq(6L),
                                any(LocalDateTime.class),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(Optional.empty());

        when(employeeShiftRepository.save(any(EmployeeShift.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        employeeShiftService.ensureShiftForLogin(6L);

        ArgumentCaptor<EmployeeShift> shiftCaptor =
                ArgumentCaptor.forClass(
                        EmployeeShift.class
                );

        verify(employeeShiftRepository)
                .save(shiftCaptor.capture());

        EmployeeShift savedShift =
                shiftCaptor.getValue();

        assertTrue(savedShift.isActive());

        assertEquals(
                RoleType.BAR,
                savedShift.getShiftRole()
        );

        assertEquals(
                ShiftStartSource.UNSCHEDULED_LOGIN,
                savedShift.getStartSource()
        );

        assertSame(
                barEmployee,
                savedShift.getEmployee()
        );

        assertSame(
                barEmployee,
                savedShift.getCreatedBy()
        );

        assertSame(
                barEmployee,
                savedShift.getStartedBy()
        );

        long durationHours =
                Duration.between(
                        savedShift.getPlannedStartAt(),
                        savedShift.getPlannedEndAt()
                ).toHours();

        assertEquals(
                UNSCHEDULED_MAX_HOURS,
                durationHours
        );
    }

    @Test
    void createPlannedShiftShouldSaveValidShift() {
        User manager =
                createUser(
                        7L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User waiter =
                createUser(
                        8L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        LocalDateTime plannedStart =
                LocalDateTime.now().plusHours(1);

        LocalDateTime plannedEnd =
                plannedStart.plusHours(6);

        when(userRepository.findByEmail(
                "manager@test.com"
        )).thenReturn(Optional.of(manager));

        when(userRepository.findByIdForUpdate(8L))
                .thenReturn(Optional.of(waiter));

        when(
                employeeShiftRepository
                        .existsByEmployeeIdAndEndedAtIsNullAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
                                8L,
                                plannedEnd,
                                plannedStart
                        )
        ).thenReturn(false);

        when(employeeShiftRepository.save(any(EmployeeShift.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        EmployeeShiftResponse response =
                employeeShiftService.createPlannedShift(
                        8L,
                        plannedStart,
                        plannedEnd,
                        "manager@test.com"
                );

        assertEquals(
                8L,
                response.getEmployeeId()
        );

        assertEquals(
                "Ospatar Test",
                response.getEmployeeName()
        );

        assertEquals(
                RoleType.WAITER,
                response.getShiftRole()
        );

        assertEquals(
                plannedStart,
                response.getPlannedStartAt()
        );

        assertEquals(
                plannedEnd,
                response.getPlannedEndAt()
        );

        assertEquals(
                7L,
                response.getCreatedByUserId()
        );

        assertEquals(
                "Manager Test",
                response.getCreatedByName()
        );

        assertFalse(response.isOpen());
    }

    @Test
    void createPlannedShiftShouldRejectOverlap() {
        User manager =
                createUser(
                        9L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User kitchenEmployee =
                createUser(
                        10L,
                        "Bucatar Test",
                        "kitchen@test.com",
                        RoleType.KITCHEN,
                        true
                );

        LocalDateTime plannedStart =
                LocalDateTime.now().plusHours(1);

        LocalDateTime plannedEnd =
                plannedStart.plusHours(8);

        when(userRepository.findByEmail(
                "manager@test.com"
        )).thenReturn(Optional.of(manager));

        when(userRepository.findByIdForUpdate(10L))
                .thenReturn(Optional.of(kitchenEmployee));

        when(
                employeeShiftRepository
                        .existsByEmployeeIdAndEndedAtIsNullAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
                                10L,
                                plannedEnd,
                                plannedStart
                        )
        ).thenReturn(true);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> employeeShiftService
                                .createPlannedShift(
                                        10L,
                                        plannedStart,
                                        plannedEnd,
                                        "manager@test.com"
                                )
                );

        assertEquals(
                "Angajatul are deja o tura care se suprapune cu intervalul ales.",
                exception.getMessage()
        );

        verify(
                employeeShiftRepository,
                never()
        ).save(any(EmployeeShift.class));
    }

    @Test
    void createPlannedShiftShouldRejectDurationOverMaximum() {
        User admin =
                createUser(
                        11L,
                        "Administrator Test",
                        "admin@test.com",
                        RoleType.ADMIN,
                        true
                );

        User waiter =
                createUser(
                        12L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        LocalDateTime plannedStart =
                LocalDateTime.now().plusHours(1);

        LocalDateTime plannedEnd =
                plannedStart.plusHours(17);

        when(userRepository.findByEmail(
                "admin@test.com"
        )).thenReturn(Optional.of(admin));

        when(userRepository.findByIdForUpdate(12L))
                .thenReturn(Optional.of(waiter));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> employeeShiftService
                                .createPlannedShift(
                                        12L,
                                        plannedStart,
                                        plannedEnd,
                                        "admin@test.com"
                                )
                );

        assertEquals(
                "O tura nu poate depasi 16 ore.",
                exception.getMessage()
        );

        verify(
                employeeShiftRepository,
                never()
        ).save(any(EmployeeShift.class));
    }

    @Test
    void closeShiftShouldFinishActiveShift() {
        User admin =
                createUser(
                        13L,
                        "Administrator Test",
                        "admin@test.com",
                        RoleType.ADMIN,
                        true
                );

        User waiter =
                createUser(
                        14L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift activeShift =
                createActiveShift(
                        20L,
                        waiter,
                        waiter,
                        now.minusHours(2),
                        now.plusHours(6),
                        ShiftStartSource.UNSCHEDULED_LOGIN
                );

        when(userRepository.findByEmail(
                "admin@test.com"
        )).thenReturn(Optional.of(admin));

        when(employeeShiftRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(activeShift));

        when(employeeShiftRepository.save(activeShift))
                .thenReturn(activeShift);

        EmployeeShiftResponse response =
                employeeShiftService.closeShift(
                        20L,
                        "admin@test.com"
                );

        assertFalse(response.isOpen());

        assertEquals(
                ShiftEndReason.MANUAL_MANAGER,
                response.getEndReason()
        );

        assertEquals(
                13L,
                response.getEndedByUserId()
        );

        assertEquals(
                "Administrator Test",
                response.getEndedByName()
        );

        assertNotNull(response.getEndedAt());
    }

    @Test
    void updatePlannedShiftShouldChangeInterval() {
        User manager =
                createUser(
                        15L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User employee =
                createUser(
                        16L,
                        "Angajat Test",
                        "employee@test.com",
                        RoleType.BAR,
                        true
                );

        LocalDateTime originalStart =
                LocalDateTime.now().plusDays(1);

        LocalDateTime originalEnd =
                originalStart.plusHours(4);

        EmployeeShift plannedShift =
                createPlannedShift(
                        30L,
                        employee,
                        manager,
                        originalStart,
                        originalEnd
                );

        LocalDateTime newStart =
                originalStart.plusHours(2);

        LocalDateTime newEnd =
                newStart.plusHours(6);

        when(userRepository.findByEmail(
                "manager@test.com"
        )).thenReturn(Optional.of(manager));

        when(employeeShiftRepository.findByIdForUpdate(30L))
                .thenReturn(Optional.of(plannedShift));

        when(
                employeeShiftRepository
                        .countOverlappingShiftsExcludingId(
                                16L,
                                30L,
                                newStart,
                                newEnd
                        )
        ).thenReturn(0L);

        when(employeeShiftRepository.save(plannedShift))
                .thenReturn(plannedShift);

        EmployeeShiftResponse response =
                employeeShiftService.updatePlannedShift(
                        30L,
                        newStart,
                        newEnd,
                        "manager@test.com"
                );

        assertEquals(
                newStart,
                response.getPlannedStartAt()
        );

        assertEquals(
                newEnd,
                response.getPlannedEndAt()
        );
    }

    @Test
    void cancelPlannedShiftShouldKeepCancellationInHistory() {
        User manager =
                createUser(
                        17L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User employee =
                createUser(
                        18L,
                        "Angajat Test",
                        "employee@test.com",
                        RoleType.KITCHEN,
                        true
                );

        LocalDateTime plannedStart =
                LocalDateTime.now().plusHours(2);

        LocalDateTime plannedEnd =
                plannedStart.plusHours(8);

        EmployeeShift plannedShift =
                createPlannedShift(
                        40L,
                        employee,
                        manager,
                        plannedStart,
                        plannedEnd
                );

        when(userRepository.findByEmail(
                "manager@test.com"
        )).thenReturn(Optional.of(manager));

        when(employeeShiftRepository.findByIdForUpdate(40L))
                .thenReturn(Optional.of(plannedShift));

        when(employeeShiftRepository.save(plannedShift))
                .thenReturn(plannedShift);

        EmployeeShiftResponse response =
                employeeShiftService.cancelPlannedShift(
                        40L,
                        "manager@test.com"
                );

        assertFalse(response.isOpen());

        assertEquals(
                ShiftEndReason.CANCELLED,
                response.getEndReason()
        );

        assertEquals(
                17L,
                response.getEndedByUserId()
        );

        assertEquals(
                "Manager Test",
                response.getEndedByName()
        );

        assertNotNull(response.getEndedAt());
    }

    @Test
    void cleanupExpiredShiftsShouldCloseAndMarkMissedShifts() {
        User manager =
                createUser(
                        19L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User kitchenEmployee =
                createUser(
                        20L,
                        "Bucatar Test",
                        "kitchen@test.com",
                        RoleType.KITCHEN,
                        true
                );

        User waiter =
                createUser(
                        21L,
                        "Ospatar Test",
                        "waiter@test.com",
                        RoleType.WAITER,
                        true
                );

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift expiredActiveShift =
                createActiveShift(
                        50L,
                        kitchenEmployee,
                        manager,
                        now.minusHours(8),
                        now.minusMinutes(10),
                        ShiftStartSource.SCHEDULED_LOGIN
                );

        EmployeeShift missedShift =
                createPlannedShift(
                        51L,
                        waiter,
                        manager,
                        now.minusHours(6),
                        now.minusHours(2)
                );

        when(
                employeeShiftRepository
                        .findExpiredActiveShiftsForUpdate(
                                any(LocalDateTime.class)
                        )
        ).thenReturn(List.of(expiredActiveShift));

        when(
                employeeShiftRepository
                        .findMissedPlannedShiftsForUpdate(
                                any(LocalDateTime.class)
                        )
        ).thenReturn(List.of(missedShift));

        int processedCount =
                employeeShiftService.cleanupExpiredShifts();

        assertEquals(
                2,
                processedCount
        );

        assertEquals(
                ShiftEndReason.AUTO_PLANNED_END,
                expiredActiveShift.getEndReason()
        );

        assertEquals(
                expiredActiveShift.getPlannedEndAt(),
                expiredActiveShift.getEndedAt()
        );

        assertEquals(
                ShiftEndReason.MISSED,
                missedShift.getEndReason()
        );

        assertEquals(
                missedShift.getPlannedEndAt(),
                missedShift.getEndedAt()
        );

        verify(employeeShiftRepository)
                .saveAll(List.of(expiredActiveShift));

        verify(employeeShiftRepository)
                .saveAll(List.of(missedShift));
    }

    @Test
    void getActiveStaffSummaryShouldReturnCountsByRole() {
        when(
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                eq(RoleType.WAITER),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(2L);

        when(
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                eq(RoleType.KITCHEN),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(3L);

        when(
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                eq(RoleType.BAR),
                                any(LocalDateTime.class)
                        )
        ).thenReturn(1L);

        ActiveStaffSummaryResponse response =
                employeeShiftService.getActiveStaffSummary();

        assertEquals(
                2L,
                response.getWaiters()
        );

        assertEquals(
                3L,
                response.getKitchenEmployees()
        );

        assertEquals(
                1L,
                response.getBarEmployees()
        );

        assertEquals(
                6L,
                response.getTotalOperationalEmployees()
        );
    }

    @Test
    void roleChangeShouldFinishActiveAndPlannedShifts() {
        User manager =
                createUser(
                        22L,
                        "Manager Test",
                        "manager@test.com",
                        RoleType.MANAGER,
                        true
                );

        User employee =
                createUser(
                        23L,
                        "Angajat Test",
                        "employee@test.com",
                        RoleType.KITCHEN,
                        true
                );

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift activeShift =
                createActiveShift(
                        60L,
                        employee,
                        manager,
                        now.minusHours(2),
                        now.plusHours(6),
                        ShiftStartSource.SCHEDULED_LOGIN
                );

        EmployeeShift plannedShift =
                createPlannedShift(
                        61L,
                        employee,
                        manager,
                        now.plusDays(1),
                        now.plusDays(1).plusHours(8)
                );

        when(
                employeeShiftRepository
                        .findAllOpenByEmployeeIdForUpdate(23L)
        ).thenReturn(
                List.of(
                        activeShift,
                        plannedShift
                )
        );

        int processedCount =
                employeeShiftService
                        .closeOpenShiftsForRoleChange(23L);

        assertEquals(
                2,
                processedCount
        );

        assertTrue(activeShift.isClosed());
        assertTrue(plannedShift.isClosed());

        assertEquals(
                ShiftEndReason.ROLE_CHANGED,
                activeShift.getEndReason()
        );

        assertEquals(
                ShiftEndReason.ROLE_CHANGED,
                plannedShift.getEndReason()
        );

        verify(employeeShiftRepository)
                .saveAll(
                        List.of(
                                activeShift,
                                plannedShift
                        )
                );
    }

    private User createUser(
            Long id,
            String fullName,
            String email,
            RoleType roleType,
            boolean active
    ) {
        Role role =
                new Role(roleType);

        ReflectionTestUtils.setField(
                role,
                "id",
                id + 100
        );

        User user =
                new User(
                        fullName,
                        email,
                        "encoded-password",
                        role
                );

        ReflectionTestUtils.setField(
                user,
                "id",
                id
        );

        user.setActive(active);

        return user;
    }

    private EmployeeShift createPlannedShift(
            Long shiftId,
            User employee,
            User createdBy,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt
    ) {
        EmployeeShift shift =
                new EmployeeShift(
                        employee,
                        employee.getRole().getName(),
                        plannedStartAt,
                        plannedEndAt,
                        createdBy
                );

        ReflectionTestUtils.setField(
                shift,
                "id",
                shiftId
        );

        return shift;
    }

    private EmployeeShift createActiveShift(
            Long shiftId,
            User employee,
            User createdBy,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt,
            ShiftStartSource startSource
    ) {
        EmployeeShift shift =
                createPlannedShift(
                        shiftId,
                        employee,
                        createdBy,
                        plannedStartAt,
                        plannedEndAt
                );

        shift.start(
                plannedStartAt,
                startSource,
                employee
        );

        return shift;
    }
}