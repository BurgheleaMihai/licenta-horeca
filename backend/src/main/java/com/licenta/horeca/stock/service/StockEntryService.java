package com.licenta.horeca.stock.service;

import com.licenta.horeca.stock.dto.StockEntryRequest;
import com.licenta.horeca.stock.entity.AuxiliarySupply;
import com.licenta.horeca.stock.entity.StockEntry;
import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.enums.StockPackageType;
import com.licenta.horeca.stock.repository.AuxiliarySupplyRepository;
import com.licenta.horeca.stock.repository.StockEntryRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StockEntryService {

    private static final BigDecimal ONE_THOUSAND =
            BigDecimal.valueOf(1000);

    private final StockEntryRepository stockEntryRepository;

    private final AuxiliarySupplyRepository
            auxiliarySupplyRepository;

    public StockEntryService(
            StockEntryRepository stockEntryRepository,
            AuxiliarySupplyRepository auxiliarySupplyRepository) {

        this.stockEntryRepository =
                stockEntryRepository;

        this.auxiliarySupplyRepository =
                auxiliarySupplyRepository;
    }

    public List<StockEntry> getEntriesForSupply(
            Long supplyId) {

        getSupplyById(supplyId);

        return stockEntryRepository
                .findBySupplyIdOrderByCreatedAtDesc(
                        supplyId
                );
    }

    public StockEntry getEntryById(Long entryId) {
        return stockEntryRepository
                .findById(entryId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Intrarea de stoc nu a fost gasita."
                        )
                );
    }

    @Transactional
    public StockEntry addStockEntry(
            Long supplyId,
            StockEntryRequest request) {

        AuxiliarySupply supply =
                getSupplyById(supplyId);

        validateRequest(request);

        BigDecimal packageQuantity =
                request.getPackageQuantity();

        BigDecimal quantityPerPackage =
                determineQuantityPerPackage(request);

        BigDecimal convertedQuantity =
                calculateConvertedQuantity(
                        packageQuantity,
                        quantityPerPackage,
                        request.getInputUnit(),
                        supply.getBaseUnit()
                );

        BigDecimal previousQuantity =
                getCurrentQuantity(supply);

        BigDecimal newQuantity =
                previousQuantity.add(
                        convertedQuantity
                );

        updateSupplyQuantity(
                supply,
                newQuantity
        );

        auxiliarySupplyRepository.save(supply);

        StockEntry stockEntry =
                new StockEntry();

        applyEntryData(
                stockEntry,
                supply,
                request,
                packageQuantity,
                quantityPerPackage,
                convertedQuantity,
                previousQuantity,
                newQuantity
        );

        stockEntry.setCreatedAt(
                LocalDateTime.now()
        );

        return stockEntryRepository.save(
                stockEntry
        );
    }

    @Transactional
    public StockEntry updateStockEntry(
            Long entryId,
            StockEntryRequest request) {

        validateRequest(request);

        StockEntry stockEntry =
                getEntryById(entryId);

        AuxiliarySupply oldSupply =
                stockEntry.getSupply();

        AuxiliarySupply targetSupply =
                request.getSupplyId() == null
                        ? oldSupply
                        : getSupplyById(
                        request.getSupplyId()
                );

        BigDecimal oldConvertedQuantity =
                stockEntry.getConvertedQuantity();

        BigDecimal oldSupplyCurrentQuantity =
                getCurrentQuantity(oldSupply);

        BigDecimal oldSupplyQuantityAfterRemoval =
                subtractWithoutNegativeResult(
                        oldSupplyCurrentQuantity,
                        oldConvertedQuantity
                );

        BigDecimal packageQuantity =
                request.getPackageQuantity();

        BigDecimal quantityPerPackage =
                determineQuantityPerPackage(request);

        BigDecimal newConvertedQuantity =
                calculateConvertedQuantity(
                        packageQuantity,
                        quantityPerPackage,
                        request.getInputUnit(),
                        targetSupply.getBaseUnit()
                );

        BigDecimal targetPreviousQuantity;
        BigDecimal targetNewQuantity;

        boolean sameSupply =
                oldSupply.getId().equals(
                        targetSupply.getId()
                );

        if (sameSupply) {
            targetPreviousQuantity =
                    oldSupplyQuantityAfterRemoval;

            targetNewQuantity =
                    targetPreviousQuantity.add(
                            newConvertedQuantity
                    );

            updateSupplyQuantity(
                    oldSupply,
                    targetNewQuantity
            );

            auxiliarySupplyRepository.save(
                    oldSupply
            );
        } else {
            updateSupplyQuantity(
                    oldSupply,
                    oldSupplyQuantityAfterRemoval
            );

            auxiliarySupplyRepository.save(
                    oldSupply
            );

            targetPreviousQuantity =
                    getCurrentQuantity(
                            targetSupply
                    );

            targetNewQuantity =
                    targetPreviousQuantity.add(
                            newConvertedQuantity
                    );

            updateSupplyQuantity(
                    targetSupply,
                    targetNewQuantity
            );

            auxiliarySupplyRepository.save(
                    targetSupply
            );
        }

        applyEntryData(
                stockEntry,
                targetSupply,
                request,
                packageQuantity,
                quantityPerPackage,
                newConvertedQuantity,
                targetPreviousQuantity,
                targetNewQuantity
        );

        return stockEntryRepository.save(
                stockEntry
        );
    }

    @Transactional
    public void deleteStockEntry(Long entryId) {
        StockEntry stockEntry =
                getEntryById(entryId);

        AuxiliarySupply supply =
                stockEntry.getSupply();

        BigDecimal currentQuantity =
                getCurrentQuantity(supply);

        BigDecimal quantityAfterDeletion =
                subtractWithoutNegativeResult(
                        currentQuantity,
                        stockEntry.getConvertedQuantity()
                );

        updateSupplyQuantity(
                supply,
                quantityAfterDeletion
        );

        auxiliarySupplyRepository.save(supply);

        stockEntryRepository.delete(stockEntry);
    }

    private void applyEntryData(
            StockEntry stockEntry,
            AuxiliarySupply supply,
            StockEntryRequest request,
            BigDecimal packageQuantity,
            BigDecimal quantityPerPackage,
            BigDecimal convertedQuantity,
            BigDecimal previousQuantity,
            BigDecimal newQuantity) {

        stockEntry.setSupply(supply);

        stockEntry.setPackageQuantity(
                packageQuantity
        );

        stockEntry.setPackageType(
                request.getPackageType()
        );

        stockEntry.setQuantityPerPackage(
                quantityPerPackage
        );

        stockEntry.setInputUnit(
                request.getInputUnit()
        );

        stockEntry.setConvertedQuantity(
                convertedQuantity
        );

        stockEntry.setPreviousQuantity(
                previousQuantity
        );

        stockEntry.setNewQuantity(
                newQuantity
        );

        stockEntry.setNotes(
                normalizeNotes(
                        request.getNotes()
                )
        );
    }

    private AuxiliarySupply getSupplyById(
            Long supplyId) {

        return auxiliarySupplyRepository
                .findById(supplyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Varianta articolului de stoc nu a fost gasita."
                        )
                );
    }

    private BigDecimal getCurrentQuantity(
            AuxiliarySupply supply) {

        if (supply.getCurrentQuantity() == null) {
            return BigDecimal.ZERO;
        }

        return supply.getCurrentQuantity();
    }

    private BigDecimal subtractWithoutNegativeResult(
            BigDecimal currentQuantity,
            BigDecimal quantityToRemove) {

        BigDecimal result =
                currentQuantity.subtract(
                        quantityToRemove
                );

        if (result.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO.setScale(
                    3,
                    RoundingMode.HALF_UP
            );
        }

        return normalizeQuantity(result);
    }

    private void updateSupplyQuantity(
            AuxiliarySupply supply,
            BigDecimal newQuantity) {

        BigDecimal normalizedQuantity =
                normalizeQuantity(newQuantity);

        supply.setCurrentQuantity(
                normalizedQuantity
        );

        boolean available =
                normalizedQuantity.compareTo(
                        BigDecimal.ZERO
                ) > 0;

        supply.setAvailableInWarehouse(
                available
        );

        if (available) {
            supply.setReportedAt(null);
        } else if (supply.getReportedAt() == null) {
            supply.setReportedAt(
                    LocalDateTime.now()
            );
        }
    }

    private BigDecimal calculateConvertedQuantity(
            BigDecimal packageQuantity,
            BigDecimal quantityPerPackage,
            MeasurementUnit inputUnit,
            MeasurementUnit baseUnit) {

        BigDecimal totalInputQuantity =
                packageQuantity.multiply(
                        quantityPerPackage
                );

        return convertQuantity(
                totalInputQuantity,
                inputUnit,
                baseUnit
        );
    }

    private BigDecimal determineQuantityPerPackage(
            StockEntryRequest request) {

        if (request.getPackageType()
                == StockPackageType.DIRECT) {

            return BigDecimal.ONE;
        }

        return request.getQuantityPerPackage();
    }

    private BigDecimal convertQuantity(
            BigDecimal quantity,
            MeasurementUnit inputUnit,
            MeasurementUnit baseUnit) {

        if (inputUnit == baseUnit) {
            return normalizeQuantity(quantity);
        }

        if (inputUnit == MeasurementUnit.KILOGRAM
                && baseUnit == MeasurementUnit.GRAM) {

            return normalizeQuantity(
                    quantity.multiply(
                            ONE_THOUSAND
                    )
            );
        }

        if (inputUnit == MeasurementUnit.GRAM
                && baseUnit == MeasurementUnit.KILOGRAM) {

            return normalizeQuantity(
                    quantity.divide(
                            ONE_THOUSAND,
                            3,
                            RoundingMode.HALF_UP
                    )
            );
        }

        if (inputUnit == MeasurementUnit.LITER
                && baseUnit == MeasurementUnit.MILLILITER) {

            return normalizeQuantity(
                    quantity.multiply(
                            ONE_THOUSAND
                    )
            );
        }

        if (inputUnit == MeasurementUnit.MILLILITER
                && baseUnit == MeasurementUnit.LITER) {

            return normalizeQuantity(
                    quantity.divide(
                            ONE_THOUSAND,
                            3,
                            RoundingMode.HALF_UP
                    )
            );
        }

        throw new RuntimeException(
                "Unitatea introdusa nu poate fi convertita "
                        + "in unitatea de baza a variantei."
        );
    }

    private BigDecimal normalizeQuantity(
            BigDecimal quantity) {

        return quantity.setScale(
                3,
                RoundingMode.HALF_UP
        );
    }

    private String normalizeNotes(String notes) {
        if (notes == null
                || notes.trim().isEmpty()) {

            return null;
        }

        return notes.trim();
    }

    private void validateRequest(
            StockEntryRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Datele intrarii de stoc sunt obligatorii."
            );
        }

        if (request.getPackageType() == null) {
            request.setPackageType(
                    StockPackageType.DIRECT
            );
        }

        if (request.getPackageQuantity() == null
                || request.getPackageQuantity()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Cantitatea primita trebuie sa fie mai mare decat zero."
            );
        }

        /*
         * Pentru cutii, pachete, baxuri, saci si sticle,
         * numarul ambalajelor trebuie sa fie intreg.
         *
         * Cantitatea directa poate fi zecimala:
         * 1.5 kg, 0.750 l etc.
         */
        if (request.getPackageType()
                != StockPackageType.DIRECT
                && request.getPackageQuantity()
                .stripTrailingZeros()
                .scale() > 0) {

            throw new RuntimeException(
                    "Numarul de ambalaje trebuie sa fie un numar intreg."
            );
        }

        if (request.getInputUnit() == null) {
            throw new RuntimeException(
                    "Unitatea cantitatii primite este obligatorie."
            );
        }

        if (request.getPackageType()
                != StockPackageType.DIRECT) {

            if (request.getQuantityPerPackage() == null
                    || request.getQuantityPerPackage()
                    .compareTo(BigDecimal.ZERO) <= 0) {

                throw new RuntimeException(
                        "Cantitatea dintr-un ambalaj "
                                + "trebuie sa fie mai mare decat zero."
                );
            }
        }
    }
}