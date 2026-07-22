package com.licenta.horeca.decision.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DecisionRequest {

    @JsonProperty("day_of_week")
    private int dayOfWeek;

    @JsonProperty("hour")
    private int hour;

    @JsonProperty("active_orders")
    private int activeOrders;

    @JsonProperty("occupied_tables")
    private int occupiedTables;

    @JsonProperty("estimated_occupancy")
    private int estimatedOccupancy;

    @JsonProperty("kitchen_load")
    private int kitchenLoad;

    @JsonProperty("bar_load")
    private int barLoad;

    @JsonProperty("avg_preparation_time")
    private int avgPreparationTime;

    @JsonProperty("orders_last_30_min")
    private int ordersLast30Min;

    @JsonProperty("order_age_minutes")
    private int orderAgeMinutes;

    @JsonProperty("item_count")
    private int itemCount;

    @JsonProperty("active_waiters")
    private int activeWaiters;

    @JsonProperty("active_kitchen")
    private int activeKitchen;

    @JsonProperty("active_bar")
    private int activeBar;

    public DecisionRequest() {
    }

    /*
     * Constructor păstrat pentru compatibilitate
     * cu testele și codul existent.
     */
    public DecisionRequest(int dayOfWeek, int hour, int activeOrders, int occupiedTables, int estimatedOccupancy, int kitchenLoad, int barLoad, int avgPreparationTime, int ordersLast30Min, int orderAgeMinutes, int itemCount) {
        this(dayOfWeek, hour, activeOrders, occupiedTables, estimatedOccupancy, kitchenLoad, barLoad, avgPreparationTime, ordersLast30Min, orderAgeMinutes, itemCount, 0, 0, 0);
    }

    public DecisionRequest(int dayOfWeek, int hour, int activeOrders, int occupiedTables, int estimatedOccupancy, int kitchenLoad, int barLoad, int avgPreparationTime, int ordersLast30Min, int orderAgeMinutes, int itemCount, int activeWaiters, int activeKitchen, int activeBar) {
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.activeOrders = activeOrders;
        this.occupiedTables = occupiedTables;
        this.estimatedOccupancy = estimatedOccupancy;
        this.kitchenLoad = kitchenLoad;
        this.barLoad = barLoad;
        this.avgPreparationTime = avgPreparationTime;
        this.ordersLast30Min = ordersLast30Min;
        this.orderAgeMinutes = orderAgeMinutes;
        this.itemCount = itemCount;
        this.activeWaiters = activeWaiters;
        this.activeKitchen = activeKitchen;
        this.activeBar = activeBar;
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

    public int getActiveWaiters() {
        return activeWaiters;
    }

    public void setActiveWaiters(int activeWaiters) {
        this.activeWaiters = activeWaiters;
    }

    public int getActiveKitchen() {
        return activeKitchen;
    }

    public void setActiveKitchen(int activeKitchen) {
        this.activeKitchen = activeKitchen;
    }

    public int getActiveBar() {
        return activeBar;
    }

    public void setActiveBar(int activeBar) {
        this.activeBar = activeBar;
    }
}