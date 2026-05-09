package com.licenta.horeca.service;

import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.repository.AuxiliarySupplyRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AuxiliarySupplyService {

    private final AuxiliarySupplyRepository auxiliarySupplyRepository;

    public AuxiliarySupplyService(AuxiliarySupplyRepository auxiliarySupplyRepository) {
        this.auxiliarySupplyRepository = auxiliarySupplyRepository;
    }

    public List<AuxiliarySupply> getAllSupplies() {
        return auxiliarySupplyRepository.findAll();
    }

    public List<AuxiliarySupply> getUnavailableSupplies() {
        return auxiliarySupplyRepository.findByAvailableInWarehouseFalse();
    }

    public AuxiliarySupply markUnavailable(Long supplyId) {
        AuxiliarySupply supply = auxiliarySupplyRepository.findById(supplyId)
                .orElseThrow(() -> new RuntimeException("Produsul auxiliar nu a fost gasit."));

        supply.setAvailableInWarehouse(false);
        supply.setReportedAt(LocalDateTime.now());

        return auxiliarySupplyRepository.save(supply);
    }

    public AuxiliarySupply markAvailable(Long supplyId) {
        AuxiliarySupply supply = auxiliarySupplyRepository.findById(supplyId)
                .orElseThrow(() -> new RuntimeException("Produsul auxiliar nu a fost gasit."));

        supply.setAvailableInWarehouse(true);
        supply.setReportedAt(null);

        return auxiliarySupplyRepository.save(supply);
    }
}