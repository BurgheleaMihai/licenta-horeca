package com.licenta.horeca.entity;

import com.licenta.horeca.enums.MeasurementUnit;
import com.licenta.horeca.enums.StockPackageType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_entries")
public class StockEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supply_id", nullable = false)
    private AuxiliarySupply supply;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal packageQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StockPackageType packageType;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal quantityPerPackage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MeasurementUnit inputUnit;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal convertedQuantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal previousQuantity;

    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal newQuantity;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(length = 500)
    private String notes;

    public StockEntry() {
    }

    public Long getId() {
        return id;
    }

    public AuxiliarySupply getSupply() {
        return supply;
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

    public BigDecimal getConvertedQuantity() {
        return convertedQuantity;
    }

    public BigDecimal getPreviousQuantity() {
        return previousQuantity;
    }

    public BigDecimal getNewQuantity() {
        return newQuantity;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setSupply(AuxiliarySupply supply) {
        this.supply = supply;
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

    public void setConvertedQuantity(BigDecimal convertedQuantity) {
        this.convertedQuantity = convertedQuantity;
    }

    public void setPreviousQuantity(BigDecimal previousQuantity) {
        this.previousQuantity = previousQuantity;
    }

    public void setNewQuantity(BigDecimal newQuantity) {
        this.newQuantity = newQuantity;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}