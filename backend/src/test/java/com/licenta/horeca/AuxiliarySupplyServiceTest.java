package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licenta.horeca.dto.AuxiliarySupplyRequest;
import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.enums.MeasurementUnit;
import com.licenta.horeca.enums.StockCategory;
import com.licenta.horeca.enums.StockType;
import com.licenta.horeca.repository.AuxiliarySupplyRepository;
import com.licenta.horeca.repository.StockEntryRepository;
import com.licenta.horeca.service.AuxiliarySupplyService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuxiliarySupplyServiceTest {

    private static final Long SUPPLY_ID = 1L;

    @Mock
    private AuxiliarySupplyRepository auxiliarySupplyRepository;

    @Mock
    private StockEntryRepository stockEntryRepository;

    @InjectMocks
    private AuxiliarySupplyService auxiliarySupplyService;

    @Test
    void getAllSuppliesReturnsOrderedSupplies() {
        AuxiliarySupply cups200 =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("20")
                );

        AuxiliarySupply cups400 =
                createSupply(
                        "Pahare carton",
                        "400 ml",
                        new BigDecimal("10")
                );

        when(auxiliarySupplyRepository
                .findAllOrderedByStockTypeNameAndVariant())
                .thenReturn(List.of(cups200, cups400));

        List<AuxiliarySupply> result =
                auxiliarySupplyService.getAllSupplies();

        assertEquals(2, result.size());
        assertEquals("200 ml", result.get(0).getVariantName());
        assertEquals("400 ml", result.get(1).getVariantName());

        verify(auxiliarySupplyRepository)
                .findAllOrderedByStockTypeNameAndVariant();
    }

    @Test
    void getAllActiveSuppliesReturnsOnlyActiveSupplies() {
        AuxiliarySupply cups =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.TEN
                );

        when(auxiliarySupplyRepository
                .findAllActiveOrderedByStockTypeNameAndVariant())
                .thenReturn(List.of(cups));

        List<AuxiliarySupply> result =
                auxiliarySupplyService.getAllActiveSupplies();

        assertEquals(1, result.size());
        assertTrue(result.get(0).isActive());
    }

    @Test
    void getUnavailableSuppliesReturnsUnavailableActiveSupplies() {
        AuxiliarySupply cups =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.ZERO
                );

        cups.setAvailableInWarehouse(false);

        when(auxiliarySupplyRepository
                .findByAvailableInWarehouseFalseAndActiveTrueOrderByNameAscVariantNameAsc())
                .thenReturn(List.of(cups));

        List<AuxiliarySupply> result =
                auxiliarySupplyService.getUnavailableSupplies();

        assertEquals(1, result.size());
        assertFalse(result.get(0).isAvailableInWarehouse());
    }

    @Test
    void createSupplyCreatesNumericVariantAndMarksItAvailable() {
        AuxiliarySupplyRequest request =
                createRequest(
                        "Pahare carton",
                        null,
                        new BigDecimal("200"),
                        MeasurementUnit.MILLILITER,
                        new BigDecimal("50"),
                        new BigDecimal("10")
                );

        when(auxiliarySupplyRepository
                .existsByNameIgnoreCaseAndVariantNameIgnoreCase(
                        "Pahare carton",
                        "200 ml"
                ))
                .thenReturn(false);

        when(auxiliarySupplyRepository.save(
                org.mockito.ArgumentMatchers.any(
                        AuxiliarySupply.class
                )
        ))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuxiliarySupply result =
                auxiliarySupplyService.createSupply(request);

        assertEquals("Pahare carton", result.getName());
        assertEquals("200 ml", result.getVariantName());
        assertEquals(
                new BigDecimal("200"),
                result.getSpecificationValue()
        );
        assertEquals(
                MeasurementUnit.MILLILITER,
                result.getSpecificationUnit()
        );
        assertTrue(result.isAvailableInWarehouse());
        assertNull(result.getReportedAt());
    }

    @Test
    void createSupplyMarksZeroQuantityAsUnavailable() {
        AuxiliarySupplyRequest request =
                createRequest(
                        "Pahare carton",
                        "Large",
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.TEN
                );

        when(auxiliarySupplyRepository
                .existsByNameIgnoreCaseAndVariantNameIgnoreCase(
                        "Pahare carton",
                        "Large"
                ))
                .thenReturn(false);

        when(auxiliarySupplyRepository.save(
                org.mockito.ArgumentMatchers.any(
                        AuxiliarySupply.class
                )
        ))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        AuxiliarySupply result =
                auxiliarySupplyService.createSupply(request);

        assertFalse(result.isAvailableInWarehouse());
        assertNotNull(result.getReportedAt());
    }

    @Test
    void createSupplyRejectsDuplicateVariant() {
        AuxiliarySupplyRequest request =
                createRequest(
                        "Pahare carton",
                        "200 ml",
                        null,
                        null,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        when(auxiliarySupplyRepository
                .existsByNameIgnoreCaseAndVariantNameIgnoreCase(
                        "Pahare carton",
                        "200 ml"
                ))
                .thenReturn(true);

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> auxiliarySupplyService.createSupply(
                        request
                )
        );

        assertEquals(
                "Exista deja aceasta varianta pentru produsul selectat.",
                exception.getMessage()
        );

        verify(auxiliarySupplyRepository, never())
                .save(
                        org.mockito.ArgumentMatchers.any()
                );
    }

    @Test
    void updateSupplyUpdatesVariantAndAvailability() {
        AuxiliarySupply existing =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.ZERO
                );

        existing.setAvailableInWarehouse(false);

        AuxiliarySupplyRequest request =
                createRequest(
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("200"),
                        MeasurementUnit.MILLILITER,
                        new BigDecimal("75"),
                        new BigDecimal("20")
                );

        when(auxiliarySupplyRepository.findById(SUPPLY_ID))
                .thenReturn(Optional.of(existing));

        when(auxiliarySupplyRepository.save(existing))
                .thenReturn(existing);

        AuxiliarySupply result =
                auxiliarySupplyService.updateSupply(
                        SUPPLY_ID,
                        request
                );

        assertEquals(
                new BigDecimal("75"),
                result.getCurrentQuantity()
        );
        assertEquals(
                new BigDecimal("20"),
                result.getMinimumQuantity()
        );
        assertTrue(result.isAvailableInWarehouse());
        assertNull(result.getReportedAt());
    }

    @Test
    void markUnavailableSetsStatusAndReportedAt() {
        AuxiliarySupply supply =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.TEN
                );

        when(auxiliarySupplyRepository.findById(SUPPLY_ID))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply))
                .thenReturn(supply);

        AuxiliarySupply result =
                auxiliarySupplyService.markUnavailable(
                        SUPPLY_ID
                );

        assertFalse(result.isAvailableInWarehouse());
        assertNotNull(result.getReportedAt());

        verify(auxiliarySupplyRepository).save(supply);
    }

    @Test
    void markAvailableSetsStatusAndClearsReportedAt() {
        AuxiliarySupply supply =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.TEN
                );

        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(
                java.time.LocalDateTime.now()
        );

        when(auxiliarySupplyRepository.findById(SUPPLY_ID))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply))
                .thenReturn(supply);

        AuxiliarySupply result =
                auxiliarySupplyService.markAvailable(
                        SUPPLY_ID
                );

        assertTrue(result.isAvailableInWarehouse());
        assertNull(result.getReportedAt());
    }

    @Test
    void deleteSupplyDeletesEntriesBeforeSupply() {
        AuxiliarySupply supply =
                createSupply(
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.TEN
                );

        when(auxiliarySupplyRepository.findById(SUPPLY_ID))
                .thenReturn(Optional.of(supply));

        auxiliarySupplyService.deleteSupply(SUPPLY_ID);

        InOrder order = inOrder(
                stockEntryRepository,
                auxiliarySupplyRepository
        );

        order.verify(stockEntryRepository)
                .deleteBySupplyId(SUPPLY_ID);

        order.verify(auxiliarySupplyRepository)
                .delete(supply);
    }

    @Test
    void getSupplyByIdThrowsWhenSupplyDoesNotExist() {
        when(auxiliarySupplyRepository.findById(SUPPLY_ID))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> auxiliarySupplyService.getSupplyById(
                        SUPPLY_ID
                )
        );

        assertEquals(
                "Varianta articolului de stoc nu a fost gasita.",
                exception.getMessage()
        );
    }

    private AuxiliarySupply createSupply(
            String name,
            String variantName,
            BigDecimal currentQuantity) {

        AuxiliarySupply supply =
                new AuxiliarySupply();

        supply.setName(name);
        supply.setVariantName(variantName);
        supply.setStockType(StockType.AUXILIARY);
        supply.setCategory(StockCategory.PACKAGING);
        supply.setBaseUnit(MeasurementUnit.PIECE);
        supply.setCurrentQuantity(currentQuantity);
        supply.setMinimumQuantity(BigDecimal.TEN);
        supply.setAvailableInWarehouse(
                currentQuantity.compareTo(BigDecimal.ZERO) > 0
        );
        supply.setActive(true);

        return supply;
    }

    private AuxiliarySupplyRequest createRequest(
            String name,
            String variantName,
            BigDecimal specificationValue,
            MeasurementUnit specificationUnit,
            BigDecimal currentQuantity,
            BigDecimal minimumQuantity) {

        AuxiliarySupplyRequest request =
                new AuxiliarySupplyRequest();

        request.setName(name);
        request.setVariantName(variantName);
        request.setSpecificationValue(
                specificationValue
        );
        request.setSpecificationUnit(
                specificationUnit
        );
        request.setStockType(StockType.AUXILIARY);
        request.setCategory(StockCategory.PACKAGING);
        request.setBaseUnit(MeasurementUnit.PIECE);
        request.setCurrentQuantity(currentQuantity);
        request.setMinimumQuantity(minimumQuantity);
        request.setActive(true);

        return request;
    }
}