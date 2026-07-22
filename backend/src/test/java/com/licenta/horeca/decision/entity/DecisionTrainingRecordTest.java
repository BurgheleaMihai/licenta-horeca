package com.licenta.horeca.decision.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class DecisionTrainingRecordTest {

    @Test
    void prePersistShouldSetCreatedAtWhenItIsNull() {
        DecisionTrainingRecord record =
                new DecisionTrainingRecord();

        assertNull(record.getCreatedAt());

        LocalDateTime before =
                LocalDateTime.now();

        record.prePersist();

        LocalDateTime after =
                LocalDateTime.now();

        assertNotNull(record.getCreatedAt());

        assertFalse(
                record.getCreatedAt()
                        .isBefore(before)
        );

        assertFalse(
                record.getCreatedAt()
                        .isAfter(after)
        );
    }

    @Test
    void prePersistShouldPreserveExistingCreatedAt() {
        DecisionTrainingRecord record =
                new DecisionTrainingRecord();

        LocalDateTime existingCreatedAt =
                LocalDateTime.of(
                        2026,
                        7,
                        10,
                        12,
                        30
                );

        record.setCreatedAt(existingCreatedAt);

        record.prePersist();

        assertEquals(
                existingCreatedAt,
                record.getCreatedAt()
        );
    }
}
