package com.licenta.horeca.decision.dto;

public class DecisionResponse {

    private String trafficLevel;

    private int recommendedWaiters;

    private int recommendedKitchenStaff;

    private int recommendedBarStaff;

    private String delayRisk;

    private int activeWaiters;

    private int activeKitchenStaff;

    private int activeBarStaff;

    private int waiterDeficit;

    private int kitchenDeficit;

    private int barDeficit;

    private int totalStaffDeficit;

    public DecisionResponse() {
    }

    public String getTrafficLevel() {
        return trafficLevel;
    }

    public void setTrafficLevel(String trafficLevel) {
        this.trafficLevel = trafficLevel;
    }

    public int getRecommendedWaiters() {
        return recommendedWaiters;
    }

    public void setRecommendedWaiters(int recommendedWaiters) {
        this.recommendedWaiters = recommendedWaiters;
    }

    public int getRecommendedKitchenStaff() {
        return recommendedKitchenStaff;
    }

    public void setRecommendedKitchenStaff(
            int recommendedKitchenStaff
    ) {
        this.recommendedKitchenStaff =
                recommendedKitchenStaff;
    }

    public int getRecommendedBarStaff() {
        return recommendedBarStaff;
    }

    public void setRecommendedBarStaff(
            int recommendedBarStaff
    ) {
        this.recommendedBarStaff =
                recommendedBarStaff;
    }

    public String getDelayRisk() {
        return delayRisk;
    }

    public void setDelayRisk(String delayRisk) {
        this.delayRisk = delayRisk;
    }

    public int getActiveWaiters() {
        return activeWaiters;
    }

    public void setActiveWaiters(int activeWaiters) {
        this.activeWaiters = activeWaiters;
    }

    public int getActiveKitchenStaff() {
        return activeKitchenStaff;
    }

    public void setActiveKitchenStaff(
            int activeKitchenStaff
    ) {
        this.activeKitchenStaff =
                activeKitchenStaff;
    }

    public int getActiveBarStaff() {
        return activeBarStaff;
    }

    public void setActiveBarStaff(int activeBarStaff) {
        this.activeBarStaff = activeBarStaff;
    }

    public int getWaiterDeficit() {
        return waiterDeficit;
    }

    public void setWaiterDeficit(int waiterDeficit) {
        this.waiterDeficit = waiterDeficit;
    }

    public int getKitchenDeficit() {
        return kitchenDeficit;
    }

    public void setKitchenDeficit(
            int kitchenDeficit
    ) {
        this.kitchenDeficit = kitchenDeficit;
    }

    public int getBarDeficit() {
        return barDeficit;
    }

    public void setBarDeficit(int barDeficit) {
        this.barDeficit = barDeficit;
    }

    public int getTotalStaffDeficit() {
        return totalStaffDeficit;
    }

    public void setTotalStaffDeficit(
            int totalStaffDeficit
    ) {
        this.totalStaffDeficit =
                totalStaffDeficit;
    }
}