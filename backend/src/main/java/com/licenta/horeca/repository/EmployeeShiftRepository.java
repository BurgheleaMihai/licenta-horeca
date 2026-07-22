package com.licenta.horeca.repository;

import com.licenta.horeca.entity.EmployeeShift;
import com.licenta.horeca.enums.RoleType;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmployeeShiftRepository
        extends JpaRepository<EmployeeShift, Long> {

    /*
     * Caută tura activă a unui angajat.
     */
    @EntityGraph(
            attributePaths = {
                    "employee",
                    "createdBy",
                    "startedBy",
                    "endedBy"
            }
    )
    Optional<EmployeeShift>
    findByEmployeeIdAndStartedAtIsNotNullAndEndedAtIsNull(
            Long employeeId
    );

    /*
     * Caută prima tură planificată care poate fi pornită
     * prin autentificarea angajatului.
     */
    @EntityGraph(
            attributePaths = {
                    "employee",
                    "createdBy",
                    "startedBy",
                    "endedBy"
            }
    )
    Optional<EmployeeShift>
    findFirstByEmployeeIdAndStartedAtIsNullAndEndedAtIsNullAndPlannedStartAtLessThanEqualAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
            Long employeeId,
            LocalDateTime latestAllowedStart,
            LocalDateTime currentMoment
    );

    /*
     * Verifică suprapunerea unei programări noi cu turele
     * nefinalizate ale aceluiași angajat.
     */
    boolean
    existsByEmployeeIdAndEndedAtIsNullAndPlannedStartAtLessThanAndPlannedEndAtGreaterThan(
            Long employeeId,
            LocalDateTime newPlannedEnd,
            LocalDateTime newPlannedStart
    );

    /*
     * Returnează turele aflate efectiv în desfășurare.
     */
    @EntityGraph(
            attributePaths = {
                    "employee",
                    "createdBy",
                    "startedBy",
                    "endedBy"
            }
    )
    List<EmployeeShift>
    findAllByStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfterOrderByStartedAtAsc(
            LocalDateTime currentMoment
    );

    /*
     * Returnează programările viitoare sau aflate încă
     * în interval, dar care nu au început.
     */
    @EntityGraph(
            attributePaths = {
                    "employee",
                    "createdBy",
                    "startedBy",
                    "endedBy"
            }
    )
    List<EmployeeShift>
    findAllByStartedAtIsNullAndEndedAtIsNullAndPlannedEndAtAfterOrderByPlannedStartAtAsc(
            LocalDateTime currentMoment
    );

    /*
     * Returnează istoricul complet al unui angajat.
     */
    @EntityGraph(
            attributePaths = {
                    "employee",
                    "createdBy",
                    "startedBy",
                    "endedBy"
            }
    )
    List<EmployeeShift>
    findAllByEmployeeIdOrderByPlannedStartAtDesc(
            Long employeeId
    );

    /*
     * Numără angajații aflați efectiv în tură pentru un rol.
     */
    long
    countByShiftRoleAndStartedAtIsNotNullAndEndedAtIsNullAndPlannedEndAtAfter(
            RoleType shiftRole,
            LocalDateTime currentMoment
    );

    /*
     * Citește și blochează o tură pentru actualizare.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shift
            FROM EmployeeShift shift
            JOIN FETCH shift.employee
            JOIN FETCH shift.createdBy
            LEFT JOIN FETCH shift.startedBy
            LEFT JOIN FETCH shift.endedBy
            WHERE shift.id = :id
            """)
    Optional<EmployeeShift> findByIdForUpdate(
            @Param("id")
            Long id
    );

    /*
     * Returnează și blochează turele active care au ajuns
     * la finalul planificat.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shift
            FROM EmployeeShift shift
            JOIN FETCH shift.employee
            JOIN FETCH shift.createdBy
            LEFT JOIN FETCH shift.startedBy
            LEFT JOIN FETCH shift.endedBy
            WHERE shift.startedAt IS NOT NULL
              AND shift.endedAt IS NULL
              AND shift.plannedEndAt <= :moment
            ORDER BY shift.plannedEndAt ASC
            """)
    List<EmployeeShift> findExpiredActiveShiftsForUpdate(
            @Param("moment")
            LocalDateTime moment
    );

    /*
     * Returnează și blochează programările încheiate
     * la care angajatul nu s-a prezentat.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shift
            FROM EmployeeShift shift
            JOIN FETCH shift.employee
            JOIN FETCH shift.createdBy
            LEFT JOIN FETCH shift.startedBy
            LEFT JOIN FETCH shift.endedBy
            WHERE shift.startedAt IS NULL
              AND shift.endedAt IS NULL
              AND shift.plannedEndAt <= :moment
            ORDER BY shift.plannedEndAt ASC
            """)
    List<EmployeeShift> findMissedPlannedShiftsForUpdate(
            @Param("moment")
            LocalDateTime moment
    );

    /*
     * Verifică suprapunerea la modificarea unei programări,
     * fără să compare tura cu propria înregistrare.
     */
    @Query("""
            SELECT COUNT(shift)
            FROM EmployeeShift shift
            WHERE shift.employee.id = :employeeId
              AND shift.id <> :excludedShiftId
              AND shift.endedAt IS NULL
              AND shift.plannedStartAt < :newPlannedEndAt
              AND shift.plannedEndAt > :newPlannedStartAt
            """)
    long countOverlappingShiftsExcludingId(
            @Param("employeeId")
            Long employeeId,

            @Param("excludedShiftId")
            Long excludedShiftId,

            @Param("newPlannedStartAt")
            LocalDateTime newPlannedStartAt,

            @Param("newPlannedEndAt")
            LocalDateTime newPlannedEndAt
    );

    /*
     * Blochează toate turele nefinalizate ale unui angajat.
     *
     * Sunt incluse tura activă și programările viitoare.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT shift
            FROM EmployeeShift shift
            WHERE shift.employee.id = :employeeId
              AND shift.endedAt IS NULL
            ORDER BY shift.plannedStartAt ASC
            """)
    List<EmployeeShift> findAllOpenByEmployeeIdForUpdate(
            @Param("employeeId")
            Long employeeId
    );
}