package com.licenta.horeca;

import com.licenta.horeca.entity.Feedback;
import com.licenta.horeca.repository.FeedbackRepository;
import com.licenta.horeca.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void saveFeedback_shouldSaveFeedback() {
        Feedback feedback = new Feedback();
        feedback.setRating(5);
        feedback.setComment("Meniul este usor de folosit.");

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Feedback savedFeedback = feedbackService.saveFeedback(feedback);

        assertEquals(5, savedFeedback.getRating());
        assertEquals("Meniul este usor de folosit.", savedFeedback.getComment());

        verify(feedbackRepository, times(1)).save(feedback);
    }

    @Test
    void getAllFeedback_shouldReturnFeedbackList() {
        Feedback feedback1 = new Feedback();
        feedback1.setRating(5);
        feedback1.setComment("Foarte bine.");

        Feedback feedback2 = new Feedback();
        feedback2.setRating(4);
        feedback2.setComment("Servire buna.");

        when(feedbackRepository.findAll())
                .thenReturn(List.of(feedback1, feedback2));

        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        assertEquals(2, feedbackList.size());
        assertEquals("Foarte bine.", feedbackList.get(0).getComment());
        assertEquals(4, feedbackList.get(1).getRating());

        verify(feedbackRepository, times(1)).findAll();
    }
}