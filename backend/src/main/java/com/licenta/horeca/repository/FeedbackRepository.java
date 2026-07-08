package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {}
