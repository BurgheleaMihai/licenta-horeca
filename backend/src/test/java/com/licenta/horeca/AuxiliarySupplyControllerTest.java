package com.licenta.horeca;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.controller.AuxiliarySupplyController;
import com.licenta.horeca.dto.AuxiliarySupplyRequest;
import com.licenta.horeca.dto.StockEntryRequest;
import com.licenta.horeca.entity.AuxiliarySupply;
import com.licenta.horeca.entity.StockEntry;
import com.licenta.horeca.enums.MeasurementUnit;
import com.licenta.horeca.enums.StockCategory;
import com.licenta.horeca.enums.StockPackageType;
import com.licenta.horeca.enums.StockType;
import com.licenta.horeca.service.AuxiliarySupplyService;
import com.licenta.horeca.service.StockEntryService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import com.licenta.horeca.auth.security.SecurityConfig;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;

@WebMvcTest(AuxiliarySupplyController.class)
@Import(SecurityConfig.class)
@WithMockUser(
        username = "manager@test.com",
        roles = "MANAGER"
)
class AuxiliarySupplyControllerTest {

    private static final String API_URL =
            "/api/auxiliary-supplies";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AuxiliarySupplyService
            auxiliarySupplyService;

    @MockitoBean
    private StockEntryService
            stockEntryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void getAllSuppliesShouldReturnAllVariants()
            throws Exception {

        AuxiliarySupply cups200 =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("50")
                );

        AuxiliarySupply cups400 =
                createSupply(
                        2L,
                        "Pahare carton",
                        "400 ml",
                        BigDecimal.ZERO
                );

        when(auxiliarySupplyService.getAllSupplies())
                .thenReturn(
                        List.of(cups200, cups400)
                );

