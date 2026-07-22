package com.licenta.horeca.stock.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.licenta.horeca.stock.dto.StockEntryRequest;
import com.licenta.horeca.stock.entity.AuxiliarySupply;
import com.licenta.horeca.stock.entity.StockEntry;
import com.licenta.horeca.stock.enums.MeasurementUnit;
import com.licenta.horeca.stock.enums.StockPackageType;
import com.licenta.horeca.stock.repository.AuxiliarySupplyRepository;
import com.licenta.horeca.stock.repository.StockEntryRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StockEntryServiceTest {

    @Mock
    private StockEntryRepository stockEntryRepository;

    @Mock
    private AuxiliarySupplyRepository auxiliarySupplyRepository;

    private StockEntryService stockEntryService;

    @BeforeEach
    void setUp() {
        stockEntryService = new StockEntryService(
                stockEntryRepository,
                auxiliarySupplyRepository
        );
    }

    @Test
    void getEntriesForSupplyShouldReturnEntries() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "10.000"
        );

        StockEntry firstEntry = new StockEntry();
        StockEntry secondEntry = new StockEntry();

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        when(
                stockEntryRepository
                        .findBySupplyIdOrderByCreatedAtDesc(1L)
        ).thenReturn(List.of(firstEntry, secondEntry));

        List<StockEntry> result =
                stockEntryService.getEntriesForSupply(1L);

        assertEquals(2, result.size());
        assertSame(firstEntry, result.get(0));
        assertSame(secondEntry, result.get(1));

        verify(auxiliarySupplyRepository)
                .findById(1L);

        verify(stockEntryRepository)
                .findBySupplyIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void getEntriesForSupplyShouldThrowWhenSupplyDoesNotExist() {
        when(auxiliarySupplyRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService
                        .getEntriesForSupply(99L)
        );

        assertEquals(
                "Varianta articolului de stoc nu a fost gasita.",
                exception.getMessage()
        );

        verify(
                stockEntryRepository,
                never()
        ).findBySupplyIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getEntryByIdShouldReturnEntry() {
        StockEntry entry = new StockEntry();

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        StockEntry result =
                stockEntryService.getEntryById(10L);

        assertSame(entry, result);
    }

    @Test
    void getEntryByIdShouldThrowWhenEntryDoesNotExist() {
        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.getEntryById(10L)
        );

        assertEquals(
                "Intrarea de stoc nu a fost gasita.",
                exception.getMessage()
        );
    }

    @Test
    void addStockEntryShouldAddDirectQuantityAndTrimNotes() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "5.000"
        );

        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(
                LocalDateTime.now().minusDays(1)
        );

        StockEntryRequest request = createRequest(
                null,
                "2.500",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                "  Marfa receptionata  "
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        configureStockEntrySave();

        StockEntry result =
                stockEntryService.addStockEntry(
                        1L,
                        request
                );

        assertNotNull(result);
        assertSame(supply, result.getSupply());

        assertDecimal(
                "2.500",
                result.getPackageQuantity()
        );

        assertEquals(
                StockPackageType.DIRECT,
                result.getPackageType()
        );

        assertDecimal(
                "1",
                result.getQuantityPerPackage()
        );

        assertEquals(
                MeasurementUnit.PIECE,
                result.getInputUnit()
        );

        assertDecimal(
                "2.500",
                result.getConvertedQuantity()
        );

        assertDecimal(
                "5.000",
                result.getPreviousQuantity()
        );

        assertDecimal(
                "7.500",
                result.getNewQuantity()
        );

        assertEquals(
                "Marfa receptionata",
                result.getNotes()
        );

        assertNotNull(result.getCreatedAt());

        assertDecimal(
                "7.500",
                supply.getCurrentQuantity()
        );

        assertTrue(supply.isAvailableInWarehouse());
        assertNull(supply.getReportedAt());

        verify(auxiliarySupplyRepository)
                .save(supply);

        verify(stockEntryRepository)
                .save(result);
    }

    @Test
    void addStockEntryShouldDefaultNullPackageTypeToDirect() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.KILOGRAM,
                null
        );

        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(
                LocalDateTime.now().minusHours(2)
        );

        StockEntryRequest request = createRequest(
                null,
                "1.500",
                null,
                null,
                MeasurementUnit.KILOGRAM,
                null
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        configureStockEntrySave();

        StockEntry result =
                stockEntryService.addStockEntry(
                        1L,
                        request
                );

        assertEquals(
                StockPackageType.DIRECT,
                request.getPackageType()
        );

        assertEquals(
                StockPackageType.DIRECT,
                result.getPackageType()
        );

        assertDecimal(
                "1",
                result.getQuantityPerPackage()
        );

        assertDecimal(
                "0",
                result.getPreviousQuantity()
        );

        assertDecimal(
                "1.500",
                result.getConvertedQuantity()
        );

        assertDecimal(
                "1.500",
                result.getNewQuantity()
        );

        assertNull(result.getNotes());

        assertDecimal(
                "1.500",
                supply.getCurrentQuantity()
        );

        assertTrue(supply.isAvailableInWarehouse());
        assertNull(supply.getReportedAt());
    }

    @Test
    void addStockEntryShouldSupportAllMassAndVolumeConversions() {
        AuxiliarySupply gramsSupply = createSupply(
                1L,
                MeasurementUnit.GRAM,
                "0.000"
        );

        AuxiliarySupply kilogramsSupply = createSupply(
                2L,
                MeasurementUnit.KILOGRAM,
                "0.000"
        );

        AuxiliarySupply millilitersSupply = createSupply(
                3L,
                MeasurementUnit.MILLILITER,
                "0.000"
        );

        AuxiliarySupply litersSupply = createSupply(
                4L,
                MeasurementUnit.LITER,
                "0.000"
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(gramsSupply));

        when(auxiliarySupplyRepository.findById(2L))
                .thenReturn(Optional.of(kilogramsSupply));

        when(auxiliarySupplyRepository.findById(3L))
                .thenReturn(Optional.of(millilitersSupply));

        when(auxiliarySupplyRepository.findById(4L))
                .thenReturn(Optional.of(litersSupply));

        configureStockEntrySave();

        StockEntry kilogramsToGrams =
                stockEntryService.addStockEntry(
                        1L,
                        createRequest(
                                null,
                                "2",
                                StockPackageType.BOX,
                                "1.500",
                                MeasurementUnit.KILOGRAM,
                                null
                        )
                );

        StockEntry gramsToKilograms =
                stockEntryService.addStockEntry(
                        2L,
                        createRequest(
                                null,
                                "2",
                                StockPackageType.PACK,
                                "500",
                                MeasurementUnit.GRAM,
                                null
                        )
                );

        StockEntry litersToMilliliters =
                stockEntryService.addStockEntry(
                        3L,
                        createRequest(
                                null,
                                "3",
                                StockPackageType.BOTTLE,
                                "1.500",
                                MeasurementUnit.LITER,
                                null
                        )
                );

        StockEntry millilitersToLiters =
                stockEntryService.addStockEntry(
                        4L,
                        createRequest(
                                null,
                                "2",
                                StockPackageType.BOTTLE,
                                "750",
                                MeasurementUnit.MILLILITER,
                                null
                        )
                );

        assertDecimal(
                "3000.000",
                kilogramsToGrams.getConvertedQuantity()
        );

        assertDecimal(
                "1.000",
                gramsToKilograms.getConvertedQuantity()
        );

        assertDecimal(
                "4500.000",
                litersToMilliliters.getConvertedQuantity()
        );

        assertDecimal(
                "1.500",
                millilitersToLiters.getConvertedQuantity()
        );

        assertDecimal(
                "3000.000",
                gramsSupply.getCurrentQuantity()
        );

        assertDecimal(
                "1.000",
                kilogramsSupply.getCurrentQuantity()
        );

        assertDecimal(
                "4500.000",
                millilitersSupply.getCurrentQuantity()
        );

        assertDecimal(
                "1.500",
                litersSupply.getCurrentQuantity()
        );
    }

    @Test
    void addStockEntryShouldRejectIncompatibleConversion() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.KILOGRAM,
                "0.000"
        );

        StockEntryRequest request = createRequest(
                null,
                "2",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        request
                )
        );

        assertEquals(
                "Unitatea introdusa nu poate fi convertita "
                        + "in unitatea de baza a variantei.",
                exception.getMessage()
        );

        verify(
                auxiliarySupplyRepository,
                never()
        ).save(any());

        verify(
                stockEntryRepository,
                never()
        ).save(any());
    }

    @Test
    void updateStockEntryShouldUpdateTheSameSupply() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "10.000"
        );

        supply.setReportedAt(
                LocalDateTime.now().minusDays(1)
        );

        StockEntry entry = createExistingEntry(
                supply,
                "4.000"
        );

        StockEntryRequest request = createRequest(
                null,
                "3",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                "   "
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        configureStockEntrySave();

        StockEntry result =
                stockEntryService.updateStockEntry(
                        10L,
                        request
                );

        assertSame(entry, result);
        assertSame(supply, result.getSupply());

        /*
         * 10 existente - 4 ale intrarii vechi + 3 noi = 9.
         */
        assertDecimal(
                "6.000",
                result.getPreviousQuantity()
        );

        assertDecimal(
                "3.000",
                result.getConvertedQuantity()
        );

        assertDecimal(
                "9.000",
                result.getNewQuantity()
        );

        assertDecimal(
                "9.000",
                supply.getCurrentQuantity()
        );

        assertTrue(supply.isAvailableInWarehouse());
        assertNull(supply.getReportedAt());
        assertNull(result.getNotes());

        verify(auxiliarySupplyRepository)
                .save(supply);

        verify(stockEntryRepository)
                .save(entry);
    }

    @Test
    void updateStockEntryShouldMoveEntryToDifferentSupply() {
        AuxiliarySupply oldSupply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "8.000"
        );

        AuxiliarySupply targetSupply = createSupply(
                2L,
                MeasurementUnit.PIECE,
                "2.000"
        );

        targetSupply.setAvailableInWarehouse(false);
        targetSupply.setReportedAt(
                LocalDateTime.now().minusDays(1)
        );

        StockEntry entry = createExistingEntry(
                oldSupply,
                "3.000"
        );

        StockEntryRequest request = createRequest(
                2L,
                "4",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                "Mutata la alta varianta"
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        when(auxiliarySupplyRepository.findById(2L))
                .thenReturn(Optional.of(targetSupply));

        configureStockEntrySave();

        StockEntry result =
                stockEntryService.updateStockEntry(
                        10L,
                        request
                );

        assertSame(targetSupply, result.getSupply());

        assertDecimal(
                "5.000",
                oldSupply.getCurrentQuantity()
        );

        assertDecimal(
                "2.000",
                result.getPreviousQuantity()
        );

        assertDecimal(
                "4.000",
                result.getConvertedQuantity()
        );

        assertDecimal(
                "6.000",
                result.getNewQuantity()
        );

        assertDecimal(
                "6.000",
                targetSupply.getCurrentQuantity()
        );

        assertTrue(oldSupply.isAvailableInWarehouse());
        assertTrue(targetSupply.isAvailableInWarehouse());
        assertNull(targetSupply.getReportedAt());

        assertEquals(
                "Mutata la alta varianta",
                result.getNotes()
        );

        verify(auxiliarySupplyRepository)
                .save(oldSupply);

        verify(auxiliarySupplyRepository)
                .save(targetSupply);

        verify(stockEntryRepository)
                .save(entry);
    }

    @Test
    void updateStockEntryShouldPreventNegativeOldSupplyQuantity() {
        AuxiliarySupply oldSupply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "1.000"
        );

        AuxiliarySupply targetSupply = createSupply(
                2L,
                MeasurementUnit.PIECE,
                null
        );

        StockEntry entry = createExistingEntry(
                oldSupply,
                "3.000"
        );

        StockEntryRequest request = createRequest(
                2L,
                "1",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        when(auxiliarySupplyRepository.findById(2L))
                .thenReturn(Optional.of(targetSupply));

        configureStockEntrySave();

        StockEntry result =
                stockEntryService.updateStockEntry(
                        10L,
                        request
                );

        assertDecimal(
                "0.000",
                oldSupply.getCurrentQuantity()
        );

        assertFalse(oldSupply.isAvailableInWarehouse());
        assertNotNull(oldSupply.getReportedAt());

        assertDecimal(
                "0",
                result.getPreviousQuantity()
        );

        assertDecimal(
                "1.000",
                targetSupply.getCurrentQuantity()
        );

        assertTrue(targetSupply.isAvailableInWarehouse());
    }

    @Test
    void deleteStockEntryShouldRemoveQuantityAndDeleteEntry() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "10.000"
        );

        supply.setReportedAt(
                LocalDateTime.now().minusDays(1)
        );

        StockEntry entry = createExistingEntry(
                supply,
                "4.000"
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        stockEntryService.deleteStockEntry(10L);

        assertDecimal(
                "6.000",
                supply.getCurrentQuantity()
        );

        assertTrue(supply.isAvailableInWarehouse());
        assertNull(supply.getReportedAt());

        verify(auxiliarySupplyRepository)
                .save(supply);

        verify(stockEntryRepository)
                .delete(entry);
    }

    @Test
    void deleteStockEntryShouldPreventNegativeQuantityAndReportSupply() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "1.000"
        );

        supply.setReportedAt(null);

        StockEntry entry = createExistingEntry(
                supply,
                "3.000"
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        stockEntryService.deleteStockEntry(10L);

        assertDecimal(
                "0.000",
                supply.getCurrentQuantity()
        );

        assertFalse(supply.isAvailableInWarehouse());
        assertNotNull(supply.getReportedAt());

        verify(auxiliarySupplyRepository)
                .save(supply);

        verify(stockEntryRepository)
                .delete(entry);
    }

    @Test
    void deleteStockEntryShouldPreserveExistingReportedAt() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        LocalDateTime existingReportedAt =
                LocalDateTime.now().minusDays(2);

        supply.setReportedAt(existingReportedAt);

        StockEntry entry = createExistingEntry(
                supply,
                "1.000"
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        stockEntryService.deleteStockEntry(10L);

        assertDecimal(
                "0.000",
                supply.getCurrentQuantity()
        );

        assertFalse(supply.isAvailableInWarehouse());

        assertEquals(
                existingReportedAt,
                supply.getReportedAt()
        );
    }

    @Test
    void addStockEntryShouldRejectNullRequest() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        null
                )
        );

        assertEquals(
                "Datele intrarii de stoc sunt obligatorii.",
                exception.getMessage()
        );

        verifyNoStockChanges();
    }

    @Test
    void addStockEntryShouldRejectMissingOrInvalidPackageQuantity() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        StockEntryRequest missingQuantity = createRequest(
                null,
                null,
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        StockEntryRequest zeroQuantity = createRequest(
                null,
                "0",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        RuntimeException missingException = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        missingQuantity
                )
        );

        RuntimeException zeroException = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        zeroQuantity
                )
        );

        assertEquals(
                "Cantitatea primita trebuie sa fie mai mare decat zero.",
                missingException.getMessage()
        );

        assertEquals(
                "Cantitatea primita trebuie sa fie mai mare decat zero.",
                zeroException.getMessage()
        );

        verifyNoStockChanges();
    }

    @Test
    void addStockEntryShouldRejectDecimalNumberOfPackages() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        StockEntryRequest request = createRequest(
                null,
                "1.500",
                StockPackageType.BOX,
                "10",
                MeasurementUnit.PIECE,
                null
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        request
                )
        );

        assertEquals(
                "Numarul de ambalaje trebuie sa fie un numar intreg.",
                exception.getMessage()
        );

        verifyNoStockChanges();
    }

    @Test
    void addStockEntryShouldRejectMissingInputUnit() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        StockEntryRequest request = createRequest(
                null,
                "1",
                StockPackageType.DIRECT,
                null,
                null,
                null
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        request
                )
        );

        assertEquals(
                "Unitatea cantitatii primite este obligatorie.",
                exception.getMessage()
        );

        verifyNoStockChanges();
    }

    @Test
    void addStockEntryShouldRejectMissingOrInvalidQuantityPerPackage() {
        AuxiliarySupply supply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "0.000"
        );

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        StockEntryRequest missingQuantityPerPackage =
                createRequest(
                        null,
                        "2",
                        StockPackageType.BOX,
                        null,
                        MeasurementUnit.PIECE,
                        null
                );

        StockEntryRequest zeroQuantityPerPackage =
                createRequest(
                        null,
                        "2",
                        StockPackageType.BOX,
                        "0",
                        MeasurementUnit.PIECE,
                        null
                );

        RuntimeException missingException = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        missingQuantityPerPackage
                )
        );

        RuntimeException zeroException = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.addStockEntry(
                        1L,
                        zeroQuantityPerPackage
                )
        );

        assertEquals(
                "Cantitatea dintr-un ambalaj "
                        + "trebuie sa fie mai mare decat zero.",
                missingException.getMessage()
        );

        assertEquals(
                "Cantitatea dintr-un ambalaj "
                        + "trebuie sa fie mai mare decat zero.",
                zeroException.getMessage()
        );

        verifyNoStockChanges();
    }

    @Test
    void updateStockEntryShouldValidateRequestBeforeReadingEntry() {
        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.updateStockEntry(
                        10L,
                        null
                )
        );

        assertEquals(
                "Datele intrarii de stoc sunt obligatorii.",
                exception.getMessage()
        );

        verify(
                stockEntryRepository,
                never()
        ).findById(any());
    }

    @Test
    void updateStockEntryShouldThrowWhenEntryDoesNotExist() {
        StockEntryRequest request = createRequest(
                null,
                "1",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        when(stockEntryRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.updateStockEntry(
                        99L,
                        request
                )
        );

        assertEquals(
                "Intrarea de stoc nu a fost gasita.",
                exception.getMessage()
        );
    }

    @Test
    void updateStockEntryShouldThrowWhenTargetSupplyDoesNotExist() {
        AuxiliarySupply oldSupply = createSupply(
                1L,
                MeasurementUnit.PIECE,
                "5.000"
        );

        StockEntry entry = createExistingEntry(
                oldSupply,
                "2.000"
        );

        StockEntryRequest request = createRequest(
                99L,
                "1",
                StockPackageType.DIRECT,
                null,
                MeasurementUnit.PIECE,
                null
        );

        when(stockEntryRepository.findById(10L))
                .thenReturn(Optional.of(entry));

        when(auxiliarySupplyRepository.findById(99L))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> stockEntryService.updateStockEntry(
                        10L,
                        request
                )
        );

        assertEquals(
                "Varianta articolului de stoc nu a fost gasita.",
                exception.getMessage()
        );

        verify(
                auxiliarySupplyRepository,
                never()
        ).save(any());

        verify(
                stockEntryRepository,
                never()
        ).save(any());
    }

    private AuxiliarySupply createSupply(
            Long id,
            MeasurementUnit baseUnit,
            String currentQuantity) {

        AuxiliarySupply supply =
                new AuxiliarySupply();

        ReflectionTestUtils.setField(
                supply,
                "id",
                id
        );

        supply.setName("Articol " + id);
        supply.setBaseUnit(baseUnit);

        if (currentQuantity == null) {
            supply.setCurrentQuantity(null);
        } else {
            supply.setCurrentQuantity(
                    new BigDecimal(currentQuantity)
            );
        }

        supply.setAvailableInWarehouse(
                currentQuantity != null
                        && new BigDecimal(currentQuantity)
                        .compareTo(BigDecimal.ZERO) > 0
        );

        return supply;
    }

    private StockEntry createExistingEntry(
            AuxiliarySupply supply,
            String convertedQuantity) {

        StockEntry entry =
                new StockEntry();

        entry.setSupply(supply);

        entry.setPackageQuantity(
                BigDecimal.ONE
        );

        entry.setPackageType(
                StockPackageType.DIRECT
        );

        entry.setQuantityPerPackage(
                BigDecimal.ONE
        );

        entry.setInputUnit(
                supply.getBaseUnit()
        );

        entry.setConvertedQuantity(
                new BigDecimal(convertedQuantity)
        );

        entry.setPreviousQuantity(
                BigDecimal.ZERO
        );

        entry.setNewQuantity(
                supply.getCurrentQuantity()
        );

        entry.setCreatedAt(
                LocalDateTime.now().minusDays(1)
        );

        return entry;
    }

    private StockEntryRequest createRequest(
            Long supplyId,
            String packageQuantity,
            StockPackageType packageType,
            String quantityPerPackage,
            MeasurementUnit inputUnit,
            String notes) {

        StockEntryRequest request =
                new StockEntryRequest();

        request.setSupplyId(supplyId);

        if (packageQuantity != null) {
            request.setPackageQuantity(
                    new BigDecimal(packageQuantity)
            );
        }

        request.setPackageType(packageType);

        if (quantityPerPackage != null) {
            request.setQuantityPerPackage(
                    new BigDecimal(quantityPerPackage)
            );
        }

        request.setInputUnit(inputUnit);
        request.setNotes(notes);

        return request;
    }

    private void configureStockEntrySave() {
        when(stockEntryRepository.save(any(StockEntry.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );
    }

    private void verifyNoStockChanges() {
        verify(
                auxiliarySupplyRepository,
                never()
        ).save(any());

        verify(
                stockEntryRepository,
                never()
        ).save(any());
    }

    private void assertDecimal(
            String expected,
            BigDecimal actual) {

        assertNotNull(actual);

        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual),
                "Valoarea asteptata este "
                        + expected
                        + ", dar valoarea obtinuta este "
                        + actual
        );
    }
}
