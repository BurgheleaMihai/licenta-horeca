package com.licenta.horeca.controller;
import com.licenta.horeca.dto.DecisionResponse;
import com.licenta.horeca.service.DecisionService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/decision")
public class DecisionController {

    private final DecisionService decisionService;

    public DecisionController(DecisionService decisionService) {
        this.decisionService = decisionService;
    }

    @GetMapping("/summary")
    public DecisionResponse getDecisionSummary() {
        return decisionService.getDecisionSummary();
    }
}