package com.licenta.horeca.order.dto;

import java.math.BigDecimal;

public class OrderStatisticsResponse {

    private int activeOrders;
    private int servedOrders;
    private BigDecimal sales;
    private double averageRating;

    public OrderStatisticsResponse(int activeOrders, int servedOrders, BigDecimal sales, double averageRating) {
        this.activeOrders = activeOrders;
        this.servedOrders = servedOrders;
        this.sales = sales;
        this.averageRating = averageRating;
    }

    public int getActiveOrders() {
        return activeOrders;
    }

    public void setActiveOrders(int activeOrders) {
        this.activeOrders = activeOrders;
    }

    public int getServedOrders() {
        return servedOrders;
    }

    public void setServedOrders(int servedOrders) {
        this.servedOrders = servedOrders;
    }

    public BigDecimal getSales() {
        return sales;
    }

    public void setSales(BigDecimal sales) {
        this.sales = sales;
    }

    public double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
}