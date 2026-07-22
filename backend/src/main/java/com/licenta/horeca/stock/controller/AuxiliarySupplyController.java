package com.licenta.horeca.stock.controller;

import com.licenta.horeca.stock.dto.AuxiliarySupplyRequest;
import com.licenta.horeca.stock.dto.StockEntryRequest;
import com.licenta.horeca.stock.entity.AuxiliarySupply;
import com.licenta.horeca.stock.entity.StockEntry;
import com.licenta.horeca.stock.service.AuxiliarySupplyService;
import com.licenta.horeca.stock.service.StockEntryService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auxiliary-supplies")
@CrossOrigin(origins = "http://localhost:5173")
public class AuxiliarySupplyController {

    private final AuxiliarySupplyService
            auxiliarySupplyService;

    private final StockEntryService
            stockEntryService;

    public AuxiliarySupplyController(
            AuxiliarySupplyService auxiliarySupplyService,
            StockEntryService stockEntryService) {

        this.auxiliarySupplyService =
                auxiliarySupplyService;

        this.stockEntryService =
                stockEntryService;
    }

    @GetMapping
    public List<AuxiliarySupply> getAllSupplies() {
        return auxiliarySupplyService
                .getAllSupplies();
    }

    @GetMapping("/active")
    public List<AuxiliarySupply>
    getAllActiveSupplies() {

        return auxiliarySupplyService
                .getAllActiveSupplies();
    }

    @GetMapping("/unavailable")
    public List<AuxiliarySupply>
    getUnavailableSupplies() {

        return auxiliarySupplyService
                .getUnavailableSupplies();
    }

    @GetMapping("/{supplyId}")
    public AuxiliarySupply getSupplyById(
            @PathVariable Long supplyId) {

        return auxiliarySupplyService
                .getSupplyById(supplyId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AuxiliarySupply createSupply(
            @RequestBody AuxiliarySupplyRequest request) {

        return auxiliarySupplyService
                .createSupply(request);
    }

    @PutMapping("/{supplyId}")
    public AuxiliarySupply updateSupply(
            @PathVariable Long supplyId,
            @RequestBody AuxiliarySupplyRequest request) {

        return auxiliarySupplyService
                .updateSupply(
                        supplyId,
                        request
                );
    }

    @PutMapping("/{supplyId}/mark-unavailable")
    public AuxiliarySupply markUnavailable(
            @PathVariable Long supplyId) {

        return auxiliarySupplyService
                .markUnavailable(supplyId);
    }

    @PutMapping("/{supplyId}/mark-available")
    public AuxiliarySupply markAvailable(
            @PathVariable Long supplyId) {

        return auxiliarySupplyService
                .markAvailable(supplyId);
    }

    @GetMapping("/{supplyId}/entries")
    public List<StockEntry> getStockEntries(
            @PathVariable Long supplyId) {

        return stockEntryService
                .getEntriesForSupply(supplyId);
    }

    @PostMapping("/{supplyId}/entries")
    @ResponseStatus(HttpStatus.CREATED)
    public StockEntry addStockEntry(
            @PathVariable Long supplyId,
            @RequestBody StockEntryRequest request) {

        return stockEntryService
                .addStockEntry(
                        supplyId,
                        request
                );
    }

    @PutMapping("/entries/{entryId}")
    public StockEntry updateStockEntry(
            @PathVariable Long entryId,
            @RequestBody StockEntryRequest request) {

        return stockEntryService
                .updateStockEntry(
                        entryId,
                        request
                );
    }

    @DeleteMapping("/entries/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStockEntry(
            @PathVariable Long entryId) {

        stockEntryService
                .deleteStockEntry(entryId);
    }

    @DeleteMapping("/{supplyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSupply(
            @PathVariable Long supplyId) {

        auxiliarySupplyService
                .deleteSupply(supplyId);
    }
}