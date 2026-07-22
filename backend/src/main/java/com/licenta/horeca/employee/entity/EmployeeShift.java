package com.licenta.horeca.employee.entity;

import com.licenta.horeca.entity.User;
import com.licenta.horeca.enums.RoleType;
import com.licenta.horeca.employee.enums.ShiftEndReason;
import com.licenta.horeca.employee.enums.ShiftStartSource;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "employee_shifts",
        indexes = {
                @Index(
                        name = "idx_employee_shifts_employee",
                        columnList = "employee_id"
                ),
                @Index(
                        name = "idx_employee_shifts_role",
                        columnList = "shift_role"
                ),
                @Index(
                        name = "idx_employee_shifts_planned_end",
                        columnList = "planned_end_at"
                ),
                @Index(
                        name = "idx_employee_shifts_ended_at",
                        columnList = "ended_at"
                )
        }
)
public class EmployeeShift {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Angajatul căruia îi aparține tura.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            updatable = false
    )
    private User employee;

    /*
     * Rolul angajatului la momentul planificării turei.
     *
     * Este păstrat separat pentru ca istoricul să nu se
     * modifice dacă utilizatorul primește ulterior alt rol.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "shift_role",
            nullable = false,
            updatable = false,
            length = 20
    )
    private RoleType shiftRole;

    /*
     * Intervalul planificat al turei.
     */
    @Column(
            name = "planned_start_at",
            nullable = false
    )
    private LocalDateTime plannedStartAt;

    @Column(
            name = "planned_end_at",
            nullable = false
    )
    private LocalDateTime plannedEndAt;

    /*
     * Momentul real în care tura a început.
     *
     * Rămâne null cât timp tura este doar planificată.
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /*
     * Momentul închiderii, anulării sau marcării
     * turei ca absentă.
     */
    @Column(name = "ended_at")
    private LocalDateTime endedAt;

    /*
     * Modul în care tura a început.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "start_source",
            length = 30
    )
    private ShiftStartSource startSource;

    /*
     * Motivul pentru care tura a fost finalizată.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "end_reason",
            length = 30
    )
    private ShiftEndReason endReason;

    /*
     * Utilizatorul care a creat programarea.
     *
     * Pentru o tură neplanificată creată automat,
     * acesta poate fi chiar angajatul.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "created_by_user_id",
            nullable = false,
            updatable = false
    )
    private User createdBy;

    /*
     * Utilizatorul care a pornit efectiv tura.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "started_by_user_id")
    private User startedBy;

    /*
     * Utilizatorul care a închis sau anulat tura.
     *
     * Poate rămâne null pentru acțiunile automate.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ended_by_user_id")
    private User endedBy;

    public EmployeeShift() {
    }

    public EmployeeShift(
            User employee,
            RoleType shiftRole,
            LocalDateTime plannedStartAt,
            LocalDateTime plannedEndAt,
            User createdBy
    ) {
        if (employee == null) {
            throw new IllegalArgumentException(
                    "Angajatul este obligatoriu."
            );
        }

        if (shiftRole == null) {
            throw new IllegalArgumentException(
                    "Rolul turei este obligatoriu."
            );
        }

        if (createdBy == null) {
            throw new IllegalArgumentException(
                    "Utilizatorul care creeaza tura este obligatoriu."
            );
        }

        validateInterval(
                plannedStartAt,
                plannedEndAt
        );

        this.employee = employee;
        this.shiftRole = shiftRole;
        this.plannedStartAt = plannedStartAt;
        this.plannedEndAt = plannedEndAt;
        this.createdBy = createdBy;
    }

    public Long getId() {
        return id;
    }

    public User getEmployee() {
        return employee;
    }

    public RoleType getShiftRole() {
        return shiftRole;
    }

    public LocalDateTime getPlannedStartAt() {
        return plannedStartAt;
    }

    public LocalDateTime getPlannedEndAt() {
        return plannedEndAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public ShiftStartSource getStartSource() {
        return startSource;
    }

    public ShiftEndReason getEndReason() {
        return endReason;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public User getStartedBy() {
        return startedBy;
    }

    public User getEndedBy() {
        return endedBy;
    }

    /*
     * Tura a fost programată, dar nu a început și nu a fost anulată.
     */
    public boolean isPlanned() {
        return startedAt == null
                && endedAt == null;
    }

    /*
     * Tura a început, dar nu a fost încă finalizată.
     */
    public boolean isActive() {
        return startedAt != null
                && endedAt == null;
    }

    public boolean isClosed() {
        return endedAt != null;
    }

    /*
     * Pornește efectiv o tură planificată.
     */
    public void start(
            LocalDateTime startedAt,
            ShiftStartSource startSource,
            User startedBy
    ) {
        if (!isPlanned()) {
            throw new IllegalStateException(
                    "Tura nu mai poate fi pornita."
            );
        }

        if (startedAt == null
                || startSource == null
                || startedBy == null) {
            throw new IllegalArgumentException(
                    "Datele de pornire ale turei sunt incomplete."
            );
        }

        this.startedAt = startedAt;
        this.startSource = startSource;
        this.startedBy = startedBy;
    }

    /*
     * Închide o tură aflată în desfășurare.
     */
    public void close(
            LocalDateTime endedAt,
            User endedBy,
            ShiftEndReason endReason
    ) {
        if (!isActive()) {
            throw new IllegalStateException(
                    "Tura nu este activa."
            );
        }

        if (endedAt == null
                || endedAt.isBefore(startedAt)) {
            throw new IllegalArgumentException(
                    "Momentul inchiderii turei este invalid."
            );
        }

        if (endReason == null) {
            throw new IllegalArgumentException(
                    "Motivul inchiderii este obligatoriu."
            );
        }

        this.endedAt = endedAt;
        this.endedBy = endedBy;
        this.endReason = endReason;
    }

    /*
     * Modifică intervalul unei ture care nu a început.
     */
    public void reschedule(
            LocalDateTime newPlannedStartAt,
            LocalDateTime newPlannedEndAt
    ) {
        if (!isPlanned()) {
            throw new IllegalStateException(
                    "Doar o tura planificata poate fi modificata."
            );
        }

        validateInterval(
                newPlannedStartAt,
                newPlannedEndAt
        );

        this.plannedStartAt =
                newPlannedStartAt;

        this.plannedEndAt =
                newPlannedEndAt;
    }

    /*
     * Anulează manual o tură planificată.
     */
    public void cancel(
            LocalDateTime cancelledAt,
            User cancelledBy
    ) {
        cancel(
                cancelledAt,
                cancelledBy,
                ShiftEndReason.CANCELLED
        );
    }

    /*
     * Finalizează o tură neîncepută cu un motiv explicit.
     *
     * Este folosită atât pentru anularea manuală, cât și
     * pentru dezactivarea contului sau schimbarea rolului.
     */
    public void cancel(
            LocalDateTime cancelledAt,
            User cancelledBy,
            ShiftEndReason cancellationReason
    ) {
        if (!isPlanned()) {
            throw new IllegalStateException(
                    "Doar o tura planificata poate fi anulata."
            );
        }

        if (cancelledAt == null) {
            throw new IllegalArgumentException(
                    "Momentul anularii este obligatoriu."
            );
        }

        if (cancellationReason == null) {
            throw new IllegalArgumentException(
                    "Motivul anularii este obligatoriu."
            );
        }

        boolean allowedReason =
                cancellationReason
                        == ShiftEndReason.CANCELLED
                        || cancellationReason
                        == ShiftEndReason.ACCOUNT_DEACTIVATED
                        || cancellationReason
                        == ShiftEndReason.ROLE_CHANGED;

        if (!allowedReason) {
            throw new IllegalArgumentException(
                    "Motivul nu este valid pentru anularea unei ture planificate."
            );
        }

        if (cancellationReason == ShiftEndReason.CANCELLED
                && cancelledBy == null) {
            throw new IllegalArgumentException(
                    "Utilizatorul care anuleaza tura este obligatoriu."
            );
        }

        this.endedAt = cancelledAt;
        this.endedBy = cancelledBy;
        this.endReason = cancellationReason;
    }

    /*
     * Marchează o tură planificată ca absentă atunci când
     * angajatul nu s-a autentificat până la finalul programului.
     */
    public void markMissed(
            LocalDateTime processedAt
    ) {
        if (!isPlanned()) {
            throw new IllegalStateException(
                    "Doar o tura neinceputa poate fi marcata ca absenta."
            );
        }

        if (processedAt == null
                || processedAt.isBefore(plannedEndAt)) {
            throw new IllegalArgumentException(
                    "Tura nu poate fi marcata ca absenta inainte de finalul planificat."
            );
        }

        this.endedAt = plannedEndAt;
        this.endedBy = null;
        this.endReason = ShiftEndReason.MISSED;
    }

    private void validateInterval(
            LocalDateTime startAt,
            LocalDateTime endAt
    ) {
        if (startAt == null
                || endAt == null) {
            throw new IllegalArgumentException(
                    "Intervalul planificat este obligatoriu."
            );
        }

        if (!endAt.isAfter(startAt)) {
            throw new IllegalArgumentException(
                    "Ora de final trebuie sa fie ulterioara orei de inceput."
            );
        }
    }
}