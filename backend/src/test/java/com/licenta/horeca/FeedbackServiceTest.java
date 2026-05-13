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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    private static final String EASY_MENU_COMMENT = "Meniul este usor de folosit.";
    private static final String VERY_GOOD_COMMENT = "Foarte bine.";
    private static final String GOOD_SERVICE_COMMENT = "Servire buna.";

    @Mock
    private FeedbackRepository feedbackRepository;

    @InjectMocks
    private FeedbackService feedbackService;

    @Test
    void saveFeedbackShouldSaveFeedback() {
        Feedback feedback = new Feedback();
        feedback.setRating(5);
        feedback.setComment(EASY_MENU_COMMENT);

        when(feedbackRepository.save(any(Feedback.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Feedback savedFeedback = feedbackService.saveFeedback(feedback);

        assertEquals(5, savedFeedback.getRating());
        assertEquals(EASY_MENU_COMMENT, savedFeedback.getComment());

        verify(feedbackRepository, times(1)).save(feedback);
    }

    @Test
    void getAllFeedbackShouldReturnFeedbackList() {
        Feedback feedback1 = new Feedback();
        feedback1.setRating(5);
        feedback1.setComment(VERY_GOOD_COMMENT);

        Feedback feedback2 = new Feedback();
        feedback2.setRating(4);
        feedback2.setComment(GOOD_SERVICE_COMMENT);

        when(feedbackRepository.findAll())
                .thenReturn(List.of(feedback1, feedback2));

        List<Feedback> feedbackList = feedbackService.getAllFeedback();

        assertEquals(2, feedbackList.size());
        assertEquals(VERY_GOOD_COMMENT, feedbackList.get(0).getComment());
        assertEquals(4, feedbackList.get(1).getRating());

        verify(feedbackRepository, times(1)).findAll();
    }
}