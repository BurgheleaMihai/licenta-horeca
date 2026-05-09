package com.licenta.horeca.service;

import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.repository.AuxiliarySupplyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuxiliarySupplyServiceTest {

    @Mock
    private AuxiliarySupplyRepository auxiliarySupplyRepository;

    @InjectMocks
    private AuxiliarySupplyService auxiliarySupplyService;

    @Test
    void getAllSupplies_returnsAllSupplies() {
        AuxiliarySupply cups = new AuxiliarySupply("Pahare carton", "Consumabile");
        AuxiliarySupply boxes = new AuxiliarySupply("Cutii cartofi", "Ambalaje");

        when(auxiliarySupplyRepository.findAll())
                .thenReturn(List.of(cups, boxes));

        List<AuxiliarySupply> result = auxiliarySupplyService.getAllSupplies();

        assertEquals(2, result.size());
        assertEquals("Pahare carton", result.get(0).getName());
        assertEquals("Cutii cartofi", result.get(1).getName());
    }

    @Test
    void getUnavailableSupplies_returnsOnlyUnavailableSupplies() {
        AuxiliarySupply cupHolder = new AuxiliarySupply("Suport pahare", "Consumabile");
        cupHolder.setAvailableInWarehouse(false);

        when(auxiliarySupplyRepository.findByAvailableInWarehouseFalse())
                .thenReturn(List.of(cupHolder));

        List<AuxiliarySupply> result = auxiliarySupplyService.getUnavailableSupplies();

        assertEquals(1, result.size());
        assertEquals("Suport pahare", result.get(0).getName());
        assertFalse(result.get(0).isAvailableInWarehouse());
    }

    @Test
    void markUnavailable_setsSupplyAsUnavailableAndReportedAt() {
        AuxiliarySupply supply = new AuxiliarySupply("Suport pahare", "Consumabile");

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply))
                .thenReturn(supply);

        AuxiliarySupply result = auxiliarySupplyService.markUnavailable(1L);

        assertFalse(result.isAvailableInWarehouse());
        assertNotNull(result.getReportedAt());

        verify(auxiliarySupplyRepository).save(supply);
    }

    @Test
    void markAvailable_setsSupplyAsAvailableAndClearsReportedAt() {
        AuxiliarySupply supply = new AuxiliarySupply("Suport pahare", "Consumabile");
        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(java.time.LocalDateTime.now());

        when(auxiliarySupplyRepository.findById(1L))
                .thenReturn(Optional.of(supply));

        when(auxiliarySupplyRepository.save(supply))
                .thenReturn(supply);

        AuxiliarySupply result = auxiliarySupplyService.markAvailable(1L);

        assertTrue(result.isAvailableInWarehouse());
        assertNull(result.getReportedAt());

        verify(auxiliarySupplyRepository).save(supply);
    }
}
