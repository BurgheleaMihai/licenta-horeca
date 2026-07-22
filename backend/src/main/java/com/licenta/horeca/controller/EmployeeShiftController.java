package com.licenta.horeca.controller;

import com.licenta.horeca.dto.shift.ActiveStaffSummaryResponse;
import com.licenta.horeca.dto.shift.EmployeeShiftResponse;
import com.licenta.horeca.dto.shift.StartEmployeeShiftRequest;
import com.licenta.horeca.service.EmployeeShiftService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.licenta.horeca.dto.shift.UpdatePlannedShiftRequest;

@RestController
@RequestMapping("/api/employee-shifts")
public class EmployeeShiftController {

    private final EmployeeShiftService employeeShiftService;

    public EmployeeShiftController(
            EmployeeShiftService employeeShiftService
    ) {
        this.employeeShiftService = employeeShiftService;
    }

    /*
     * Pornește o tură pentru un angajat operațional.
     *
     * Utilizatorul care efectuează acțiunea este identificat
     * din tokenul JWT, nu din date trimise de frontend.
     */
    @PostMapping
    public ResponseEntity<EmployeeShiftResponse> startShift(
            @Valid @RequestBody StartEmployeeShiftRequest request,
            Principal principal
    ) {
        EmployeeShiftResponse response =
                employeeShiftService.startShift(
                        request.getEmployeeId(),
                        principal.getName()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /*
     * Închide o tură existentă.
     */
    @PutMapping("/{shiftId}/close")
    public ResponseEntity<EmployeeShiftResponse> closeShift(
            @PathVariable Long shiftId,
            Principal principal
    ) {
        EmployeeShiftResponse response =
                employeeShiftService.closeShift(
                        shiftId,
                        principal.getName()
                );

        return ResponseEntity.ok(response);
    }

    /*
     * Returnează toate turele care sunt încă deschise.
     */
    @GetMapping("/active")
    public ResponseEntity<List<EmployeeShiftResponse>>
    getActiveShifts() {

        return ResponseEntity.ok(
                employeeShiftService.getActiveShifts()
        );
    }

    /*
     * Returnează istoricul turelor unui angajat.
     */
    @GetMapping("/employee/{employeeId}")
    public ResponseEntity<List<EmployeeShiftResponse>>
    getEmployeeShiftHistory(
            @PathVariable Long employeeId
    ) {
        return ResponseEntity.ok(
                employeeShiftService
                        .getEmployeeShiftHistory(employeeId)
        );
    }

    /*
     * Returnează numărul angajaților aflați în tură,
     * grupați după rolul operațional.
     *
     * Acest endpoint va putea fi folosit ulterior
     * și de sistemul decizional.
     */
    @GetMapping("/summary")
    public ResponseEntity<ActiveStaffSummaryResponse>
    getActiveStaffSummary() {

        return ResponseEntity.ok(
                employeeShiftService.getActiveStaffSummary()
        );
    }

    /*
     * Modifica intervalul unei ture planificate.
     */
    @PutMapping("/{shiftId}/planned")
    public ResponseEntity<EmployeeShiftResponse>
    updatePlannedShift(
            @PathVariable Long shiftId,
            @Valid
            @RequestBody
            UpdatePlannedShiftRequest request,
            Principal principal
    ) {
        EmployeeShiftResponse response =
                employeeShiftService
                        .updatePlannedShift(
                                shiftId,
                                request.getPlannedStartAt(),
                                request.getPlannedEndAt(),
                                principal.getName()
                        );

        return ResponseEntity.ok(response);
    }

    /*
     * Anuleaza o tura planificata.
     *
     * Folosim PUT, nu DELETE, deoarece tura ramane
     * in baza de date cu motivul CANCELLED.
     */
    @PutMapping("/{shiftId}/cancel")
    public ResponseEntity<EmployeeShiftResponse>
    cancelPlannedShift(
            @PathVariable Long shiftId,
            Principal principal
    ) {
        EmployeeShiftResponse response =
                employeeShiftService
                        .cancelPlannedShift(
                                shiftId,
                                principal.getName()
                        );

        return ResponseEntity.ok(response);
    }
}