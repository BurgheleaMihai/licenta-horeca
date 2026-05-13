package com.licenta.horeca;

import com.licenta.horeca.entity.TrafficEvent;
import com.licenta.horeca.enums.TrafficEventType;
import com.licenta.horeca.repository.TrafficEventRepository;
import com.licenta.horeca.service.TrafficEventService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TrafficEventServiceTest {

    @Mock
    private TrafficEventRepository trafficEventRepository;

    @InjectMocks
    private TrafficEventService trafficEventService;

    @Test
    void saveEventWithEntryTypeSavesEntryEvent() {
        TrafficEvent savedEvent = new TrafficEvent(TrafficEventType.ENTRY);

        when(trafficEventRepository.save(any(TrafficEvent.class)))
                .thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.ENTRY);

        assertEquals(TrafficEventType.ENTRY, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void saveEventWithExitTypeSavesExitEvent() {
        TrafficEvent savedEvent = new TrafficEvent(TrafficEventType.EXIT);

        when(trafficEventRepository.save(any(TrafficEvent.class)))
                .thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.EXIT);

        assertEquals(TrafficEventType.EXIT, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void getEstimatedOccupancyReturnsEntriesMinusExits() {
        when(trafficEventRepository.countByType(TrafficEventType.ENTRY))
                .thenReturn(10L);

        when(trafficEventRepository.countByType(TrafficEventType.EXIT))
                .thenReturn(4L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(6L, result);
    }

    @Test
    void getEstimatedOccupancyWhenExitsAreGreaterThanEntriesReturnsZero() {
        when(trafficEventRepository.countByType(TrafficEventType.ENTRY))
                .thenReturn(3L);

        when(trafficEventRepository.countByType(TrafficEventType.EXIT))
                .thenReturn(8L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(0L, result);
    }
}