        mockMvc.perform(
                        get(API_URL)
                                .accept(
                                        MediaType.APPLICATION_JSON
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().contentTypeCompatibleWith(
                                MediaType.APPLICATION_JSON
                        )
                )
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].name")
                                .value("Pahare carton")
                )
                .andExpect(
                        jsonPath("$[0].variantName")
                                .value("200 ml")
                )
                .andExpect(
                        jsonPath("$[0].currentQuantity")
                                .value(50)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[1].variantName")
                                .value("400 ml")
                )
                .andExpect(
                        jsonPath("$[1].currentQuantity")
                                .value(0)
                );

        verify(auxiliarySupplyService)
                .getAllSupplies();
    }

    @Test
    void getAllSuppliesShouldReturnEmptyArray()
            throws Exception {

        when(auxiliarySupplyService.getAllSupplies())
                .thenReturn(List.of());

        mockMvc.perform(
                        get(API_URL)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(0)
                );

        verify(auxiliarySupplyService)
                .getAllSupplies();
    }

    @Test
    void getAllActiveSuppliesShouldReturnOnlyActiveSupplies()
            throws Exception {

        AuxiliarySupply activeSupply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("50")
                );

        activeSupply.setActive(true);

        when(
                auxiliarySupplyService
                        .getAllActiveSupplies()
        ).thenReturn(List.of(activeSupply));

        mockMvc.perform(
                        get(API_URL + "/active")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].active")
                                .value(true)
                );

        verify(auxiliarySupplyService)
                .getAllActiveSupplies();
    }

    @Test
    void getUnavailableSuppliesShouldReturnUnavailableSupplies()
            throws Exception {

        AuxiliarySupply unavailableSupply =
                createSupply(
                        2L,
                        "Capace",
                        "Large",
                        BigDecimal.ZERO
                );

        unavailableSupply.setAvailableInWarehouse(
                false
        );

        unavailableSupply.setReportedAt(
                LocalDateTime.now()
                        .minusMinutes(20)
        );

        when(
                auxiliarySupplyService
                        .getUnavailableSupplies()
        ).thenReturn(
                List.of(unavailableSupply)
        );

        mockMvc.perform(
                        get(API_URL + "/unavailable")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].availableInWarehouse")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$[0].reportedAt")
                                .exists()
                );

        verify(auxiliarySupplyService)
                .getUnavailableSupplies();
    }

    @Test
    void getSupplyByIdShouldReturnRequestedSupply()
            throws Exception {

        AuxiliarySupply supply =
                createSupply(
                        5L,
                        "Sirop",
                        "Vanilie",
                        new BigDecimal("3.500")
                );

        supply.setBaseUnit(
                MeasurementUnit.LITER
        );

        when(
                auxiliarySupplyService
                        .getSupplyById(5L)
        ).thenReturn(supply);

        mockMvc.perform(
                        get(API_URL + "/{supplyId}", 5L)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(5)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Sirop")
                )
                .andExpect(
                        jsonPath("$.variantName")
                                .value("Vanilie")
                )
                .andExpect(
                        jsonPath("$.baseUnit")
                                .value("LITER")
                )
                .andExpect(
                        jsonPath("$.currentQuantity")
                                .value(3.5)
                );

        verify(auxiliarySupplyService)
                .getSupplyById(5L);
    }

    @Test
    void getSupplyByIdShouldRejectInvalidId()
            throws Exception {

        mockMvc.perform(
                        get(API_URL + "/invalid")
                )
                .andExpect(status().isBadRequest());

        verify(
                auxiliarySupplyService,
                never()
        ).getSupplyById(anyLong());
    }

    @Test
    void createSupplyShouldReturnCreatedVariant()
            throws Exception {

        AuxiliarySupplyRequest request =
                createSupplyRequest();

        AuxiliarySupply savedSupply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.ZERO
                );

        savedSupply.setSpecificationValue(
                new BigDecimal("200")
        );

        savedSupply.setSpecificationUnit(
                MeasurementUnit.MILLILITER
        );

        when(
                auxiliarySupplyService.createSupply(
                        any(AuxiliarySupplyRequest.class)
                )
        ).thenReturn(savedSupply);

        mockMvc.perform(
                        post(API_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Pahare carton")
                )
                .andExpect(
                        jsonPath("$.variantName")
                                .value("200 ml")
                )
                .andExpect(
                        jsonPath("$.specificationValue")
                                .value(200)
                )
                .andExpect(
                        jsonPath("$.specificationUnit")
                                .value("MILLILITER")
                );

        ArgumentCaptor<AuxiliarySupplyRequest>
                requestCaptor =
                ArgumentCaptor.forClass(
                        AuxiliarySupplyRequest.class
                );

        verify(auxiliarySupplyService)
                .createSupply(
                        requestCaptor.capture()
                );

        AuxiliarySupplyRequest capturedRequest =
                requestCaptor.getValue();

        org.junit.jupiter.api.Assertions.assertEquals(
                "Pahare carton",
                capturedRequest.getName()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                MeasurementUnit.PIECE,
                capturedRequest.getBaseUnit()
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                StockType.AUXILIARY,
                capturedRequest.getStockType()
        );
    }

    @Test
    void createSupplyShouldRejectMalformedJson()
            throws Exception {

        String malformedJson = """
                {
                  "name": "Pahare carton",
                  "currentQuantity":
                }
                """;

        mockMvc.perform(
                        post(API_URL)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(malformedJson)
                )
                .andExpect(status().isBadRequest());

        verify(
                auxiliarySupplyService,
                never()
        ).createSupply(
                any(AuxiliarySupplyRequest.class)
        );
    }

    @Test
    void updateSupplyShouldReturnUpdatedSupply()
            throws Exception {

        AuxiliarySupplyRequest request =
                createSupplyRequest();

        request.setMinimumQuantity(
                new BigDecimal("20")
        );

        AuxiliarySupply updatedSupply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("80")
                );

        updatedSupply.setMinimumQuantity(
                new BigDecimal("20")
        );

        when(
                auxiliarySupplyService.updateSupply(
                        eq(1L),
                        any(AuxiliarySupplyRequest.class)
                )
        ).thenReturn(updatedSupply);

        mockMvc.perform(
                        put(API_URL + "/{supplyId}", 1L)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.name")
                                .value("Pahare carton")
                )
                .andExpect(
                        jsonPath("$.currentQuantity")
                                .value(80)
                )
                .andExpect(
                        jsonPath("$.minimumQuantity")
                                .value(20)
                );

        verify(auxiliarySupplyService)
                .updateSupply(
                        eq(1L),
                        any(AuxiliarySupplyRequest.class)
                );
    }

    @Test
    void updateSupplyShouldRejectInvalidId()
            throws Exception {

        AuxiliarySupplyRequest request =
                createSupplyRequest();

        mockMvc.perform(
                        put(API_URL + "/invalid")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verify(
                auxiliarySupplyService,
                never()
        ).updateSupply(
                anyLong(),
                any(AuxiliarySupplyRequest.class)
        );
    }

    @Test
    void markUnavailableShouldReturnUpdatedSupply()
            throws Exception {

        AuxiliarySupply supply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        BigDecimal.ZERO
                );

        supply.setAvailableInWarehouse(false);

        supply.setReportedAt(
                LocalDateTime.now()
        );

        when(
                auxiliarySupplyService
                        .markUnavailable(1L)
        ).thenReturn(supply);

        mockMvc.perform(
                        put(
                                API_URL
                                        + "/1/mark-unavailable"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.availableInWarehouse")
                                .value(false)
                )
                .andExpect(
                        jsonPath("$.reportedAt")
                                .exists()
                );

        verify(auxiliarySupplyService)
                .markUnavailable(1L);
    }

    @Test
    void markAvailableShouldReturnUpdatedSupply()
            throws Exception {

        AuxiliarySupply supply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("50")
                );

        supply.setAvailableInWarehouse(true);
        supply.setReportedAt(null);

        when(
                auxiliarySupplyService
                        .markAvailable(1L)
        ).thenReturn(supply);

        mockMvc.perform(
                        put(
                                API_URL
                                        + "/1/mark-available"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.availableInWarehouse")
                                .value(true)
                )
                .andExpect(
                        jsonPath("$.reportedAt")
                                .doesNotExist()
                );

        verify(auxiliarySupplyService)
                .markAvailable(1L);
    }

    @Test
    void getStockEntriesShouldReturnEntriesForSupply()
            throws Exception {

        AuxiliarySupply supply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("50")
                );

        StockEntry firstEntry =
                createStockEntry(
                        10L,
                        supply,
                        new BigDecimal("50")
                );

        StockEntry secondEntry =
                createStockEntry(
                        11L,
                        supply,
                        new BigDecimal("25")
                );

        when(
                stockEntryService
                        .getEntriesForSupply(1L)
        ).thenReturn(
                List.of(
                        firstEntry,
                        secondEntry
                )
        );

        mockMvc.perform(
                        get(
                                API_URL
                                        + "/{supplyId}/entries",
                                1L
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$[0].id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$[0].convertedQuantity")
                                .value(50)
                )
                .andExpect(
                        jsonPath("$[1].id")
                                .value(11)
                )
                .andExpect(
                        jsonPath("$[1].convertedQuantity")
                                .value(25)
                );

        verify(stockEntryService)
                .getEntriesForSupply(1L);
    }

    @Test
    void addStockEntryShouldReturnCreatedEntry()
            throws Exception {

        StockEntryRequest request =
                createEntryRequest();

        AuxiliarySupply supply =
                createSupply(
                        1L,
                        "Pahare carton",
                        "200 ml",
                        new BigDecimal("50")
                );

        StockEntry entry =
                createStockEntry(
                        10L,
                        supply,
                        new BigDecimal("50")
                );

        when(
                stockEntryService.addStockEntry(
                        eq(1L),
                        any(StockEntryRequest.class)
                )
        ).thenReturn(entry);

        mockMvc.perform(
                        post(API_URL + "/1/entries")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isCreated())
                .andExpect(
                        jsonPath("$.id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.convertedQuantity")
                                .value(50)
                )
                .andExpect(
                        jsonPath("$.supply.variantName")
                                .value("200 ml")
                );

        verify(stockEntryService)
                .addStockEntry(
                        eq(1L),
                        any(StockEntryRequest.class)
                );
    }

    @Test
    void addStockEntryShouldRejectMalformedJson()
            throws Exception {

        String malformedJson = """
                {
                  "packageQuantity":
                }
                """;

        mockMvc.perform(
                        post(API_URL + "/1/entries")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(malformedJson)
                )
                .andExpect(status().isBadRequest());

        verify(
                stockEntryService,
                never()
        ).addStockEntry(
                anyLong(),
                any(StockEntryRequest.class)
        );
    }

    @Test
    void updateStockEntryShouldReturnUpdatedEntry()
            throws Exception {

        StockEntryRequest request =
                createEntryRequest();

        request.setSupplyId(2L);

        AuxiliarySupply supply400 =
                createSupply(
                        2L,
                        "Pahare carton",
                        "400 ml",
                        new BigDecimal("50")
                );

        StockEntry updatedEntry =
                createStockEntry(
                        10L,
                        supply400,
                        new BigDecimal("50")
                );

        when(
                stockEntryService.updateStockEntry(
                        eq(10L),
                        any(StockEntryRequest.class)
                )
        ).thenReturn(updatedEntry);

        mockMvc.perform(
                        put(API_URL + "/entries/10")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.supply.id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.supply.variantName")
                                .value("400 ml")
                );

        verify(stockEntryService)
                .updateStockEntry(
                        eq(10L),
                        any(StockEntryRequest.class)
                );
    }

    @Test
    void updateStockEntryShouldRejectInvalidEntryId()
            throws Exception {

        StockEntryRequest request =
                createEntryRequest();

        mockMvc.perform(
                        put(
                                API_URL
                                        + "/entries/invalid"
                        )
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        objectMapper.writeValueAsString(
                                                request
                                        )
                                )
                )
                .andExpect(status().isBadRequest());

        verify(
                stockEntryService,
                never()
        ).updateStockEntry(
                anyLong(),
                any(StockEntryRequest.class)
        );
    }

    @Test
    void deleteStockEntryShouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(stockEntryService)
                .deleteStockEntry(10L);

        mockMvc.perform(
                        delete(
                                API_URL
                                        + "/entries/10"
                        )
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(stockEntryService)
                .deleteStockEntry(10L);
    }

    @Test
    void deleteStockEntryShouldRejectInvalidEntryId()
            throws Exception {

        mockMvc.perform(
                        delete(
                                API_URL
                                        + "/entries/invalid"
                        )
                )
                .andExpect(status().isBadRequest());

        verify(
                stockEntryService,
                never()
        ).deleteStockEntry(anyLong());
    }

    @Test
    void deleteSupplyShouldReturnNoContent()
            throws Exception {

        doNothing()
                .when(auxiliarySupplyService)
                .deleteSupply(1L);

        mockMvc.perform(
                        delete(API_URL + "/1")
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        verify(auxiliarySupplyService)
                .deleteSupply(1L);
    }

    @Test
    void deleteSupplyShouldRejectInvalidSupplyId()
            throws Exception {

        mockMvc.perform(
                        delete(API_URL + "/invalid")
                )
                .andExpect(status().isBadRequest());

        verify(
                auxiliarySupplyService,
                never()
        ).deleteSupply(anyLong());
    }

    private AuxiliarySupply createSupply(
            Long id,
            String name,
            String variantName,
            BigDecimal quantity) {

        AuxiliarySupply supply =
                new AuxiliarySupply();

        ReflectionTestUtils.setField(
                supply,
                "id",
                id
        );

        supply.setName(name);
        supply.setVariantName(variantName);

        supply.setStockType(
                StockType.AUXILIARY
        );

        supply.setCategory(
                StockCategory.PACKAGING
        );

        supply.setBaseUnit(
                MeasurementUnit.PIECE
        );

        supply.setCurrentQuantity(quantity);

        supply.setMinimumQuantity(
                BigDecimal.TEN
        );

        supply.setAvailableInWarehouse(
                quantity.compareTo(BigDecimal.ZERO) > 0
        );

        supply.setActive(true);

        return supply;
    }

    private AuxiliarySupplyRequest
    createSupplyRequest() {

        AuxiliarySupplyRequest request =
                new AuxiliarySupplyRequest();

        request.setName("Pahare carton");

        request.setSpecificationValue(
                new BigDecimal("200")
        );

        request.setSpecificationUnit(
                MeasurementUnit.MILLILITER
        );

        request.setStockType(
                StockType.AUXILIARY
        );

        request.setCategory(
                StockCategory.PACKAGING
        );

        request.setBaseUnit(
                MeasurementUnit.PIECE
        );

        request.setCurrentQuantity(
                BigDecimal.ZERO
        );

        request.setMinimumQuantity(
                BigDecimal.TEN
        );

        request.setActive(true);

        return request;
    }

    private StockEntryRequest createEntryRequest() {
        StockEntryRequest request =
                new StockEntryRequest();

        request.setSupplyId(1L);

        request.setPackageQuantity(
                new BigDecimal("2")
        );

        request.setPackageType(
                StockPackageType.PACK
        );

        request.setQuantityPerPackage(
                new BigDecimal("25")
        );

        request.setInputUnit(
                MeasurementUnit.PIECE
        );

        request.setNotes("Livrare test");

        return request;
    }

    private StockEntry createStockEntry(
            Long id,
            AuxiliarySupply supply,
            BigDecimal convertedQuantity) {

        StockEntry entry =
                new StockEntry();

        ReflectionTestUtils.setField(
                entry,
                "id",
                id
        );

        entry.setSupply(supply);

        entry.setPackageQuantity(
                new BigDecimal("2")
        );

        entry.setPackageType(
                StockPackageType.PACK
        );

        entry.setQuantityPerPackage(
                new BigDecimal("25")
        );

        entry.setInputUnit(
                MeasurementUnit.PIECE
        );

        entry.setConvertedQuantity(
                convertedQuantity
        );

        entry.setPreviousQuantity(
                BigDecimal.ZERO
        );

        entry.setNewQuantity(
                convertedQuantity
        );

        entry.setCreatedAt(
                LocalDateTime.now()
        );

        entry.setNotes("Livrare test");

        return entry;
    }
}
