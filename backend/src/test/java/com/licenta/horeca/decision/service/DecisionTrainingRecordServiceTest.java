package com.licenta.horeca.decision.service;

import com.licenta.horeca.decision.dto.DecisionLabelRequest;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.decision.repository.DecisionTrainingRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionTrainingRecordServiceTest {

    @Mock
    private DecisionTrainingRecordRepository repository;

    private DecisionTrainingRecordService service;

    @BeforeEach
    void setUp() {
        service = new DecisionTrainingRecordService(
                repository
        );
    }

    @Test
    void getLatestUnlabeledRecordShouldReturnRecord() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        when(
                repository
                        .findFirstByLabeledAtIsNullOrderByCreatedAtDesc()
        ).thenReturn(Optional.of(record));

        DecisionTrainingRecord result =
                service.getLatestUnlabeledRecord();

        assertSame(record, result);

        verify(repository)
                .findFirstByLabeledAtIsNullOrderByCreatedAtDesc();

        verifyNoMoreInteractions(repository);
    }

    @Test
    void getLatestUnlabeledRecordShouldThrowNotFoundWhenNoneExists() {
        when(
                repository
                        .findFirstByLabeledAtIsNullOrderByCreatedAtDesc()
        ).thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service
                                .getLatestUnlabeledRecord()
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "Nu exista nicio predictie neetichetata.",
                exception.getReason()
        );

        verify(repository)
                .findFirstByLabeledAtIsNullOrderByCreatedAtDesc();

        verifyNoMoreInteractions(repository);
    }

    @Test
    void labelRecordShouldNormalizeAndSaveAllValues() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "  mediu  ",
                        "  ridicat  ",
                        3,
                        2,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        when(repository.save(record))
                .thenReturn(record);

        LocalDateTime beforeLabeling =
                LocalDateTime.now();

        DecisionTrainingRecord result =
                service.labelRecord(
                        10L,
                        request
                );

        LocalDateTime afterLabeling =
                LocalDateTime.now();

        assertSame(record, result);

        assertEquals(
                "MEDIU",
                result.getObservedTrafficLevel()
        );

        assertEquals(
                "RIDICAT",
                result.getObservedDelayRisk()
        );

        assertEquals(
                3,
                result.getActualWaiters()
        );

        assertEquals(
                2,
                result.getActualKitchenStaff()
        );

        assertEquals(
                1,
                result.getActualBarStaff()
        );

        assertNotNull(result.getLabeledAt());

        assertFalse(
                result.getLabeledAt()
                        .isBefore(beforeLabeling)
        );

        assertFalse(
                result.getLabeledAt()
                        .isAfter(afterLabeling)
        );

        /*
         * Etichetarea nu trebuie sa modifice
         * predictiile originale ale sistemului AI.
         */
        assertEquals(
                "SCAZUT",
                result.getPredictedTrafficLevel()
        );

        assertEquals(
                "MEDIU",
                result.getPredictedDelayRisk()
        );

        assertEquals(
                2,
                result.getRecommendedWaiters()
        );

        assertEquals(
                1,
                result.getRecommendedKitchenStaff()
        );

        assertEquals(
                1,
                result.getRecommendedBarStaff()
        );

        verify(repository).findById(10L);
        verify(repository).save(record);
        verifyNoMoreInteractions(repository);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SCAZUT",
            "MEDIU",
            "RIDICAT"
    })
    void labelRecordShouldAcceptEveryAllowedLevel(
            String allowedLevel) {

        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        allowedLevel,
                        allowedLevel,
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        when(repository.save(record))
                .thenReturn(record);

        DecisionTrainingRecord result =
                service.labelRecord(
                        10L,
                        request
                );

        assertEquals(
                allowedLevel,
                result.getObservedTrafficLevel()
        );

        assertEquals(
                allowedLevel,
                result.getObservedDelayRisk()
        );

        verify(repository).save(record);
    }

    @Test
    void labelRecordShouldThrowNotFoundWhenRecordDoesNotExist() {
        DecisionLabelRequest request =
                createValidRequest();

        when(repository.findById(99L))
                .thenReturn(Optional.empty());

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                99L,
                                request
                        )
                );

        assertEquals(
                HttpStatus.NOT_FOUND,
                exception.getStatusCode()
        );

        assertEquals(
                "Inregistrarea nu exista.",
                exception.getReason()
        );

        verify(repository).findById(99L);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldThrowConflictWhenRecordIsAlreadyLabeled() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        LocalDateTime originalLabeledAt =
                LocalDateTime.now().minusHours(1);

        record.setLabeledAt(originalLabeledAt);
        record.setObservedTrafficLevel("SCAZUT");
        record.setObservedDelayRisk("MEDIU");
        record.setActualWaiters(1);
        record.setActualKitchenStaff(1);
        record.setActualBarStaff(1);

        DecisionLabelRequest request =
                createRequest(
                        "RIDICAT",
                        "RIDICAT",
                        5,
                        4,
                        3
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertEquals(
                HttpStatus.CONFLICT,
                exception.getStatusCode()
        );

        assertEquals(
                "Inregistrarea a fost deja etichetata.",
                exception.getReason()
        );

        /*
         * Valorile vechi trebuie sa ramana nemodificate.
         */
        assertEquals(
                originalLabeledAt,
                record.getLabeledAt()
        );

        assertEquals(
                "SCAZUT",
                record.getObservedTrafficLevel()
        );

        assertEquals(
                "MEDIU",
                record.getObservedDelayRisk()
        );

        assertEquals(
                1,
                record.getActualWaiters()
        );

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectNullTrafficLevel() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        null,
                        "MEDIU",
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Nivelul real de trafic este obligatoriu."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectBlankTrafficLevel() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "   ",
                        "MEDIU",
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Nivelul real de trafic este obligatoriu."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectInvalidTrafficLevel() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "FOARTE_RIDICAT",
                        "MEDIU",
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Nivelul real de trafic trebuie sa fie "
                        + "SCAZUT, MEDIU sau RIDICAT."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectNullDelayRisk() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "MEDIU",
                        null,
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Riscul real de intarziere este obligatoriu."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectBlankDelayRisk() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "MEDIU",
                        "\t ",
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Riscul real de intarziere este obligatoriu."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldRejectInvalidDelayRisk() {
        DecisionTrainingRecord record =
                createTrainingRecord();

        DecisionLabelRequest request =
                createRequest(
                        "MEDIU",
                        "NECUNOSCUT",
                        1,
                        1,
                        1
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(record));

        ResponseStatusException exception =
                assertThrows(
                        ResponseStatusException.class,
                        () -> service.labelRecord(
                                10L,
                                request
                        )
                );

        assertBadRequest(
                exception,
                "Riscul real de intarziere trebuie sa fie "
                        + "SCAZUT, MEDIU sau RIDICAT."
        );

        assertRecordWasNotLabeled(record);

        verify(
                repository,
                never()
        ).save(any());
    }

    @Test
    void labelRecordShouldReturnObjectReturnedByRepository() {
        DecisionTrainingRecord existingRecord =
                createTrainingRecord();

        DecisionTrainingRecord savedRecord =
                createTrainingRecord();

        savedRecord.setObservedTrafficLevel("RIDICAT");
        savedRecord.setObservedDelayRisk("SCAZUT");
        savedRecord.setActualWaiters(4);
        savedRecord.setActualKitchenStaff(3);
        savedRecord.setActualBarStaff(2);
        savedRecord.setLabeledAt(LocalDateTime.now());

        DecisionLabelRequest request =
                createRequest(
                        "RIDICAT",
                        "SCAZUT",
                        4,
                        3,
                        2
                );

        when(repository.findById(10L))
                .thenReturn(Optional.of(existingRecord));

        when(repository.save(existingRecord))
                .thenReturn(savedRecord);

        DecisionTrainingRecord result =
                service.labelRecord(
                        10L,
                        request
                );

        assertSame(savedRecord, result);

        verify(repository).save(existingRecord);
    }

    private DecisionTrainingRecord createTrainingRecord() {
        DecisionTrainingRecord record =
                new DecisionTrainingRecord();

        record.setCreatedAt(
                LocalDateTime.now().minusMinutes(10)
        );

        record.setDayOfWeek(3);
        record.setHour(19);
        record.setActiveOrders(5);
        record.setOccupiedTables(4);
        record.setEstimatedOccupancy(33);
        record.setKitchenLoad(7);
        record.setBarLoad(3);
        record.setAvgPreparationTime(20);
        record.setOrdersLast30Min(6);
        record.setOrderAgeMinutes(12);
        record.setItemCount(10);

        record.setPredictedTrafficLevel("SCAZUT");
        record.setPredictedDelayRisk("MEDIU");
        record.setRecommendedWaiters(2);
        record.setRecommendedKitchenStaff(1);
        record.setRecommendedBarStaff(1);

        return record;
    }

    private DecisionLabelRequest createValidRequest() {
        return createRequest(
                "MEDIU",
                "SCAZUT",
                2,
                1,
                1
        );
    }

    private DecisionLabelRequest createRequest(
            String trafficLevel,
            String delayRisk,
            Integer waiters,
            Integer kitchenStaff,
            Integer barStaff) {

        DecisionLabelRequest request =
                new DecisionLabelRequest();

        request.setObservedTrafficLevel(
                trafficLevel
        );

        request.setObservedDelayRisk(
                delayRisk
        );

        request.setActualWaiters(waiters);

        request.setActualKitchenStaff(
                kitchenStaff
        );

        request.setActualBarStaff(
                barStaff
        );

        return request;
    }

    private void assertBadRequest(
            ResponseStatusException exception,
            String expectedReason) {

        assertEquals(
                HttpStatus.BAD_REQUEST,
                exception.getStatusCode()
        );

        assertEquals(
                expectedReason,
                exception.getReason()
        );
    }

    private void assertRecordWasNotLabeled(
            DecisionTrainingRecord record) {

        assertNull(
                record.getObservedTrafficLevel()
        );

        assertNull(
                record.getObservedDelayRisk()
        );

        assertNull(record.getActualWaiters());

        assertNull(
                record.getActualKitchenStaff()
        );

        assertNull(record.getActualBarStaff());
        assertNull(record.getLabeledAt());
    }
}