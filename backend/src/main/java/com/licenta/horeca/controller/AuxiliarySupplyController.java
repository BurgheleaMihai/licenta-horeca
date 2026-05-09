package com.licenta.horeca.controller;

import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.service.AuxiliarySupplyService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/auxiliary-supplies")
@CrossOrigin(origins = "http://localhost:5173")
public class AuxiliarySupplyController {

    private final AuxiliarySupplyService auxiliarySupplyService;

    public AuxiliarySupplyController(AuxiliarySupplyService auxiliarySupplyService) {
        this.auxiliarySupplyService = auxiliarySupplyService;
    }

    @GetMapping
    public List<AuxiliarySupply> getAllSupplies() {
        return auxiliarySupplyService.getAllSupplies();
    }

    @GetMapping("/unavailable")
    public List<AuxiliarySupply> getUnavailableSupplies() {
        return auxiliarySupplyService.getUnavailableSupplies();
    }

    @PutMapping("/{supplyId}/mark-unavailable")
    public AuxiliarySupply markUnavailable(@PathVariable Long supplyId) {
        return auxiliarySupplyService.markUnavailable(supplyId);
    }

    @PutMapping("/{supplyId}/mark-available")
    public AuxiliarySupply markAvailable(@PathVariable Long supplyId) {
        return auxiliarySupplyService.markAvailable(supplyId);
    }
}
