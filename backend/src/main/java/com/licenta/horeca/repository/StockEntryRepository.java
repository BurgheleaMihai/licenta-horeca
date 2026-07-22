package com.licenta.horeca.repository;

import com.licenta.horeca.entity.StockEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockEntryRepository
        extends JpaRepository<StockEntry, Long> {

    List<StockEntry> findBySupplyIdOrderByCreatedAtDesc(
            Long supplyId
    );

    void deleteBySupplyId(Long supplyId);
}