package com.licenta.horeca.decision.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.licenta.horeca.decision.dto.DecisionRequest;
import com.licenta.horeca.decision.dto.DecisionResponse;
import com.licenta.horeca.employee.dto.shift.ActiveStaffSummaryResponse;
import com.licenta.horeca.product.entity.Category;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.product.entity.Product;
import com.licenta.horeca.table.entity.TableSession;
import com.licenta.horeca.order.enums.OrderStatus;
import com.licenta.horeca.decision.repository.DecisionTrainingRecordRepository;
import com.licenta.horeca.table.repository.TableSessionRepository;
import com.licenta.horeca.employee.service.EmployeeShiftService;
import com.licenta.horeca.order.service.OrderService;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@ExtendWith(MockitoExtension.class)
class DecisionServiceTest {

    private static final String AI_SERVICE_URL =
            "http://127.0.0.1:5000/predict/all";

    private static final String AI_RETRAIN_URL =
            "http://127.0.0.1:5000/retrain";

    private static final String RETRAIN_TOKEN =
            "test-retrain-token";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private RestTemplate retrainingRestTemplate;

    @Mock
    private OrderService orderService;

    @Mock
    private EmployeeShiftService employeeShiftService;

    @Mock
    private TableSessionRepository tableSessionRepository;

    @Mock
    private DecisionTrainingRecordRepository
            decisionTrainingRecordRepository;

    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        decisionService = new DecisionService(
                orderService,
                employeeShiftService,
                tableSessionRepository,
                decisionTrainingRecordRepository,
                RETRAIN_TOKEN
        );

        /*
         * Unele teste verifică doar reantrenarea și nu apelează
         * getActiveStaffSummary(), de aceea stubbing-ul este lenient.
         */
        lenient()
                .when(employeeShiftService.getActiveStaffSummary())
                .thenReturn(
                        new ActiveStaffSummaryResponse(
                                2,
                                3,
                                1
                        )
                );

        /*
         * DecisionService creeaza intern obiectele RestTemplate.
         * In teste le inlocuim cu mock-uri.
         */
        ReflectionTestUtils.setField(
                decisionService,
                "restTemplate",
                restTemplate
        );

