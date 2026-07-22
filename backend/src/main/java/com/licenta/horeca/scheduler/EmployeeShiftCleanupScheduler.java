package com.licenta.horeca.scheduler;

import com.licenta.horeca.service.EmployeeShiftService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class EmployeeShiftCleanupScheduler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(
                    EmployeeShiftCleanupScheduler.class
            );

    private final EmployeeShiftService employeeShiftService;

    public EmployeeShiftCleanupScheduler(
            EmployeeShiftService employeeShiftService
    ) {
        this.employeeShiftService =
                employeeShiftService;
    }

    /*
     * Verifica periodic turele.
     *
     * Nu afiseaza nimic atunci cand nu exista ture de procesat,
     * pentru a pastra terminalul backend curat.
     */
    @Scheduled(
            fixedDelayString =
                    "${app.shifts.cleanup-delay-ms:300000}",
            initialDelayString =
                    "${app.shifts.cleanup-initial-delay-ms:60000}"
    )
    public void cleanupExpiredShifts() {

        try {
            int processedShiftCount =
                    employeeShiftService
                            .cleanupExpiredShifts();

            if (processedShiftCount > 0) {
                LOGGER.info(
                        "Au fost procesate automat {} ture expirate.",
                        processedShiftCount
                );
            }

        } catch (RuntimeException exception) {
            /*
             * Afisam eroarea reala, deoarece o problema in acest
             * proces ar putea afecta numarul de angajati transmis AI-ului.
             */
            LOGGER.error(
                    "Procesarea automata a turelor a esuat.",
                    exception
            );
        }
    }
}
