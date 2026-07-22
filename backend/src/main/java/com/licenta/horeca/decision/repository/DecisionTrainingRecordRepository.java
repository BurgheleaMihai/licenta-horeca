package com.licenta.horeca.decision.repository;

import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionTrainingRecordRepository
        extends JpaRepository<DecisionTrainingRecord, Long> {
    List<DecisionTrainingRecord>
    findByObservedTrafficLevelIsNotNullAndObservedDelayRiskIsNotNull();

    Optional<DecisionTrainingRecord>
    findFirstByLabeledAtIsNullOrderByCreatedAtDesc();
}