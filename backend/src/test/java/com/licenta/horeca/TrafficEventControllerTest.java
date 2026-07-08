package com.licenta.horeca;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licenta.horeca.controller.TrafficEventController;
import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.service.TrafficEventService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TrafficEventController.class)
class TrafficEventControllerTest {
    private static final String ENTRY_ENDPOINT = "/api/traffic/entry";
    private static final String EXIT_ENDPOINT = "/api/traffic/exit";
    private static final String TRAFFIC_ENDPOINT = "/api/traffic";
    private static final String SUMMARY_ENDPOINT = "/api/traffic/summary";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private TrafficEventService trafficEventService;

    @Test
    void registerEntryShouldReturnEntryEvent() throws Exception {
        TrafficEvent event = new TrafficEvent(TrafficEventType.ENTRY);

        when(trafficEventService.saveEvent(TrafficEventType.ENTRY))
                .thenReturn(event);

        mockMvc.perform(post(ENTRY_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("ENTRY"));
    }

    @Test
    void registerExitShouldReturnExitEvent() throws Exception {
        TrafficEvent event = new TrafficEvent(TrafficEventType.EXIT);

        when(trafficEventService.saveEvent(TrafficEventType.EXIT))
                .thenReturn(event);

        mockMvc.perform(post(EXIT_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("EXIT"));
    }

    @Test
    void getAllEventsShouldReturnEvents() throws Exception {
        TrafficEvent entryEvent = new TrafficEvent(TrafficEventType.ENTRY);
        TrafficEvent exitEvent = new TrafficEvent(TrafficEventType.EXIT);

        when(trafficEventService.getAllEvents())
                .thenReturn(List.of(entryEvent, exitEvent));

        mockMvc.perform(get(TRAFFIC_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("ENTRY"))
                .andExpect(jsonPath("$[1].type").value("EXIT"));
    }

    @Test
    void getSummaryShouldReturnTrafficSummary() throws Exception {
        when(trafficEventService.getEntryCount()).thenReturn(10L);
        when(trafficEventService.getExitCount()).thenReturn(4L);
        when(trafficEventService.getEstimatedOccupancy()).thenReturn(6L);

        mockMvc.perform(get(SUMMARY_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entries").value(10))
                .andExpect(jsonPath("$.exits").value(4))
                .andExpect(jsonPath("$.estimatedOccupancy").value(6));
    }
}
