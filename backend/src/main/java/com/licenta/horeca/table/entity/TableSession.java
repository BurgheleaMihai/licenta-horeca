package com.licenta.horeca.table.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "table_sessions")
public class TableSession {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    // Masa fizica la care apartine sesiunea QR
    @ManyToOne
    @JoinColumn(name = "table_id", nullable = false)
    private RestaurantTable restaurantTable;

    // Cod unic pentru sesiune, folosit la link/QR
    @Column(nullable = false, unique = true) private String sessionCode;

    // Momentul cand sesiunea a inceput
    @Column(nullable = false) private LocalDateTime startedAt;

    // Momentul cand sesiunea s-a incheiat
    private LocalDateTime endedAt;

    // Daca sesiunea este activa
    @Column(nullable = false) private boolean active = true;

    public TableSession() {}

    public TableSession(RestaurantTable restaurantTable, String sessionCode) {
        this.restaurantTable = restaurantTable;
        this.sessionCode = sessionCode;
        this.startedAt = LocalDateTime.now();
        this.active = true;
    }

    public Long getId() {
        return id;
    }

    public RestaurantTable getRestaurantTable() {
        return restaurantTable;
    }

    public void setRestaurantTable(RestaurantTable restaurantTable) {
        this.restaurantTable = restaurantTable;
    }

    public String getSessionCode() {
        return sessionCode;
    }

    public void setSessionCode(String sessionCode) {
        this.sessionCode = sessionCode;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getEndedAt() {
        return endedAt;
    }

    public void setEndedAt(LocalDateTime endedAt) {
        this.endedAt = endedAt;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}