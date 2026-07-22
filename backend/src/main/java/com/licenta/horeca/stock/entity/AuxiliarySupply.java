package com.licenta.horeca.stock.entity;

import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.stock.enums.StockCategory;
import com.licenta.horeca.stock.enums.StockType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auxiliary_supplies")
public class AuxiliarySupply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Denumirea produsului de baza.
     * Exemplu: Pahare carton, Carne, Sirop.
     */
    @Column(nullable = false)
    private String name;

    /*
     * Varianta produsului.
     * Exemplu: 200 ml, Large, Pui, Porc.
     */
    private String variantName;

    /*
     * Valoarea numerica a specificatiei.
     * Exemplu: 200 pentru varianta de 200 ml.
     */
    @Column(precision = 12, scale = 3)
    private BigDecimal specificationValue;

    /*
     * Unitatea specificatiei variantei.
     * Nu este unitatea in care se tine stocul.
     */
    @Enumerated(EnumType.STRING)
    private MeasurementUnit specificationUnit;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockType stockType = StockType.AUXILIARY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockCategory category = StockCategory.OTHER;

    /*
     * Unitatea in care se tine stocul.
     * Exemplu: un pahar de 200 ml se numara in PIECE.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasurementUnit baseUnit = MeasurementUnit.PIECE;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal currentQuantity = BigDecimal.ZERO;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal minimumQuantity = BigDecimal.ZERO;

    @Column(nullable = false)
    private boolean availableInWarehouse = true;

    @Column(nullable = false)
    private boolean active = true;

    private LocalDateTime reportedAt;

    public AuxiliarySupply() {
    }

    public Long getId() {
        return id;
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

    public boolean isAvailableInWarehouse() {
        return availableInWarehouse;
    }

    public boolean isActive() {
        return active;
    }

    public LocalDateTime getReportedAt() {
        return reportedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setVariantName(String variantName) {
        this.variantName = variantName;
    }

    public void setSpecificationValue(BigDecimal specificationValue) {

        this.specificationValue = specificationValue;
    }

    public void setSpecificationUnit(MeasurementUnit specificationUnit) {

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

    public void setCurrentQuantity(BigDecimal currentQuantity) {
        this.currentQuantity = currentQuantity;
    }

    public void setMinimumQuantity(BigDecimal minimumQuantity) {
        this.minimumQuantity = minimumQuantity;
    }

    public void setAvailableInWarehouse(boolean availableInWarehouse) {

        this.availableInWarehouse = availableInWarehouse;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public void setReportedAt(LocalDateTime reportedAt) {
        this.reportedAt = reportedAt;
    }
}