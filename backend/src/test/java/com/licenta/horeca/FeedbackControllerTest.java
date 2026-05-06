package com.licenta.horeca;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.controller.FeedbackController;
import com.licenta.horeca.entity.Feedback;
import com.licenta.horeca.service.FeedbackService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FeedbackController.class)
class FeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @Test
    void createFeedback_shouldReturnSavedFeedback() throws Exception {
        Feedback feedback = new Feedback();
        feedback.setRating(5);
        feedback.setComment("Foarte bine.");

        when(feedbackService.saveFeedback(any(Feedback.class))).thenReturn(feedback);

        mockMvc.perform(post("/api/feedback")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(feedback)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(5))
                .andExpect(jsonPath("$.comment").value("Foarte bine."));
    }

    @Test
    void getAllFeedback_shouldReturnFeedbackList() throws Exception {
        Feedback feedback1 = new Feedback();
        feedback1.setRating(5);
        feedback1.setComment("Foarte bine.");

        Feedback feedback2 = new Feedback();
        feedback2.setRating(4);
        feedback2.setComment("Servire buna.");

        when(feedbackService.getAllFeedback()).thenReturn(List.of(feedback1, feedback2));

        mockMvc.perform(get("/api/feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].rating").value(5))
                .andExpect(jsonPath("$[1].comment").value("Servire buna."));
    }
}