package com.licenta.horeca.stock.dto;

import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.stock.enums.StockPackageType;

import java.math.BigDecimal;

public class StockEntryRequest {

    /*
     * Folosit la modificarea unei intrări.
     * Permite mutarea intrării către altă variantă.
     */
    private Long supplyId;

    private BigDecimal packageQuantity;

    private StockPackageType packageType;

    private BigDecimal quantityPerPackage;

    private MeasurementUnit inputUnit;

    private String notes;

    public StockEntryRequest() {
    }

    public Long getSupplyId() {
        return supplyId;
    }

    public BigDecimal getPackageQuantity() {
        return packageQuantity;
    }

    public StockPackageType getPackageType() {
        return packageType;
    }

    public BigDecimal getQuantityPerPackage() {
        return quantityPerPackage;
    }

    public MeasurementUnit getInputUnit() {
        return inputUnit;
    }

    public String getNotes() {
        return notes;
    }

    public void setSupplyId(Long supplyId) {
        this.supplyId = supplyId;
    }

    public void setPackageQuantity(BigDecimal packageQuantity) {

        this.packageQuantity = packageQuantity;
    }

    public void setPackageType(StockPackageType packageType) {

        this.packageType = packageType;
    }

    public void setQuantityPerPackage(BigDecimal quantityPerPackage) {

        this.quantityPerPackage = quantityPerPackage;
    }

    public void setInputUnit(MeasurementUnit inputUnit) {

        this.inputUnit = inputUnit;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}