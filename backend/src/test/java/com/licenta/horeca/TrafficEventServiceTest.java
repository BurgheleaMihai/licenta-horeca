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

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

        when(trafficEventRepository.save(any(TrafficEvent.class))).thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.ENTRY);

        assertEquals(TrafficEventType.ENTRY, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void saveEventWithExitTypeSavesExitEvent() {
        TrafficEvent savedEvent = new TrafficEvent(TrafficEventType.EXIT);

        when(trafficEventRepository.save(any(TrafficEvent.class))).thenReturn(savedEvent);

        TrafficEvent result = trafficEventService.saveEvent(TrafficEventType.EXIT);

        assertEquals(TrafficEventType.EXIT, result.getType());
        verify(trafficEventRepository).save(any(TrafficEvent.class));
    }

    @Test
    void getEstimatedOccupancyReturnsEntriesMinusExits() {
        when(trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TrafficEventType.ENTRY),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(10L);

        when(trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TrafficEventType.EXIT),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(4L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(6L, result);
    }

    @Test
    void getEstimatedOccupancyWhenExitsAreGreaterThanEntriesReturnsZero() {
        when(trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TrafficEventType.ENTRY),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(3L);

        when(trafficEventRepository.countByTypeAndCreatedAtGreaterThanEqualAndCreatedAtLessThan(
                eq(TrafficEventType.EXIT),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(8L);

        long result = trafficEventService.getEstimatedOccupancy();

        assertEquals(0L, result);
    }
}