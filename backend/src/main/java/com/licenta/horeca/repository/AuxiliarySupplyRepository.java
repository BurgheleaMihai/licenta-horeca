package com.licenta.horeca.repository;

import com.licenta.horeca.entity.AuxiliarySupply;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuxiliarySupplyRepository
        extends JpaRepository<AuxiliarySupply, Long> {
    List<AuxiliarySupply> findByAvailableInWarehouseFalse();
}
