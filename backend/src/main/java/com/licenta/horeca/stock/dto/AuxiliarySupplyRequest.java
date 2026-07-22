package com.licenta.horeca.stock.dto;

import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.stock.enums.StockCategory;
import com.licenta.horeca.stock.enums.StockType;
import java.math.BigDecimal;

public class AuxiliarySupplyRequest {

    private String name;

    private String variantName;

    private BigDecimal specificationValue;

    private MeasurementUnit specificationUnit;

    private StockType stockType;

    private StockCategory category;

    private MeasurementUnit baseUnit;

    private BigDecimal currentQuantity;

    private BigDecimal minimumQuantity;

    private Boolean active;

    public AuxiliarySupplyRequest() {
    }

    public String getName() {
        return name;
    }

    public String getVariantName() {
        return variantName;
    }

    public BigDecimal getSpecificationValue() {
        return specificationValue;
    }

    public MeasurementUnit getSpecificationUnit() {
        return specificationUnit;
    }

    public StockType getStockType() {
        return stockType;
    }

    public StockCategory getCategory() {
        return category;
    }

    public MeasurementUnit getBaseUnit() {
        return baseUnit;
    }

    public BigDecimal getCurrentQuantity() {
        return currentQuantity;
    }

    public BigDecimal getMinimumQuantity() {
        return minimumQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public void setSpecificationValue(
            BigDecimal specificationValue) {

        this.specificationValue = specificationValue;
    }

    public void setSpecificationUnit(
            MeasurementUnit specificationUnit) {

        this.specificationUnit = specificationUnit;
    }

    public void setStockType(StockType stockType) {
        this.stockType = stockType;
    }

    public void setCategory(StockCategory category) {
        this.category = category;
    }

    public void setBaseUnit(MeasurementUnit baseUnit) {
        this.baseUnit = baseUnit;
    }

    public void setCurrentQuantity(
            BigDecimal currentQuantity) {

        this.currentQuantity = currentQuantity;
    }

    public void setMinimumQuantity(
            BigDecimal minimumQuantity) {

        this.minimumQuantity = minimumQuantity;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }
}