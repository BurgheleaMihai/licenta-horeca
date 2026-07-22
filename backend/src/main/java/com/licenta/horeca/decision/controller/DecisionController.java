package com.licenta.horeca.decision.controller;

import com.licenta.horeca.decision.dto.DecisionLabelRequest;
import com.licenta.horeca.decision.dto.DecisionResponse;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.decision.service.DecisionService;
import com.licenta.horeca.decision.service.DecisionTrainingRecordService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/decision")
public class DecisionController {
    private final DecisionService decisionService;

    private final DecisionTrainingRecordService decisionTrainingRecordService;

    public DecisionController(DecisionService decisionService, DecisionTrainingRecordService decisionTrainingRecordService) {
        this.decisionService = decisionService;

        this.decisionTrainingRecordService = decisionTrainingRecordService;
    }

    @GetMapping("/summary")
    public DecisionResponse getDecisionSummary() {
        return decisionService.getDecisionSummary();
    }

    @GetMapping("/training-records/latest-unlabeled")
    public DecisionTrainingRecord getLatestUnlabeledRecord() {
        return decisionTrainingRecordService.getLatestUnlabeledRecord();
    }

    @PutMapping("/training-records/{recordId}/label")
    public DecisionTrainingRecord labelRecord(@PathVariable Long recordId, @Valid @RequestBody DecisionLabelRequest request) {
        return decisionTrainingRecordService.labelRecord(recordId, request);
    }

    @PostMapping("/retrain")
    public ResponseEntity<String> retrainModels() {
        return decisionService.retrainModels();
    }
}