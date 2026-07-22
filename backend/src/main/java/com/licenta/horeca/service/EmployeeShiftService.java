package com.licenta.horeca.service;

import com.licenta.horeca.dto.shift.ActiveStaffSummaryResponse;
import com.licenta.horeca.dto.shift.EmployeeShiftResponse;
import com.licenta.horeca.entity.EmployeeShift;
import com.licenta.horeca.entity.User;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.enums.ShiftEndReason;
import com.licenta.horeca.enums.ShiftStartSource;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.repository.EmployeeShiftRepository;
import com.licenta.horeca.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmployeeShiftService {

    private static final Set<RoleType> OPERATIONAL_ROLES =
            EnumSet.of(
                    RoleType.WAITER,
                    RoleType.KITCHEN,
                    RoleType.BAR
            );

    private static final Set<RoleType> SHIFT_OPERATOR_ROLES =
            EnumSet.of(
                    RoleType.MANAGER,
                    RoleType.ADMIN
            );

    private final EmployeeShiftRepository employeeShiftRepository;

    private final UserRepository userRepository;

    private final long unscheduledMaxHours;

    private final long maximumDurationHours;

    private final long earlyLoginMinutes;

    public EmployeeShiftService(
            EmployeeShiftRepository employeeShiftRepository,
            UserRepository userRepository,
            @Value("${app.shifts.unscheduled-max-hours:16}")
            long unscheduledMaxHours,
            @Value("${app.shifts.maximum-duration-hours:16}")
            long maximumDurationHours,
            @Value("${app.shifts.early-login-minutes:60}")
            long earlyLoginMinutes
    ) {
        if (unscheduledMaxHours <= 0
                || maximumDurationHours <= 0
                || earlyLoginMinutes < 0) {
            throw new IllegalArgumentException(
                    "Configuratia turelor este invalida."
            );
        }

        this.employeeShiftRepository =
                employeeShiftRepository;

        this.userRepository =
                userRepository;

        this.unscheduledMaxHours =
                unscheduledMaxHours;

        this.maximumDurationHours =
                maximumDurationHours;

        this.earlyLoginMinutes =
                earlyLoginMinutes;
    }

    /*
     * Garantează automat existența unei ture pentru
     * un angajat operațional autentificat.
     */
    @Transactional
    public void ensureShiftForLogin(
            Long employeeId
    ) {
        User employee =
                userRepository
                        .findByIdForUpdate(employeeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Angajatul nu exista."
                                )
                        );

        if (!employee.isActive()) {
            throw new BusinessException(
                    "Tura nu poate fi pornita pentru un cont dezactivat."
            );
        }

        RoleType employeeRole =
                employee.getRole().getName();

        if (!OPERATIONAL_ROLES.contains(employeeRole)) {
            return;
        }

        LocalDateTime now =
                LocalDateTime.now();

        Optional<EmployeeShift> activeShift =
                employeeShiftRepository
                        .findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
                                employeeId
                        );

        if (activeShift.isPresent()) {
            EmployeeShift shift =
                    activeShift.get();

            if (shift.getPlannedEndAt().isAfter(now)) {
                return;
            }

            closeAutomatically(shift);

            employeeShiftRepository.save(shift);
        }

        LocalDateTime latestAllowedStart =
                now.plusMinutes(earlyLoginMinutes);

        Optional<EmployeeShift> plannedShift =
                employeeShiftRepository
                        .findFirstByEmployeeIdAndStartedAtIsNullAndEndedAtIsNullAndPlannedStartAtLessThanEqualAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
                                employeeId,
                                latestAllowedStart,
                                now
                        );

        if (plannedShift.isPresent()) {
            EmployeeShift shift =
                    plannedShift.get();

            shift.start(
                    now,
                    ShiftStartSource.SCHEDULED_LOGIN,
                    employee
            );

            employeeShiftRepository.save(shift);

            return;
        }

        /*
         * Dacă managerul nu a creat programarea, angajatul
         * nu este blocat. Se creează o tură neplanificată.
         */
        EmployeeShift unscheduledShift =
                new EmployeeShift(
                        employee,
                        employeeRole,
                        now,
                        now.plusHours(unscheduledMaxHours),
                        employee
                );

        unscheduledShift.start(
                now,
                ShiftStartSource.UNSCHEDULED_LOGIN,
                employee
        );

        employeeShiftRepository.save(
                unscheduledShift
        );
    }

    /*
     * Creează o programare flexibilă.
     */
    @Transactional
    public EmployeeShiftResponse createPlannedShift(
            Long employeeId,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt,
            String operatorEmail
    ) {
        User operator =
                findAndValidateOperator(
                        operatorEmail
                );

        User employee =
                userRepository
                        .findByIdForUpdate(employeeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Angajatul nu exista."
                                )
                        );

        validateEmployeeForShift(employee);

        validatePlannedInterval(
                plannedStartAt,
                plannedEndAt
        );

        boolean overlappingShift =
                employeeShiftRepository
                        .existsByEmployeeIdAndEndedAtIsNullAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
                                employeeId,
                                plannedEndAt,
                                plannedStartAt
                        );

        if (overlappingShift) {
            throw new BusinessException(
                    "Angajatul are deja o tura care se suprapune cu intervalul ales."
            );
        }

        EmployeeShift shift =
                new EmployeeShift(
                        employee,
                        employee.getRole().getName(),
                        plannedStartAt,
                        plannedEndAt,
                        operator
                );

        return mapToResponse(
                employeeShiftRepository.save(shift)
        );
    }

    /*
     * Pornește manual o tură neplanificată.
     */
    @Transactional
    public EmployeeShiftResponse startShift(
            Long employeeId,
            String operatorEmail
    ) {
        User operator =
                findAndValidateOperator(
                        operatorEmail
                );

        User employee =
                userRepository
                        .findByIdForUpdate(employeeId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Angajatul nu exista."
                                )
                        );

        validateEmployeeForShift(employee);

        Optional<EmployeeShift> existingShift =
                employeeShiftRepository
                        .findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
                                employeeId
                        );

        if (existingShift.isPresent()) {
            throw new BusinessException(
                    "Angajatul are deja o tura activa."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        EmployeeShift shift =
                new EmployeeShift(
                        employee,
                        employee.getRole().getName(),
                        now,
                        now.plusHours(unscheduledMaxHours),
                        operator
                );

        shift.start(
                now,
                ShiftStartSource.MANUAL_MANAGER,
                operator
        );

        return mapToResponse(
                employeeShiftRepository.save(shift)
        );
    }

    /*
     * Închide manual o tură activă.
     */
    @Transactional
    public EmployeeShiftResponse closeShift(
            Long shiftId,
            String operatorEmail
    ) {
        User operator =
                findAndValidateOperator(
                        operatorEmail
                );

        EmployeeShift shift =
                employeeShiftRepository
                        .findByIdForUpdate(shiftId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Tura nu exista."
                                )
                        );

        if (!shift.isActive()) {
            throw new BusinessException(
                    "Tura nu este activa."
            );
        }

        shift.close(
                LocalDateTime.now(),
                operator,
                ShiftEndReason.MANUAL_MANAGER
        );

        return mapToResponse(
                employeeShiftRepository.save(shift)
        );
    }

    /*
     * Modifică intervalul unei programări.
     */
    @Transactional
    public EmployeeShiftResponse updatePlannedShift(
            Long shiftId,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt,
            String operatorEmail
    ) {
        findAndValidateOperator(
                operatorEmail
        );

        EmployeeShift shift =
                employeeShiftRepository
                        .findByIdForUpdate(shiftId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Tura nu exista."
                                )
                        );

        if (!shift.isPlanned()) {
            throw new BusinessException(
                    "Doar o tura care nu a inceput poate fi modificata."
            );
        }

        validatePlannedInterval(
                plannedStartAt,
                plannedEndAt
        );

        long overlappingShiftCount =
                employeeShiftRepository
                        .countOverlappingShiftsExcludingId(
                                shift.getEmployee().getId(),
                                shift.getId(),
                                plannedStartAt,
                                plannedEndAt
                        );

        if (overlappingShiftCount > 0) {
            throw new BusinessException(
                    "Angajatul are deja o alta tura care se suprapune cu intervalul ales."
            );
        }

        shift.reschedule(
                plannedStartAt,
                plannedEndAt
        );

        return mapToResponse(
                employeeShiftRepository.save(shift)
        );
    }

    /*
     * Anulează o programare, fără să o șteargă.
     */
    @Transactional
    public EmployeeShiftResponse cancelPlannedShift(
            Long shiftId,
            String operatorEmail
    ) {
        User operator =
                findAndValidateOperator(
                        operatorEmail
                );

        EmployeeShift shift =
                employeeShiftRepository
                        .findByIdForUpdate(shiftId)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Tura nu exista."
                                )
                        );

        if (!shift.isPlanned()) {
            throw new BusinessException(
                    "Doar o tura care nu a inceput poate fi anulata."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (!shift.getPlannedEndAt().isAfter(now)) {
            throw new BusinessException(
                    "Tura a ajuns deja la final si nu mai poate fi anulata."
            );
        }

        shift.cancel(
                now,
                operator
        );

        return mapToResponse(
                employeeShiftRepository.save(shift)
        );
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getActiveShifts() {
        LocalDateTime now =
                LocalDateTime.now();

        return employeeShiftRepository
                .findAllByStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfterOrderByStartedAtAsc(
                        now
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getPlannedShifts() {
        LocalDateTime now =
                LocalDateTime.now();

        return employeeShiftRepository
                .findAllByStartedAtIsNullAndEndedAtIsNullAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
                        now
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EmployeeShiftResponse> getEmployeeShiftHistory(
            Long employeeId
    ) {
        if (!userRepository.existsById(employeeId)) {
            throw new BusinessException(
                    "Angajatul nu exista."
            );
        }

        return employeeShiftRepository
                .findAllByEmployeeIdOrderByPlannedStartAtDesc(
                        employeeId
                )
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ActiveStaffSummaryResponse getActiveStaffSummary() {
        LocalDateTime now =
                LocalDateTime.now();

        long waiters =
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                RoleType.WAITER,
                                now
                        );

        long kitchenEmployees =
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                RoleType.KITCHEN,
                                now
                        );

        long barEmployees =
                employeeShiftRepository
                        .countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
                                RoleType.BAR,
                                now
                        );

        return new ActiveStaffSummaryResponse(
                waiters,
                kitchenEmployees,
                barEmployees
        );
    }

    /*
     * Închide turele expirate și marchează absențele.
     */
    @Transactional
    public int cleanupExpiredShifts() {
        LocalDateTime now =
                LocalDateTime.now();

        List<EmployeeShift> expiredActiveShifts =
                employeeShiftRepository
                        .findExpiredActiveShiftsForUpdate(now);

        for (EmployeeShift shift : expiredActiveShifts) {
            closeAutomatically(shift);
        }

        List<EmployeeShift> missedPlannedShifts =
                employeeShiftRepository
                        .findMissedPlannedShiftsForUpdate(now);

        for (EmployeeShift shift : missedPlannedShifts) {
            shift.markMissed(now);
        }

        employeeShiftRepository.saveAll(
                expiredActiveShifts
        );

        employeeShiftRepository.saveAll(
                missedPlannedShifts
        );

        return expiredActiveShifts.size()
                + missedPlannedShifts.size();
    }

    /*
     * Închide sau anulează turele atunci când contul
     * angajatului este dezactivat.
     */
    @Transactional
    public int closeOpenShiftsForAccountDeactivation(
            Long employeeId
    ) {
        return finishOpenShifts(
                employeeId,
                ShiftEndReason.ACCOUNT_DEACTIVATED
        );
    }

    /*
     * Închide sau anulează turele atunci când rolul
     * angajatului este schimbat.
     */
    @Transactional
    public int closeOpenShiftsForRoleChange(
            Long employeeId
    ) {
        return finishOpenShifts(
                employeeId,
                ShiftEndReason.ROLE_CHANGED
        );
    }

    private int finishOpenShifts(
            Long employeeId,
            ShiftEndReason endReason
    ) {
        List<EmployeeShift> openShifts =
                employeeShiftRepository
                        .findAllOpenByEmployeeIdForUpdate(
                                employeeId
                        );

        if (openShifts.isEmpty()) {
            return 0;
        }

        LocalDateTime now =
                LocalDateTime.now();

        int processedShiftCount = 0;

        for (EmployeeShift shift : openShifts) {
            if (shift.isActive()) {
                shift.close(
                        now,
                        null,
                        endReason
                );

                processedShiftCount++;

            } else if (shift.isPlanned()) {
                shift.cancel(
                        now,
                        null,
                        endReason
                );

                processedShiftCount++;
            }
        }

        employeeShiftRepository.saveAll(
                openShifts
        );

        return processedShiftCount;
    }

    private void closeAutomatically(
            EmployeeShift shift
    ) {
        ShiftEndReason endReason =
                shift.getStartSource()
                        == ShiftStartSource.SCHEDULED_LOGIN
                        ? ShiftEndReason.AUTO_PLANNED_END
                        : ShiftEndReason.AUTO_SAFETY_LIMIT;

        shift.close(
                shift.getPlannedEndAt(),
                null,
                endReason
        );
    }

    private void validatePlannedInterval(
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt
    ) {
        if (plannedStartAt == null
                || plannedEndAt == null) {
            throw new BusinessException(
                    "Intervalul turei este obligatoriu."
            );
        }

        if (!plannedEndAt.isAfter(plannedStartAt)) {
            throw new BusinessException(
                    "Ora de final trebuie sa fie ulterioara orei de inceput."
            );
        }

        LocalDateTime now =
                LocalDateTime.now();

        if (!plannedEndAt.isAfter(now)) {
            throw new BusinessException(
                    "Ora de final a turei trebuie sa fie in viitor."
            );
        }

        long durationMinutes =
                Duration.between(
                        plannedStartAt,
                        plannedEndAt
                ).toMinutes();

        if (durationMinutes
                > maximumDurationHours * 60) {
            throw new BusinessException(
                    "O tura nu poate depasi "
                            + maximumDurationHours
                            + " ore."
            );
        }
    }

    private User findAndValidateOperator(
            String operatorEmail
    ) {
        if (operatorEmail == null
                || operatorEmail.isBlank()) {
            throw new BusinessException(
                    "Utilizatorul autentificat nu a putut fi identificat."
            );
        }

        User operator =
                userRepository
                        .findByEmail(operatorEmail)
                        .orElseThrow(() ->
                                new BusinessException(
                                        "Utilizatorul autentificat nu exista."
                                )
                        );

        if (!operator.isActive()) {
            throw new BusinessException(
                    "Contul utilizatorului este dezactivat."
            );
        }

        if (!SHIFT_OPERATOR_ROLES.contains(
                operator.getRole().getName()
        )) {
            throw new BusinessException(
                    "Nu aveti permisiunea sa gestionati ture."
            );
        }

        return operator;
    }

    private void validateEmployeeForShift(
            User employee
    ) {
        if (!employee.isActive()) {
            throw new BusinessException(
                    "Tura nu poate fi pornita pentru un cont dezactivat."
            );
        }

        if (!OPERATIONAL_ROLES.contains(
                employee.getRole().getName()
        )) {
            throw new BusinessException(
                    "Doar ospatarii si angajatii din bucatarie sau bar pot avea ture operationale."
            );
        }
    }

    private EmployeeShiftResponse mapToResponse(
            EmployeeShift shift
    ) {
        EmployeeShiftResponse response =
                new EmployeeShiftResponse();

        response.setId(
                shift.getId()
        );

        response.setEmployeeId(
                shift.getEmployee().getId()
        );

        response.setEmployeeName(
                shift.getEmployee().getFullName()
        );

        response.setShiftRole(
                shift.getShiftRole()
        );

        response.setPlannedStartAt(
                shift.getPlannedStartAt()
        );

        response.setPlannedEndAt(
                shift.getPlannedEndAt()
        );

        response.setStartedAt(
                shift.getStartedAt()
        );

        response.setEndedAt(
                shift.getEndedAt()
        );

        response.setStartSource(
                shift.getStartSource()
        );

        response.setEndReason(
                shift.getEndReason()
        );

        response.setOpen(
                shift.isActive()
        );

        response.setCreatedByUserId(
                shift.getCreatedBy().getId()
        );

        response.setCreatedByName(
                shift.getCreatedBy().getFullName()
        );

        if (shift.getStartedAt() != null) {
            LocalDateTime durationEnd =
                    shift.getEndedAt() == null
                            ? LocalDateTime.now()
                            : shift.getEndedAt();

            long durationMinutes =
                    Duration.between(
                            shift.getStartedAt(),
                            durationEnd
                    ).toMinutes();

            response.setDurationMinutes(
                    Math.max(durationMinutes, 0)
            );

        } else {
            response.setDurationMinutes(0L);
        }

        if (shift.getStartedBy() != null) {
            response.setStartedByUserId(
                    shift.getStartedBy().getId()
            );

            response.setStartedByName(
                    shift.getStartedBy().getFullName()
            );
        }

        if (shift.getEndedBy() != null) {
            response.setEndedByUserId(
                    shift.getEndedBy().getId()
            );

            response.setEndedByName(
                    shift.getEndedBy().getFullName()
            );
        }

        return response;
    }
}