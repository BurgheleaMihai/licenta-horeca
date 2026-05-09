package com.licenta.horeca.service;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.TrafficEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class TrafficEventServiceTest {

    @Mock
    private TrafficEventRepository trafficEventRepository;

    @InjectMocks
    private TrafficEventService trafficEventService;

    @Test
    void saveEvent_withEntryType_savesEntryEvent() {
        TrafficEvent savedEvent = new TrafficEvent(TrafficEventType.ENTRY);

        when(trafficEventRepository.save(any(TrafficEvent.class)))
                .thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.ENTRY);

        assertEquals(TrafficEventType.ENTRY, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void saveEvent_withExitType_savesExitEvent() {
        TrafficEvent savedEvent = new TrafficEvent(TrafficEventType.EXIT);

        when(trafficEventRepository.save(any(TrafficEvent.class)))
                .thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.EXIT);

        assertEquals(TrafficEventType.EXIT, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void getEstimatedOccupancy_returnsEntriesMinusExits() {
        when(trafficEventRepository.countByType(TrafficEventType.ENTRY))
                .thenReturn(10L);

        when(trafficEventRepository.countByType(TrafficEventType.EXIT))
                .thenReturn(4L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(6L, result);
    }

    @Test
    void getEstimatedOccupancy_whenExitsAreGreaterThanEntries_returnsZero() {
        when(trafficEventRepository.countByType(TrafficEventType.ENTRY))
                .thenReturn(3L);

        when(trafficEventRepository.countByType(TrafficEventType.EXIT))
                .thenReturn(8L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(0L, result);
    }
}
