package com.licenta.horeca.decision.service;

import com.licenta.horeca.decision.dto.DecisionLabelRequest;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.decision.repository.DecisionTrainingRecordRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.time.LocalDateTime;
import java.util.Set;

@Service
public class DecisionTrainingRecordService {
    private static final Set<String> ALLOWED_LEVELS =
            Set.of("SCAZUT", "MEDIU", "RIDICAT");
    private final DecisionTrainingRecordRepository repository;
    public DecisionTrainingRecordService(DecisionTrainingRecordRepository repository) {
        this.repository = repository;
    }
    @Transactional(readOnly = true)
    public DecisionTrainingRecord getLatestUnlabeledRecord() {
        return repository
                .findFirstByLabeledAtIsNullOrderByCreatedAtDesc()
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu exista nicio predictie neetichetata."));
    }

    @Transactional
    public DecisionTrainingRecord labelRecord(Long recordId, DecisionLabelRequest request) {
        DecisionTrainingRecord record = repository
                .findById(recordId)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "Inregistrarea nu exista."));
        if (record.getLabeledAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Inregistrarea a fost deja etichetata.");
        }

        String observedTrafficLevel = normalizeLevel(request.getObservedTrafficLevel(),"Nivelul real de trafic");
        String observedDelayRisk = normalizeLevel(request.getObservedDelayRisk(),"Riscul real de intarziere");

        record.setObservedTrafficLevel(observedTrafficLevel);
        record.setObservedDelayRisk(observedDelayRisk);
        record.setActualWaiters(request.getActualWaiters());
        record.setActualKitchenStaff(request.getActualKitchenStaff());
        record.setActualBarStaff(request.getActualBarStaff());
        record.setLabeledAt(LocalDateTime.now());
        return repository.save(record);
    }

    private String normalizeLevel(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, fieldName + " este obligatoriu.");
        }

        String normalizedValue = value.trim().toUpperCase();
        if (!ALLOWED_LEVELS.contains(normalizedValue)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, fieldName + " trebuie sa fie SCAZUT, MEDIU sau RIDICAT.");
        }

        return normalizedValue;
    }
}
