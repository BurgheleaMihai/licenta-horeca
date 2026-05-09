package com.licenta.horeca.repository;

import com.licenta.horeca.entity.AuxiliarySupply;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuxiliarySupplyRepository extends JpaRepository<AuxiliarySupply, Long> {

    List<AuxiliarySupply> findByAvailableInWarehouseFalse();
}
