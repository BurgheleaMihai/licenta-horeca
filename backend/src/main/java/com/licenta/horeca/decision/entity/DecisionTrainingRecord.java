package com.licenta.horeca.decision.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "decision_training_records")
public class DecisionTrainingRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;

    @Column(nullable = false) private LocalDateTime createdAt;

    // Datele de intrare trimise catre serviciul AI

    @Column(nullable = false) private int dayOfWeek;

    @Column(nullable = false) private int hour;

    @Column(nullable = false) private int activeOrders;

    @Column(nullable = false) private int occupiedTables;

    @Column(nullable = false) private int estimatedOccupancy;

    @Column(nullable = false) private int kitchenLoad;

    @Column(nullable = false) private int barLoad;

    @Column(nullable = false) private int avgPreparationTime;

    @Column(nullable = false) private int ordersLast30Min;

    @Column(nullable = false) private int orderAgeMinutes;

    @Column(nullable = false) private int itemCount;

    // Rezultatele prezise de modelele AI

    @Column(length = 30) private String predictedTrafficLevel;

    private Integer recommendedWaiters;

    private Integer recommendedKitchenStaff;

    private Integer recommendedBarStaff;

    @Column(length = 30) private String predictedDelayRisk;

    /*
     * Rezultatele reale observate ulterior.
     * Aceste campuri raman null pana cand exista suficiente
     * informatii reale pentru completarea lor.
     */

    @Column(length = 30) private String observedTrafficLevel;

    private Integer actualWaiters;

    private Integer actualKitchenStaff;

    private Integer actualBarStaff;

    @Column(length = 30) private String observedDelayRisk;

    private LocalDateTime labeledAt;

    public DecisionTrainingRecord() {}

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public int getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getHour() {
        return hour;
    }

    public void setHour(int hour) {
        this.hour = hour;
    }

    public int getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(int activeOrders) {
        this.activeOrders = activeOrders;
    }

    public int getOccupiedTables() {
        return occupiedTables;
    }

    public void setOccupiedTables(int occupiedTables) {
        this.occupiedTables = occupiedTables;
    }

    public int getEstimatedOccupancy() {
        return estimatedOccupancy;
    }

    public void setEstimatedOccupancy(int estimatedOccupancy) {
        this.estimatedOccupancy = estimatedOccupancy;
    }

    public int getKitchenLoad() {
        return kitchenLoad;
    }

    public void setKitchenLoad(int kitchenLoad) {
        this.kitchenLoad = kitchenLoad;
    }

    public int getBarLoad() {
        return barLoad;
    }

    public void setBarLoad(int barLoad) {
        this.barLoad = barLoad;
    }

    public int getAvgPreparationTime() {
        return avgPreparationTime;
    }

    public void setAvgPreparationTime(int avgPreparationTime) {
        this.avgPreparationTime = avgPreparationTime;
    }

    public int getOrdersLast30Min() {
        return ordersLast30Min;
    }

    public void setOrdersLast30Min(int ordersLast30Min) {
        this.ordersLast30Min = ordersLast30Min;
    }

    public int getOrderAgeMinutes() {
        return orderAgeMinutes;
    }

    public void setOrderAgeMinutes(int orderAgeMinutes) {
        this.orderAgeMinutes = orderAgeMinutes;
    }

    public int getItemCount() {
        return itemCount;
    }

    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

    public String getPredictedTrafficLevel() {
        return predictedTrafficLevel;
    }

    public void setPredictedTrafficLevel(String predictedTrafficLevel) {
        this.predictedTrafficLevel = predictedTrafficLevel;
    }

    public Integer getRecommendedWaiters() {
        return recommendedWaiters;
    }

    public void setRecommendedWaiters(Integer recommendedWaiters) {
        this.recommendedWaiters = recommendedWaiters;
    }

    public Integer getRecommendedKitchenStaff() {
        return recommendedKitchenStaff;
    }

    public void setRecommendedKitchenStaff(Integer recommendedKitchenStaff) {
        this.recommendedKitchenStaff = recommendedKitchenStaff;
    }

    public Integer getRecommendedBarStaff() {
        return recommendedBarStaff;
    }

    public void setRecommendedBarStaff(Integer recommendedBarStaff) {
        this.recommendedBarStaff = recommendedBarStaff;
    }

    public String getPredictedDelayRisk() {
        return predictedDelayRisk;
    }

    public void setPredictedDelayRisk(String predictedDelayRisk) {
        this.predictedDelayRisk = predictedDelayRisk;
    }

    public String getObservedTrafficLevel() {
        return observedTrafficLevel;
    }

    public void setObservedTrafficLevel(String observedTrafficLevel) {
        this.observedTrafficLevel = observedTrafficLevel;
    }

    public Integer getActualWaiters() {
        return actualWaiters;
    }

    public void setActualWaiters(Integer actualWaiters) {
        this.actualWaiters = actualWaiters;
    }

    public Integer getActualKitchenStaff() {
        return actualKitchenStaff;
    }

    public void setActualKitchenStaff(Integer actualKitchenStaff) {
        this.actualKitchenStaff = actualKitchenStaff;
    }

    public Integer getActualBarStaff() {
        return actualBarStaff;
    }

    public void setActualBarStaff(Integer actualBarStaff) {
        this.actualBarStaff = actualBarStaff;
    }

    public String getObservedDelayRisk() {
        return observedDelayRisk;
    }

    public void setObservedDelayRisk(String observedDelayRisk) {
        this.observedDelayRisk = observedDelayRisk;
    }

    public LocalDateTime getLabeledAt() {
        return labeledAt;
    }

    public void setLabeledAt(LocalDateTime labeledAt) {
        this.labeledAt = labeledAt;
    }
}