package com.licenta.horeca.feedback.service;

import com.licenta.horeca.feedback.entity.Feedback;
import com.licenta.horeca.feedback.repository.FeedbackRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(
            FeedbackRepository feedbackRepository
    ) {
        this.feedbackRepository = feedbackRepository;
    }

    public Feedback saveFeedback(
            Feedback feedback
    ) {
        return feedbackRepository.save(feedback);
    }

    public List<Feedback> getAllFeedback() {
        return feedbackRepository.findAll();
    }

    public double getAverageRatingBetween(
            LocalDateTime start,
            LocalDateTime end
    ) {
        List<Feedback> feedbackList =
                feedbackRepository
                        .findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                                start,
                                end
                        );

        return feedbackList
                .stream()
                .map(Feedback::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
    }
}