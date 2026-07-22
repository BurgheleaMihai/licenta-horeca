package com.licenta.horeca.repository;

import com.licenta.horeca.entity.AuxiliarySupply;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AuxiliarySupplyRepository
        extends JpaRepository<AuxiliarySupply, Long> {

    List<AuxiliarySupply>
    findByAvailableInWarehouseFalseAndActiveTrueOrderByNameAscVariantNameAsc();

    @Query("""
            SELECT supply
            FROM AuxiliarySupply supply
            ORDER BY
                CASE supply.stockType
                    WHEN com.licenta.horeca.enums.StockType.AUXILIARY THEN 1
                    WHEN com.licenta.horeca.enums.StockType.WAREHOUSE THEN 2
                    WHEN com.licenta.horeca.enums.StockType.FRUIT_AND_VEGETABLE THEN 3
                    ELSE 4
                END,
                LOWER(supply.name),
                LOWER(COALESCE(supply.variantName, ''))
            """)
    List<AuxiliarySupply> findAllOrderedByStockTypeNameAndVariant();

    @Query("""
            SELECT supply
            FROM AuxiliarySupply supply
            WHERE supply.active = true
            ORDER BY
                CASE supply.stockType
                    WHEN com.licenta.horeca.enums.StockType.AUXILIARY THEN 1
                    WHEN com.licenta.horeca.enums.StockType.WAREHOUSE THEN 2
                    WHEN com.licenta.horeca.enums.StockType.FRUIT_AND_VEGETABLE THEN 3
                    ELSE 4
                END,
                LOWER(supply.name),
                LOWER(COALESCE(supply.variantName, ''))
            """)
    List<AuxiliarySupply> findAllActiveOrderedByStockTypeNameAndVariant();

    boolean existsByNameIgnoreCaseAndVariantNameIgnoreCase(
            String name,
            String variantName
    );

    boolean existsByNameIgnoreCaseAndVariantNameIsNull(
            String name
    );
}