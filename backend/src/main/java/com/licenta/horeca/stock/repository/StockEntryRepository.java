package com.licenta.horeca.stock.repository;

import com.licenta.horeca.stock.entity.StockEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockEntryRepository
        extends JpaRepository<StockEntry, Long> {

    List<StockEntry> findBySupplyIdOrderByCreatedAtDesc(
            Long supplyId
    );

    void deleteBySupplyId(Long supplyId);
}