package com.licenta.horeca.controller;

import com.licenta.horeca.entity.Feedback;
import com.licenta.horeca.service.FeedbackService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feedback")
@CrossOrigin(origins = "http://localhost:5173")
public class FeedbackController {
    private final FeedbackService feedbackService;

    public FeedbackController(FeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PostMapping
    public Feedback saveFeedback(@RequestBody Feedback feedback) {
        return feedbackService.saveFeedback(feedback);
    }

    @GetMapping
    public List<Feedback> getAllFeedback() {
        return feedbackService.getAllFeedback();
    }
}
