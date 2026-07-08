package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.repository.AuxiliarySupplyRepository;
import com.licenta.horeca.service.AuxiliarySupplyService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuxiliarySupplyServiceTest {
    private static final String CONSUMABLE_CATEGORY = "Consumabile";
    private static final String CUP_HOLDER_NAME = "Suport pahare";

    @Mock private AuxiliarySupplyRepository auxiliarySupplyRepository;

    @InjectMocks private AuxiliarySupplyService auxiliarySupplyService;

    @Test
    void getAllSuppliesReturnsAllSupplies() {
        AuxiliarySupply cups =
                new AuxiliarySupply("Pahare carton", CONSUMABLE_CATEGORY);
        AuxiliarySupply boxes = new AuxiliarySupply("Cutii cartofi", "Ambalaje");

        when(auxiliarySupplyRepository.findAll()).thenReturn(List.of(cups, boxes));

        List<AuxiliarySupply> result = auxiliarySupplyService.getAllSupplies();

        assertEquals(2, result.size());
        assertEquals("Pahare carton", result.get(0).getName());
        assertEquals("Cutii cartofi", result.get(1).getName());
    }

    @Test
    void getUnavailableSuppliesReturnsOnlyUnavailableSupplies() {
        AuxiliarySupply cupHolder =
                new AuxiliarySupply(CUP_HOLDER_NAME, CONSUMABLE_CATEGORY);
        cupHolder.setAvailableInWarehouse(false);

        when(auxiliarySupplyRepository.findByAvailableInWarehouseFalse())
                .thenReturn(List.of(cupHolder));

        List<AuxiliarySupply> result =
                auxiliarySupplyService.getUnavailableSupplies();

        assertEquals(1, result.size());
        assertEquals(CUP_HOLDER_NAME, result.get(0).getName());
        assertFalse(result.get(0).isAvailableInWarehouse());
    }

    @Test
    void markUnavailableSetsSupplyAsUnavailableAndReportedAt() {
        AuxiliarySupply supply =
                new AuxiliarySupply(CUP_HOLDER_NAME, CONSUMABLE_CATEGORY);

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply)).thenReturn(supply);

        AuxiliarySupply result = auxiliarySupplyService.markUnavailable(1L);

        assertFalse(result.isAvailableInWarehouse());
        assertNotNull(result.getReportedAt());

        verify(auxiliarySupplyRepository).save(supply);
    }

    @Test
    void markAvailableSetsSupplyAsAvailableAndClearsReportedAt() {
        AuxiliarySupply supply =
                new AuxiliarySupply(CUP_HOLDER_NAME, CONSUMABLE_CATEGORY);
        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(java.time.LocalDateTime.now());

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply)).thenReturn(supply);

        AuxiliarySupply result = auxiliarySupplyService.markAvailable(1L);

        assertTrue(result.isAvailableInWarehouse());
        assertNull(result.getReportedAt());

        verify(auxiliarySupplyRepository).save(supply);
    }
}