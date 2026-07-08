package com.licenta.horeca;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.licenta.horeca.controller.AuxiliarySupplyController;
import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.service.AuxiliarySupplyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuxiliarySupplyController.class)
class AuxiliarySupplyControllerTest {
    private static final String SUPPLIES_ENDPOINT = "/api/auxiliary-supplies";
    private static final String UNAVAILABLE_SUPPLIES_ENDPOINT =
            "/api/auxiliary-supplies/unavailable";
    private static final String MARK_UNAVAILABLE_ENDPOINT =
            "/api/auxiliary-supplies/1/mark-unavailable";
    private static final String MARK_AVAILABLE_ENDPOINT =
            "/api/auxiliary-supplies/1/mark-available";

    private static final String CUPS_NAME = "Pahare carton";
    private static final String BOXES_NAME = "Cutii cartofi";
    private static final String CONSUMABLE_CATEGORY = "Consumabile";
    private static final String PACKAGING_CATEGORY = "Ambalaje";

    @Autowired private MockMvc mockMvc;

    @MockitoBean private AuxiliarySupplyService auxiliarySupplyService;

    @Test
    void getAllSuppliesShouldReturnAllSupplies() throws Exception {
        AuxiliarySupply cups = new AuxiliarySupply(CUPS_NAME, CONSUMABLE_CATEGORY);
        AuxiliarySupply boxes = new AuxiliarySupply(BOXES_NAME, PACKAGING_CATEGORY);

        when(auxiliarySupplyService.getAllSupplies())
                .thenReturn(List.of(cups, boxes));

        mockMvc.perform(get(SUPPLIES_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value(CUPS_NAME))
                .andExpect(jsonPath("$[0].category").value(CONSUMABLE_CATEGORY))
                .andExpect(jsonPath("$[1].name").value(BOXES_NAME))
                .andExpect(jsonPath("$[1].category").value(PACKAGING_CATEGORY));
    }

    @Test
    void getUnavailableSuppliesShouldReturnUnavailableSupplies()
            throws Exception {
        AuxiliarySupply cups = new AuxiliarySupply(CUPS_NAME, CONSUMABLE_CATEGORY);
        cups.setAvailableInWarehouse(false);

        when(auxiliarySupplyService.getUnavailableSupplies())
                .thenReturn(List.of(cups));

        mockMvc.perform(get(UNAVAILABLE_SUPPLIES_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value(CUPS_NAME))
                .andExpect(jsonPath("$[0].availableInWarehouse").value(false));
    }

    @Test
    void markUnavailableShouldReturnUnavailableSupply() throws Exception {
        AuxiliarySupply cups = new AuxiliarySupply(CUPS_NAME, CONSUMABLE_CATEGORY);
        cups.setAvailableInWarehouse(false);

        when(auxiliarySupplyService.markUnavailable(1L)).thenReturn(cups);

        mockMvc.perform(put(MARK_UNAVAILABLE_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(CUPS_NAME))
                .andExpect(jsonPath("$.availableInWarehouse").value(false));
    }

    @Test
    void markAvailableShouldReturnAvailableSupply() throws Exception {
        AuxiliarySupply cups = new AuxiliarySupply(CUPS_NAME, CONSUMABLE_CATEGORY);
        cups.setAvailableInWarehouse(true);

        when(auxiliarySupplyService.markAvailable(1L)).thenReturn(cups);

        mockMvc.perform(put(MARK_AVAILABLE_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(CUPS_NAME))
                .andExpect(jsonPath("$.availableInWarehouse").value(true));
    }
}