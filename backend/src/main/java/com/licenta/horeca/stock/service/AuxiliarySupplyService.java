package com.licenta.horeca.stock.service;

import com.licenta.horeca.stock.dto.AuxiliarySupplyRequest;
import com.licenta.horeca.stock.entity.AuxiliarySupply;
import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.stock.enums.StockCategory;
import com.licenta.horeca.stock.enums.StockType;
import com.licenta.horeca.stock.repository.AuxiliarySupplyRepository;
import com.licenta.horeca.stock.repository.StockEntryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuxiliarySupplyService {

    private final AuxiliarySupplyRepository auxiliarySupplyRepository;

    private final StockEntryRepository stockEntryRepository;

    public AuxiliarySupplyService(
            AuxiliarySupplyRepository auxiliarySupplyRepository,
            StockEntryRepository stockEntryRepository) {

        this.auxiliarySupplyRepository =
                auxiliarySupplyRepository;

        this.stockEntryRepository =
                stockEntryRepository;
    }

    public List<AuxiliarySupply> getAllSupplies() {
        return auxiliarySupplyRepository
                .findAllOrderedByStockTypeNameAndVariant();
    }

    public List<AuxiliarySupply> getAllActiveSupplies() {
        return auxiliarySupplyRepository
                .findAllActiveOrderedByStockTypeNameAndVariant();
    }

    public List<AuxiliarySupply> getUnavailableSupplies() {
        return auxiliarySupplyRepository
                .findByAvailableInWarehouseFalseAndActiveTrueOrderByNameAscVariantNameAsc();
    }

    public AuxiliarySupply getSupplyById(Long supplyId) {
        return auxiliarySupplyRepository
                .findById(supplyId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Varianta articolului de stoc nu a fost gasita."
                        )
                );
    }

    public AuxiliarySupply createSupply(
            AuxiliarySupplyRequest request) {

        validateRequest(request);

        String normalizedName =
                request.getName().trim();

        String normalizedVariant =
                normalizeVariantName(request);

        boolean alreadyExists;

        if (normalizedVariant == null) {
            alreadyExists = auxiliarySupplyRepository
                    .existsByNameIgnoreCaseAndVariantNameIsNull(
                            normalizedName
                    );
        } else {
            alreadyExists = auxiliarySupplyRepository
                    .existsByNameIgnoreCaseAndVariantNameIgnoreCase(
                            normalizedName,
                            normalizedVariant
                    );
        }

        if (alreadyExists) {
            throw new RuntimeException(
                    "Exista deja aceasta varianta pentru produsul selectat."
            );
        }

        AuxiliarySupply supply =
                new AuxiliarySupply();

        applyRequestData(
                supply,
                request,
                normalizedVariant
        );

        updateAvailabilityFromQuantity(supply);

        return auxiliarySupplyRepository.save(supply);
    }

    public AuxiliarySupply updateSupply(
            Long supplyId,
            AuxiliarySupplyRequest request) {

        validateRequest(request);

        AuxiliarySupply supply =
                getSupplyById(supplyId);

        String normalizedVariant =
                normalizeVariantName(request);

        applyRequestData(
                supply,
                request,
                normalizedVariant
        );

        updateAvailabilityFromQuantity(supply);

        return auxiliarySupplyRepository.save(supply);
    }

    public AuxiliarySupply markUnavailable(
            Long supplyId) {

        AuxiliarySupply supply =
                getSupplyById(supplyId);

        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(LocalDateTime.now());

        return auxiliarySupplyRepository.save(supply);
    }

    public AuxiliarySupply markAvailable(
            Long supplyId) {

        AuxiliarySupply supply =
                getSupplyById(supplyId);

        supply.setAvailableInWarehouse(true);
        supply.setReportedAt(null);

        return auxiliarySupplyRepository.save(supply);
    }

    @Transactional
    public void deleteSupply(Long supplyId) {
        AuxiliarySupply supply =
                getSupplyById(supplyId);

        /*
         * Intrările trebuie șterse înaintea variantei,
         * deoarece stock_entries conține cheia externă
         * către auxiliary_supplies.
         */
        stockEntryRepository.deleteBySupplyId(supplyId);

        auxiliarySupplyRepository.delete(supply);
    }

    private void applyRequestData(
            AuxiliarySupply supply,
            AuxiliarySupplyRequest request,
            String normalizedVariant) {

        supply.setName(request.getName().trim());

        supply.setVariantName(
                normalizedVariant
        );

        supply.setSpecificationValue(
                request.getSpecificationValue()
        );

        supply.setSpecificationUnit(
                request.getSpecificationUnit()
        );

        supply.setStockType(
                request.getStockType()
        );

        supply.setCategory(
                request.getCategory()
        );

        supply.setBaseUnit(
                request.getBaseUnit()
        );

        supply.setCurrentQuantity(
                request.getCurrentQuantity()
        );

        supply.setMinimumQuantity(
                request.getMinimumQuantity()
        );

        supply.setActive(
                request.getActive() == null
                        || request.getActive()
        );
    }

    private String normalizeVariantName(
            AuxiliarySupplyRequest request) {

        if (request.getVariantName() != null
                && !request.getVariantName()
                .trim()
                .isEmpty()) {

            return request
                    .getVariantName()
                    .trim();
        }

        if (request.getSpecificationValue() != null
                && request.getSpecificationUnit() != null) {

            String value = request
                    .getSpecificationValue()
                    .stripTrailingZeros()
                    .toPlainString();

            return value
                    + " "
                    + getUnitLabel(
                    request.getSpecificationUnit()
            );
        }

        return null;
    }

    private String getUnitLabel(
            MeasurementUnit unit) {

        return switch (unit) {
            case PIECE -> "buc";
            case GRAM -> "g";
            case KILOGRAM -> "kg";
            case MILLILITER -> "ml";
            case LITER -> "l";
        };
    }

    private void updateAvailabilityFromQuantity(
            AuxiliarySupply supply) {

        boolean available =
                supply.getCurrentQuantity()
                        .compareTo(BigDecimal.ZERO) > 0;

        supply.setAvailableInWarehouse(available);

        if (available) {
            supply.setReportedAt(null);
        } else if (supply.getReportedAt() == null) {
            supply.setReportedAt(
                    LocalDateTime.now()
            );
        }
    }

    private void validateRequest(
            AuxiliarySupplyRequest request) {

        if (request == null) {
            throw new RuntimeException(
                    "Datele articolului de stoc sunt obligatorii."
            );
        }

        if (request.getName() == null
                || request.getName()
                .trim()
                .isEmpty()) {

            throw new RuntimeException(
                    "Denumirea produsului este obligatorie."
            );
        }

        if (request.getStockType() == null) {
            request.setStockType(
                    StockType.AUXILIARY
            );
        }

        if (request.getCategory() == null) {
            request.setCategory(
                    StockCategory.OTHER
            );
        }

        if (request.getBaseUnit() == null) {
            request.setBaseUnit(
                    MeasurementUnit.PIECE
            );
        }

        if (request.getCurrentQuantity() == null) {
            request.setCurrentQuantity(
                    BigDecimal.ZERO
            );
        }

        if (request.getMinimumQuantity() == null) {
            request.setMinimumQuantity(
                    BigDecimal.ZERO
            );
        }

        if (request.getCurrentQuantity()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Cantitatea curenta nu poate fi negativa."
            );
        }

        if (request.getMinimumQuantity()
                .compareTo(BigDecimal.ZERO) < 0) {

            throw new RuntimeException(
                    "Pragul minim nu poate fi negativ."
            );
        }

        if (request.getSpecificationValue() != null
                && request.getSpecificationValue()
                .compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "Valoarea specificatiei trebuie sa fie mai mare decat zero."
            );
        }

        if (request.getSpecificationValue() != null
                && request.getSpecificationUnit() == null) {

            throw new RuntimeException(
                    "Unitatea specificatiei este obligatorie."
            );
        }

        if (request.getSpecificationValue() == null) {
            request.setSpecificationUnit(null);
        }
    }
}