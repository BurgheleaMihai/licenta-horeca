package com.licenta.horeca.employee.dto.shift;

public class ActiveStaffSummaryResponse {

    private long waiters;

    private long kitchenEmployees;

    private long barEmployees;

    private long totalOperationalEmployees;

    public ActiveStaffSummaryResponse() {
    }

    public ActiveStaffSummaryResponse(long waiters, long kitchenEmployees, long barEmployees) {
        this.waiters = waiters;
        this.kitchenEmployees = kitchenEmployees;
        this.barEmployees = barEmployees;
        this.totalOperationalEmployees = waiters + kitchenEmployees + barEmployees;
    }

    public long getWaiters() {
        return waiters;
    }

    public void setWaiters(long waiters) {
        this.waiters = waiters;
    }

    public long getKitchenEmployees() {
        return kitchenEmployees;
    }

    public void setKitchenEmployees(long kitchenEmployees) {
        this.kitchenEmployees = kitchenEmployees;
    }

    public long getBarEmployees() {
        return barEmployees;
    }

    public void setBarEmployees(long barEmployees) {
        this.barEmployees = barEmployees;
    }

    public long getTotalOperationalEmployees() {
        return totalOperationalEmployees;
    }

    public void setTotalOperationalEmployees(long totalOperationalEmployees) {
        this.totalOperationalEmployees = totalOperationalEmployees;
    }
}