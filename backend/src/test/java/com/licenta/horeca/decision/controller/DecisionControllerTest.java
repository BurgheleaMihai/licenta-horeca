package com.licenta.horeca.decision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.decision.dto.DecisionLabelRequest;
import com.licenta.horeca.decision.dto.DecisionResponse;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.decision.service.DecisionService;
import com.licenta.horeca.decision.service.DecisionTrainingRecordService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(DecisionController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin@test.com", roles = "ADMIN")
class DecisionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private DecisionService decisionService;

    @MockitoBean
    private DecisionTrainingRecordService decisionTrainingRecordService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getDecisionSummaryShouldReturnAiSummary() throws Exception {

        DecisionResponse response = new DecisionResponse();

        response.setTrafficLevel("RIDICAT");
        response.setRecommendedWaiters(3);
        response.setRecommendedKitchenStaff(2);
        response.setRecommendedBarStaff(1);
        response.setDelayRisk("MEDIU");

        when(decisionService.getDecisionSummary()).thenReturn(response);

        mockMvc.perform(get("/api/decision/summary").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.trafficLevel").value("RIDICAT")).andExpect(jsonPath("$.recommendedWaiters").value(3)).andExpect(jsonPath("$.recommendedKitchenStaff").value(2)).andExpect(jsonPath("$.recommendedBarStaff").value(1)).andExpect(jsonPath("$.delayRisk").value("MEDIU"));

        verify(decisionService).getDecisionSummary();
    }

    @Test
    void getLatestUnlabeledRecordShouldReturnRecord() throws Exception {

        DecisionTrainingRecord record = createTrainingRecord();

        when(decisionTrainingRecordService.getLatestUnlabeledRecord()).thenReturn(record);

        mockMvc.perform(get("/api/decision/" + "training-records/" + "latest-unlabeled").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(15)).andExpect(jsonPath("$.predictedTrafficLevel").value("MEDIU")).andExpect(jsonPath("$.predictedDelayRisk").value("SCAZUT")).andExpect(jsonPath("$.activeOrders").value(4)).andExpect(jsonPath("$.occupiedTables").value(3)).andExpect(jsonPath("$.recommendedWaiters").value(2));

        verify(decisionTrainingRecordService).getLatestUnlabeledRecord();
    }

    @Test
    void getLatestUnlabeledRecordShouldReturnNotFound() throws Exception {

        when(decisionTrainingRecordService.getLatestUnlabeledRecord()).thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Nu exista nicio predictie neetichetata."));

        mockMvc.perform(get("/api/decision/" + "training-records/" + "latest-unlabeled")).andExpect(status().isNotFound());

        verify(decisionTrainingRecordService).getLatestUnlabeledRecord();
    }

    @Test
    void labelRecordShouldValidateDelegateAndReturnRecord() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        DecisionTrainingRecord savedRecord = createTrainingRecord();

        savedRecord.setObservedTrafficLevel("RIDICAT");
        savedRecord.setObservedDelayRisk("MEDIU");
        savedRecord.setActualWaiters(3);
        savedRecord.setActualKitchenStaff(2);
        savedRecord.setActualBarStaff(1);
        savedRecord.setLabeledAt(LocalDateTime.now());

        when(decisionTrainingRecordService.labelRecord(eq(15L), any(DecisionLabelRequest.class))).thenReturn(savedRecord);

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.id").value(15)).andExpect(jsonPath("$.observedTrafficLevel").value("RIDICAT")).andExpect(jsonPath("$.observedDelayRisk").value("MEDIU")).andExpect(jsonPath("$.actualWaiters").value(3)).andExpect(jsonPath("$.actualKitchenStaff").value(2)).andExpect(jsonPath("$.actualBarStaff").value(1)).andExpect(jsonPath("$.labeledAt").exists());

        ArgumentCaptor<DecisionLabelRequest> requestCaptor = ArgumentCaptor.forClass(DecisionLabelRequest.class);

        verify(decisionTrainingRecordService).labelRecord(eq(15L), requestCaptor.capture());

        DecisionLabelRequest capturedRequest = requestCaptor.getValue();

        assertEquals("RIDICAT", capturedRequest.getObservedTrafficLevel());

        assertEquals("MEDIU", capturedRequest.getObservedDelayRisk());

        assertEquals(3, capturedRequest.getActualWaiters());

        assertEquals(2, capturedRequest.getActualKitchenStaff());

        assertEquals(1, capturedRequest.getActualBarStaff());
    }

    @Test
    void labelRecordShouldRejectBlankTrafficLevel() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        request.setObservedTrafficLevel("   ");

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldRejectMissingDelayRisk() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        request.setObservedDelayRisk(null);

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldRejectNegativeWaiterCount() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        request.setActualWaiters(-1);

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldRejectMissingKitchenStaff() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        request.setActualKitchenStaff(null);

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldRejectMissingBarStaff() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        request.setActualBarStaff(null);

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldRejectMalformedJson() throws Exception {

        String malformedJson = """
                {
                  "observedTrafficLevel": "RIDICAT",
                  "observedDelayRisk":
                }
                """;

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(malformedJson)).andExpect(status().isBadRequest());

        verify(decisionTrainingRecordService, never()).labelRecord(any(), any(DecisionLabelRequest.class));
    }

    @Test
    void labelRecordShouldReturnConflictWhenAlreadyLabeled() throws Exception {

        DecisionLabelRequest request = createValidLabelRequest();

        when(decisionTrainingRecordService.labelRecord(eq(15L), any(DecisionLabelRequest.class))).thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Inregistrarea a fost deja etichetata."));

        mockMvc.perform(put("/api/decision/" + "training-records/" + "{recordId}/label", 15L).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isConflict());

        verify(decisionTrainingRecordService).labelRecord(eq(15L), any(DecisionLabelRequest.class));
    }

    @Test
    void retrainModelsShouldReturnSuccessfulResponse() throws Exception {

        String responseBody = """
                {
                  "status": "success",
                  "modelsReplaced": true,
                  "message": "Modelele au fost reantrenate."
                }
                """;

        when(decisionService.retrainModels()).thenReturn(ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(responseBody));

        mockMvc.perform(post("/api/decision/retrain").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.status").value("success")).andExpect(jsonPath("$.modelsReplaced").value(true));

        verify(decisionService).retrainModels();
    }

    @Test
    void retrainModelsShouldPreserveBlockedStatus() throws Exception {

        String responseBody = """
                {
                  "status": "blocked",
                  "modelsReplaced": false,
                  "message": "Sunt necesare 30 de inregistrari."
                }
                """;

        when(decisionService.retrainModels()).thenReturn(ResponseEntity.status(HttpStatus.BAD_REQUEST).contentType(MediaType.APPLICATION_JSON).body(responseBody));

        mockMvc.perform(post("/api/decision/retrain")).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value("blocked")).andExpect(jsonPath("$.modelsReplaced").value(false)).andExpect(jsonPath("$.message").value("Sunt necesare " + "30 de inregistrari."));

        verify(decisionService).retrainModels();
    }

    @Test
    void retrainModelsShouldReturnServiceUnavailable() throws Exception {

        String responseBody = """
                {
                  "status": "error",
                  "modelsReplaced": false,
                  "message": "AI Service nu este disponibil."
                }
                """;

        when(decisionService.retrainModels()).thenReturn(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).contentType(MediaType.APPLICATION_JSON).body(responseBody));

        mockMvc.perform(post("/api/decision/retrain")).andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.status").value("error")).andExpect(jsonPath("$.modelsReplaced").value(false)).andExpect(jsonPath("$.message").value("AI Service nu este disponibil."));

        verify(decisionService).retrainModels();
    }

    private DecisionLabelRequest createValidLabelRequest() {
        DecisionLabelRequest request = new DecisionLabelRequest();

        request.setObservedTrafficLevel("RIDICAT");

        request.setObservedDelayRisk("MEDIU");

        request.setActualWaiters(3);
        request.setActualKitchenStaff(2);
        request.setActualBarStaff(1);

        return request;
    }

    private DecisionTrainingRecord createTrainingRecord() {
        DecisionTrainingRecord record = new DecisionTrainingRecord();

        ReflectionTestUtils.setField(record, "id", 15L);

        record.setCreatedAt(LocalDateTime.now().minusMinutes(5));

        record.setDayOfWeek(3);
        record.setHour(19);
        record.setActiveOrders(4);
        record.setOccupiedTables(3);
        record.setEstimatedOccupancy(25);
        record.setKitchenLoad(5);
        record.setBarLoad(2);
        record.setAvgPreparationTime(20);
        record.setOrdersLast30Min(4);
        record.setOrderAgeMinutes(8);
        record.setItemCount(7);

        record.setPredictedTrafficLevel("MEDIU");

        record.setPredictedDelayRisk("SCAZUT");

        record.setRecommendedWaiters(2);

        record.setRecommendedKitchenStaff(1);

        record.setRecommendedBarStaff(1);

        return record;
    }
}
