package com.licenta.horeca.repository;

import com.licenta.horeca.entity.DecisionTrainingRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DecisionTrainingRecordRepository
        extends JpaRepository<DecisionTrainingRecord, Long> {

    List<DecisionTrainingRecord>
    findByObservedTrafficLevelIsNotNullAndObservedDelayRiskIsNotNull();

    Optional<DecisionTrainingRecord>
    findFirstByLabeledAtIsNullOrderByCreatedAtDesc();
}