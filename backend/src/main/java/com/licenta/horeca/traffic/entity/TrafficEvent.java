package com.licenta.horeca.traffic.entity;

import com.licenta.horeca.traffic.enums.TrafficEventType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "traffic_events")
public class TrafficEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TrafficEventType type;

    @Column(nullable = false) private LocalDateTime createdAt;

    public TrafficEvent() {}

    public TrafficEvent(TrafficEventType type) {
        this.type = type;
    }

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public TrafficEventType getType() {
        return type;
    }

    public void setType(TrafficEventType type) {
        this.type = type;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}