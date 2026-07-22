package com.licenta.horeca;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.controller.FeedbackController;
import com.licenta.horeca.entity.Feedback;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.service.FeedbackService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(FeedbackController.class)
@Import(SecurityConfig.class)
class FeedbackControllerTest {

    private static final String FEEDBACK_COMMENT =
            "Foarte bine.";

    private static final String GOOD_SERVICE_COMMENT =
            "Servire buna.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private FeedbackService feedbackService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createFeedbackShouldReturnSavedFeedback()
            throws Exception {

        Feedback feedback = new Feedback();
        feedback.setRating(5);
        feedback.setComment(FEEDBACK_COMMENT);

        when(
                feedbackService.saveFeedback(
                        any(Feedback.class)
                )
        ).thenReturn(feedback);

        mockMvc
                .perform(
                        post("/api/feedback")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                feedback
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.rating")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.comment")
                                .value(FEEDBACK_COMMENT)
                );
    }

    @Test
    @WithMockUser(
            username = "manager@test.com",
            roles = "MANAGER"
    )
    void getAllFeedbackShouldReturnFeedbackList()
            throws Exception {

        Feedback feedback1 = new Feedback();
        feedback1.setRating(5);
        feedback1.setComment(FEEDBACK_COMMENT);

        Feedback feedback2 = new Feedback();
        feedback2.setRating(4);
        feedback2.setComment(GOOD_SERVICE_COMMENT);

        when(feedbackService.getAllFeedback())
                .thenReturn(
                        List.of(
                                feedback1,
                                feedback2
                        )
                );

        mockMvc
                .perform(
                        get("/api/feedback")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].rating")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$[1].comment")
                                .value(GOOD_SERVICE_COMMENT)
                );
    }
}
