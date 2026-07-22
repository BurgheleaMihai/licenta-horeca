package com.licenta.horeca.decision.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class DecisionLabelRequest {
    @NotBlank
    private String observedTrafficLevel;

    @NotBlank
    private String observedDelayRisk;

    @NotNull
    @Min(0)
    private Integer actualWaiters;

    @NotNull
    @Min(0)
    private Integer actualKitchenStaff;

    @NotNull
    @Min(0)
    private Integer actualBarStaff;

    public DecisionLabelRequest() {
    }

    public String getObservedTrafficLevel() {
        return observedTrafficLevel;
    }

    public void setObservedTrafficLevel(String observedTrafficLevel) {
        this.observedTrafficLevel = observedTrafficLevel;
    }

    public String getObservedDelayRisk() {
        return observedDelayRisk;
    }

    public void setObservedDelayRisk(String observedDelayRisk) {
        this.observedDelayRisk = observedDelayRisk;
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
}