package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface FeedbackRepository
        extends JpaRepository<Feedback, Long> {

    List<Feedback>
    findByCreatedAtGreaterThanEqualAndCreatedAtLessThan(
            LocalDateTime start,
            LocalDateTime end
    );
}