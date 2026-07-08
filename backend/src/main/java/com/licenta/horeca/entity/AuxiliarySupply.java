package com.licenta.horeca.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auxiliary_supplies")
public class AuxiliarySupply {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    // Exemplu: pahare carton, cutii cartofi, servetele
    @Column(nullable = false) private String name;

    // Exemplu: ambalaje, consumabile, produse speciale
    @Column(nullable = false) private String category;

    // true = exista in depozit, false = lipsa in depozit
    @Column(nullable = false) private boolean availableInWarehouse = true;

    private LocalDateTime reportedAt;

    public AuxiliarySupply() {}

    public AuxiliarySupply(String name, String category) {
        this.name = name;
        this.category = category;
        this.availableInWarehouse = true;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailableInWarehouse() {
        return availableInWarehouse;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setAvailableInWarehouse(boolean availableInWarehouse) {
        this.availableInWarehouse = availableInWarehouse;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}