        ReflectionTestUtils.setField(
                decisionService,
                "retrainingRestTemplate",
                retrainingRestTemplate
        );
    }

    @Test
    void getDecisionSummaryShouldReturnAiResponseAndSaveTrainingRecord() {
        Order firstOrder = new Order();
        firstOrder.setCreatedAt(
                LocalDateTime.now().minusMinutes(50)
        );

        Order secondOrder = new Order();
        secondOrder.setCreatedAt(null);

        Product kitchenProduct =
                createProductWithCategory("Pizza");

        Product barProduct =
                createProductWithCategory("Bauturi");

        Product productWithoutCategory =
                mock(Product.class);

        when(productWithoutCategory.getCategory())
                .thenReturn(null);

        OrderItem unfinishedKitchenItem =
                createOrderItem(
                        kitchenProduct,
                        OrderStatus.IN_PREPARARE
                );

        OrderItem finishedKitchenItem =
                createOrderItem(
                        kitchenProduct,
                        OrderStatus.GATA
                );

        OrderItem unfinishedBarItem =
                createOrderItem(
                        barProduct,
                        OrderStatus.NOUA
                );

        OrderItem finishedBarItem =
                createOrderItem(
                        barProduct,
                        OrderStatus.GATA
                );

        OrderItem itemWithoutProduct =
                new OrderItem();

        itemWithoutProduct.setStatus(
                OrderStatus.IN_PREPARARE
        );

        itemWithoutProduct.setProduct(null);

        OrderItem itemWithoutCategory =
                createOrderItem(
                        productWithoutCategory,
                        OrderStatus.IN_PREPARARE
                );

        firstOrder.addItem(unfinishedKitchenItem);
        firstOrder.addItem(finishedKitchenItem);
        firstOrder.addItem(unfinishedBarItem);
        firstOrder.addItem(finishedBarItem);
        firstOrder.addItem(itemWithoutProduct);
        firstOrder.addItem(itemWithoutCategory);

        when(orderService.getActiveOrders())
                .thenReturn(
                        List.of(firstOrder, secondOrder)
                );

        when(
                orderService.countOrdersCreatedAfter(
                        any(LocalDateTime.class)
                )
        ).thenReturn(3);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(
                        List.of(
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class)
                        )
                );

        DecisionResponse aiResponse =
                createDecisionResponse(
                        "RIDICAT",
                        3,
                        2,
                        1,
                        "MEDIU"
                );

        ArgumentCaptor<DecisionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        DecisionRequest.class
                );

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        requestCaptor.capture(),
                        eq(DecisionResponse.class)
                )
        ).thenReturn(aiResponse);

        LocalDateTime beforeCall =
                LocalDateTime.now();

        DecisionResponse result =
                decisionService.getDecisionSummary();

        LocalDateTime afterCall =
                LocalDateTime.now();

        assertSame(aiResponse, result);

        assertEquals(
                "RIDICAT",
                result.getTrafficLevel()
        );

        assertEquals(
                3,
                result.getRecommendedWaiters()
        );

        assertEquals(
                2,
                result.getRecommendedKitchenStaff()
        );

        assertEquals(
                1,
                result.getRecommendedBarStaff()
        );

        assertEquals(
                "MEDIU",
                result.getDelayRisk()
        );

        assertEquals(
                2,
                result.getActiveWaiters()
        );

        assertEquals(
                3,
                result.getActiveKitchenStaff()
        );

        assertEquals(
                1,
                result.getActiveBarStaff()
        );

        assertEquals(
                1,
                result.getWaiterDeficit()
        );

        assertEquals(
                -1,
                result.getKitchenDeficit()
        );

        assertEquals(
                0,
                result.getBarDeficit()
        );

        assertEquals(
                1,
                result.getTotalStaffDeficit()
        );

        DecisionRequest sentRequest =
                requestCaptor.getValue();

        int expectedDayBefore =
                beforeCall.getDayOfWeek().getValue() - 1;

        int expectedDayAfter =
                afterCall.getDayOfWeek().getValue() - 1;

        /*
         * Acceptam ziua de dinainte sau de dupa apel
         * pentru cazul rar in care testul ruleaza exact
         * la schimbarea zilei.
         */
        assertTrue(
                sentRequest.getDayOfWeek()
                        == expectedDayBefore
                        || sentRequest.getDayOfWeek()
                        == expectedDayAfter
        );

        assertTrue(
                sentRequest.getHour() >= 0
                        && sentRequest.getHour() <= 23
        );

        assertEquals(
                2,
                sentRequest.getActiveOrders()
        );

        assertEquals(
                3,
                sentRequest.getOccupiedTables()
        );

        assertEquals(
                25,
                sentRequest.getEstimatedOccupancy()
        );

        assertEquals(
                1,
                sentRequest.getKitchenLoad()
        );

        assertEquals(
                1,
                sentRequest.getBarLoad()
        );

        assertEquals(
                20,
                sentRequest.getAvgPreparationTime()
        );

        assertEquals(
                3,
                sentRequest.getOrdersLast30Min()
        );

        /*
         * Comanda are 50 de minute, dar valoarea
         * este limitata la maximum 40.
         */
        assertEquals(
                40,
                sentRequest.getOrderAgeMinutes()
        );

        /*
         * Sunt nefinalizate:
         * - produsul de bucatarie;
         * - produsul de bar;
         * - produsul fara Product;
         * - produsul fara Category.
         */
        assertEquals(
                4,
                sentRequest.getItemCount()
        );

        assertEquals(
                2,
                sentRequest.getActiveWaiters()
        );

        assertEquals(
                3,
                sentRequest.getActiveKitchen()
        );

        assertEquals(
                1,
                sentRequest.getActiveBar()
        );

        verify(employeeShiftService)
                .getActiveStaffSummary();

        ArgumentCaptor<DecisionTrainingRecord>
                recordCaptor =
                ArgumentCaptor.forClass(
                        DecisionTrainingRecord.class
                );

        verify(decisionTrainingRecordRepository)
                .save(recordCaptor.capture());

        DecisionTrainingRecord savedRecord =
                recordCaptor.getValue();

        assertNotNull(savedRecord.getCreatedAt());

        assertEquals(
                sentRequest.getDayOfWeek(),
                savedRecord.getDayOfWeek()
        );

        assertEquals(
                sentRequest.getHour(),
                savedRecord.getHour()
        );

        assertEquals(
                2,
                savedRecord.getActiveOrders()
        );

        assertEquals(
                3,
                savedRecord.getOccupiedTables()
        );

        assertEquals(
                25,
                savedRecord.getEstimatedOccupancy()
        );

        assertEquals(
                1,
                savedRecord.getKitchenLoad()
        );

        assertEquals(
                1,
                savedRecord.getBarLoad()
        );

        assertEquals(
                20,
                savedRecord.getAvgPreparationTime()
        );

        assertEquals(
                3,
                savedRecord.getOrdersLast30Min()
        );

        assertEquals(
                40,
                savedRecord.getOrderAgeMinutes()
        );

        assertEquals(
                4,
                savedRecord.getItemCount()
        );

        assertEquals(
                "RIDICAT",
                savedRecord.getPredictedTrafficLevel()
        );

        assertEquals(
                3,
                savedRecord.getRecommendedWaiters()
        );

        assertEquals(
                2,
                savedRecord.getRecommendedKitchenStaff()
        );

        assertEquals(
                1,
                savedRecord.getRecommendedBarStaff()
        );

        assertEquals(
                "MEDIU",
                savedRecord.getPredictedDelayRisk()
        );
    }

    @Test
    void saveDecisionTrainingRecordShouldCopyAllRequestFields() {
        DecisionRequest request =
                new DecisionRequest(
                        4,
                        18,
                        7,
                        5,
                        42,
                        6,
                        3,
                        27,
                        8,
                        19,
                        9
                );

        DecisionResponse response =
                createDecisionResponse(
                        "MEDIU",
                        3,
                        2,
                        1,
                        "RIDICAT"
                );

        ReflectionTestUtils.invokeMethod(
                decisionService,
                "saveDecisionTrainingRecord",
                request,
                response
        );

        ArgumentCaptor<DecisionTrainingRecord>
                recordCaptor =
                ArgumentCaptor.forClass(
                        DecisionTrainingRecord.class
                );

        verify(decisionTrainingRecordRepository)
                .save(recordCaptor.capture());

        DecisionTrainingRecord savedRecord =
                recordCaptor.getValue();

        assertNotNull(savedRecord.getCreatedAt());

        assertEquals(
                4,
                savedRecord.getDayOfWeek()
        );

        assertEquals(
                18,
                savedRecord.getHour()
        );

        assertEquals(
                7,
                savedRecord.getActiveOrders()
        );

        assertEquals(
                5,
                savedRecord.getOccupiedTables()
        );

        assertEquals(
                42,
                savedRecord.getEstimatedOccupancy()
        );

        assertEquals(
                6,
                savedRecord.getKitchenLoad()
        );

        assertEquals(
                3,
                savedRecord.getBarLoad()
        );

        assertEquals(
                27,
                savedRecord.getAvgPreparationTime()
        );

        assertEquals(
                8,
                savedRecord.getOrdersLast30Min()
        );

        assertEquals(
                19,
                savedRecord.getOrderAgeMinutes()
        );

        assertEquals(
                9,
                savedRecord.getItemCount()
        );

        assertEquals(
                "MEDIU",
                savedRecord.getPredictedTrafficLevel()
        );

        assertEquals(
                3,
                savedRecord.getRecommendedWaiters()
        );

        assertEquals(
                2,
                savedRecord.getRecommendedKitchenStaff()
        );

        assertEquals(
                1,
                savedRecord.getRecommendedBarStaff()
        );

        assertEquals(
                "RIDICAT",
                savedRecord.getPredictedDelayRisk()
        );
    }

    @Test
    void getDecisionSummaryShouldReturnFallbackWhenAiReturnsNull() {
        configureEmptyCurrentState();

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        any(DecisionRequest.class),
                        eq(DecisionResponse.class)
                )
        ).thenReturn(null);

        DecisionResponse result =
                decisionService.getDecisionSummary();

        assertFallbackResponse(result);

        ArgumentCaptor<DecisionTrainingRecord>
                recordCaptor =
                ArgumentCaptor.forClass(
                        DecisionTrainingRecord.class
                );

        verify(decisionTrainingRecordRepository)
                .save(recordCaptor.capture());

        DecisionTrainingRecord savedRecord =
                recordCaptor.getValue();

        assertNotNull(savedRecord.getCreatedAt());

        assertEquals(
                0,
                savedRecord.getActiveOrders()
        );

        assertEquals(
                0,
                savedRecord.getOccupiedTables()
        );

        assertEquals(
                0,
                savedRecord.getEstimatedOccupancy()
        );

        assertEquals(
                0,
                savedRecord.getKitchenLoad()
        );

        assertEquals(
                0,
                savedRecord.getBarLoad()
        );

        assertEquals(
                20,
                savedRecord.getAvgPreparationTime()
        );

        assertEquals(
                0,
                savedRecord.getOrdersLast30Min()
        );

        assertEquals(
                0,
                savedRecord.getOrderAgeMinutes()
        );

        assertEquals(
                0,
                savedRecord.getItemCount()
        );

        assertNull(
                savedRecord.getPredictedTrafficLevel()
        );

        assertNull(
                savedRecord.getRecommendedWaiters()
        );

        assertNull(
                savedRecord.getRecommendedKitchenStaff()
        );

        assertNull(
                savedRecord.getRecommendedBarStaff()
        );

        assertNull(
                savedRecord.getPredictedDelayRisk()
        );
    }

    @Test
    void getDecisionSummaryShouldReturnFallbackWhenAiIsUnavailable() {
        configureEmptyCurrentState();

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        any(DecisionRequest.class),
                        eq(DecisionResponse.class)
                )
        ).thenThrow(
                new RestClientException(
                        "Connection refused"
                )
        );

        DecisionResponse result =
                decisionService.getDecisionSummary();

        assertFallbackResponse(result);

        verify(decisionTrainingRecordRepository)
                .save(any(DecisionTrainingRecord.class));
    }

    @Test
    void getDecisionSummaryShouldLimitEstimatedOccupancyToOneHundred() {
        when(orderService.getActiveOrders())
                .thenReturn(Collections.emptyList());

        when(
                orderService.countOrdersCreatedAfter(
                        any(LocalDateTime.class)
                )
        ).thenReturn(0);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(
                        List.of(
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class),
                                mock(TableSession.class)
                        )
                );

        ArgumentCaptor<DecisionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        DecisionRequest.class
                );

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        requestCaptor.capture(),
                        eq(DecisionResponse.class)
                )
        ).thenReturn(
                createDecisionResponse(
                        "RIDICAT",
                        4,
                        3,
                        2,
                        "RIDICAT"
                )
        );

        decisionService.getDecisionSummary();

        assertEquals(
                100,
                requestCaptor
                        .getValue()
                        .getEstimatedOccupancy()
        );
    }

    @Test
    void getDecisionSummaryShouldCalculateAgeBelowMaximumLimit() {
        Order order = new Order();

        order.setCreatedAt(
                LocalDateTime.now().minusMinutes(10)
        );

        when(orderService.getActiveOrders())
                .thenReturn(List.of(order));

        when(
                orderService.countOrdersCreatedAfter(
                        any(LocalDateTime.class)
                )
        ).thenReturn(1);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<DecisionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        DecisionRequest.class
                );

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        requestCaptor.capture(),
                        eq(DecisionResponse.class)
                )
        ).thenReturn(
                createDecisionResponse(
                        "SCAZUT",
                        1,
                        1,
                        1,
                        "SCAZUT"
                )
        );

        decisionService.getDecisionSummary();

        int calculatedAge =
                requestCaptor
                        .getValue()
                        .getOrderAgeMinutes();

        /*
         * Testul poate rula la limita dintre doua minute,
         * de aceea acceptam 9 sau 10.
         */
        assertTrue(
                calculatedAge >= 9
                        && calculatedAge <= 10
        );
    }

    @Test
    void getDecisionSummaryShouldDistinguishKitchenBarAndFinishedItems() {
        Product kitchenProduct =
                createProductWithCategory("Pizza");

        Product barProduct =
                createProductWithCategory("Bauturi");

        Order order = new Order();

        /*
         * Bucatarie:
         * doua nefinalizate si una finalizata.
         */
        order.addItem(
                createOrderItem(
                        kitchenProduct,
                        OrderStatus.NOUA
                )
        );

        order.addItem(
                createOrderItem(
                        kitchenProduct,
                        OrderStatus.IN_PREPARARE
                )
        );

        order.addItem(
                createOrderItem(
                        kitchenProduct,
                        OrderStatus.GATA
                )
        );

        /*
         * Bar:
         * una nefinalizata si doua finalizate.
         */
        order.addItem(
                createOrderItem(
                        barProduct,
                        OrderStatus.NOUA
                )
        );

        order.addItem(
                createOrderItem(
                        barProduct,
                        OrderStatus.GATA
                )
        );

        order.addItem(
                createOrderItem(
                        barProduct,
                        OrderStatus.GATA
                )
        );

        when(orderService.getActiveOrders())
                .thenReturn(List.of(order));

        when(
                orderService.countOrdersCreatedAfter(
                        any(LocalDateTime.class)
                )
        ).thenReturn(1);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(Collections.emptyList());

        ArgumentCaptor<DecisionRequest> requestCaptor =
                ArgumentCaptor.forClass(
                        DecisionRequest.class
                );

        when(
                restTemplate.postForObject(
                        eq(AI_SERVICE_URL),
                        requestCaptor.capture(),
                        eq(DecisionResponse.class)
                )
        ).thenReturn(
                createDecisionResponse(
                        "MEDIU",
                        2,
                        2,
                        1,
                        "SCAZUT"
                )
        );

        decisionService.getDecisionSummary();

        DecisionRequest sentRequest =
                requestCaptor.getValue();

        assertEquals(
                2,
                sentRequest.getKitchenLoad()
        );

        assertEquals(
                1,
                sentRequest.getBarLoad()
        );

        assertEquals(
                3,
                sentRequest.getItemCount()
        );
    }

    @Test
    void retrainModelsShouldReturnSuccessfulResponseAndSendToken() {
        ResponseEntity<String> aiResponse =
                ResponseEntity.ok("""
                        {
                          "status": "success",
                          "modelsReplaced": true
                        }
                        """);

        ArgumentCaptor<HttpEntity<Void>>
                requestCaptor =
                ArgumentCaptor.forClass(
                        HttpEntity.class
                );

        when(
                retrainingRestTemplate.exchange(
                        eq(AI_RETRAIN_URL),
                        eq(HttpMethod.POST),
                        requestCaptor.capture(),
                        eq(String.class)
                )
        ).thenReturn(aiResponse);

        ResponseEntity<String> result =
                decisionService.retrainModels();

        assertEquals(
                HttpStatus.OK,
                result.getStatusCode()
        );

        assertEquals(
                aiResponse.getBody(),
                result.getBody()
        );

        HttpEntity<Void> sentRequest =
                requestCaptor.getValue();

        assertEquals(
                RETRAIN_TOKEN,
                sentRequest
                        .getHeaders()
                        .getFirst("X-Retrain-Token")
        );

        assertTrue(
                sentRequest
                        .getHeaders()
                        .getAccept()
                        .contains(
                                MediaType.APPLICATION_JSON
                        )
        );
    }

    @Test
    void retrainModelsShouldPreserveAiErrorStatusBodyAndContentType() {
        HttpHeaders responseHeaders =
                new HttpHeaders();

        responseHeaders.setContentType(
                MediaType.APPLICATION_JSON
        );

        String responseBody = """
                {
                  "status": "blocked",
                  "modelsReplaced": false,
                  "message": "Sunt necesare 30 de inregistrari."
                }
                """;

        HttpClientErrorException exception =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        responseHeaders,
                        responseBody.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        StandardCharsets.UTF_8
                );

        when(
                retrainingRestTemplate.exchange(
                        eq(AI_RETRAIN_URL),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenThrow(exception);

        ResponseEntity<String> result =
                decisionService.retrainModels();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                result.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_JSON,
                result.getHeaders().getContentType()
        );

        assertEquals(
                responseBody,
                result.getBody()
        );
    }

    @Test
    void retrainModelsShouldUseJsonContentTypeWhenAiErrorHasNoContentType() {
        HttpHeaders responseHeaders =
                new HttpHeaders();

        String responseBody = """
                {
                  "status": "blocked"
                }
                """;

        HttpClientErrorException exception =
                HttpClientErrorException.create(
                        HttpStatus.BAD_REQUEST,
                        "Bad Request",
                        responseHeaders,
                        responseBody.getBytes(
                                StandardCharsets.UTF_8
                        ),
                        StandardCharsets.UTF_8
                );

        when(
                retrainingRestTemplate.exchange(
                        eq(AI_RETRAIN_URL),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenThrow(exception);

        ResponseEntity<String> result =
                decisionService.retrainModels();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                result.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_JSON,
                result.getHeaders().getContentType()
        );

        assertEquals(
                responseBody,
                result.getBody()
        );
    }

    @Test
    void retrainModelsShouldUseJsonContentTypeWhenResponseHeadersAreNull() {
        String responseBody = """
                {
                  "status": "blocked",
                  "modelsReplaced": false
                }
                """;

        HttpClientErrorException exception =
                mock(HttpClientErrorException.class);

        when(exception.getResponseHeaders())
                .thenReturn(null);

        when(exception.getStatusCode())
                .thenReturn(HttpStatus.BAD_REQUEST);

        when(exception.getResponseBodyAsString())
                .thenReturn(responseBody);

        when(
                retrainingRestTemplate.exchange(
                        eq(AI_RETRAIN_URL),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenThrow(exception);

        ResponseEntity<String> result =
                decisionService.retrainModels();

        assertEquals(
                HttpStatus.BAD_REQUEST,
                result.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_JSON,
                result.getHeaders().getContentType()
        );

        assertEquals(
                responseBody,
                result.getBody()
        );
    }

    @Test
    void retrainModelsShouldReturnServiceUnavailableWhenAiCannotBeReached() {
        when(
                retrainingRestTemplate.exchange(
                        eq(AI_RETRAIN_URL),
                        eq(HttpMethod.POST),
                        any(HttpEntity.class),
                        eq(String.class)
                )
        ).thenThrow(
                new RestClientException(
                        "Connection refused"
                )
        );

        ResponseEntity<String> result =
                decisionService.retrainModels();

        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                result.getStatusCode()
        );

        assertEquals(
                MediaType.APPLICATION_JSON,
                result.getHeaders().getContentType()
        );

        assertNotNull(result.getBody());

        assertTrue(
                result.getBody().contains(
                        "\"status\": \"error\""
                )
        );

        assertTrue(
                result.getBody().contains(
                        "\"modelsReplaced\": false"
                )
        );

        assertTrue(
                result.getBody().contains(
                        "AI Service nu este disponibil."
                )
        );
    }

    private void configureEmptyCurrentState() {
        when(orderService.getActiveOrders())
                .thenReturn(Collections.emptyList());

        when(
                orderService.countOrdersCreatedAfter(
                        any(LocalDateTime.class)
                )
        ).thenReturn(0);

        when(tableSessionRepository.findByActiveTrue())
                .thenReturn(Collections.emptyList());
    }

    private Product createProductWithCategory(
            String categoryName
    ) {
        Category category =
                mock(Category.class);

        when(category.getName())
                .thenReturn(categoryName);

        Product product =
                mock(Product.class);

        when(product.getCategory())
                .thenReturn(category);

        return product;
    }

    private OrderItem createOrderItem(
            Product product,
            OrderStatus status
    ) {
        OrderItem item = new OrderItem();

        item.setProduct(product);
        item.setQuantity(1);
        item.setUnitPrice(BigDecimal.TEN);
        item.setSubtotal(BigDecimal.TEN);
        item.setStatus(status);

        return item;
    }

    private DecisionResponse createDecisionResponse(
            String trafficLevel,
            int waiters,
            int kitchenStaff,
            int barStaff,
            String delayRisk
    ) {
        DecisionResponse response =
                new DecisionResponse();

        response.setTrafficLevel(trafficLevel);
        response.setRecommendedWaiters(waiters);

        response.setRecommendedKitchenStaff(
                kitchenStaff
        );

        response.setRecommendedBarStaff(barStaff);
        response.setDelayRisk(delayRisk);

        return response;
    }

    private void assertFallbackResponse(
            DecisionResponse response
    ) {
        assertNotNull(response);

        assertEquals(
                "NECUNOSCUT",
                response.getTrafficLevel()
        );

        assertEquals(
                0,
                response.getRecommendedWaiters()
        );

        assertEquals(
                0,
                response.getRecommendedKitchenStaff()
        );

        assertEquals(
                0,
                response.getRecommendedBarStaff()
        );

        assertEquals(
                "NECUNOSCUT",
                response.getDelayRisk()
        );

        assertEquals(
                2,
                response.getActiveWaiters()
        );

        assertEquals(
                3,
                response.getActiveKitchenStaff()
        );

        assertEquals(
                1,
                response.getActiveBarStaff()
        );

        assertEquals(
                0,
                response.getWaiterDeficit()
        );

        assertEquals(
                0,
                response.getKitchenDeficit()
        );

        assertEquals(
                0,
                response.getBarDeficit()
        );

        assertEquals(
                0,
                response.getTotalStaffDeficit()
        );
    }
}