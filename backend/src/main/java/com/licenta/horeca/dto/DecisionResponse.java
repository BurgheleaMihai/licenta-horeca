package com.licenta.horeca.dto;

public class DecisionResponse {

    private String trafficLevel;
    private int recommendedWaiters;
    private int recommendedKitchenStaff;
    private int recommendedBarStaff;
    private String delayRisk;

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

    public void setRecommendedKitchenStaff(int recommendedKitchenStaff) {
        this.recommendedKitchenStaff = recommendedKitchenStaff;
    }

    public int getRecommendedBarStaff() {
        return recommendedBarStaff;
    }

    public void setRecommendedBarStaff(int recommendedBarStaff) {
        this.recommendedBarStaff = recommendedBarStaff;
    }

    public String getDelayRisk() {
        return delayRisk;
    }

    public void setDelayRisk(String delayRisk) {
        this.delayRisk = delayRisk;
    }
}