package com.licenta.horeca.service;

import com.licenta.horeca.dto.DecisionRequest;
import com.licenta.horeca.dto.DecisionResponse;
import com.licenta.horeca.entity.DecisionTrainingRecord;
import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.repository.DecisionTrainingRecordRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DecisionService {

    private static final String AI_SERVICE_URL =
            "http://127.0.0.1:5000/predict/all";

    private static final String AI_RETRAIN_URL =
            "http://127.0.0.1:5000/retrain";

    private final RestTemplate restTemplate;

    private final RestTemplate retrainingRestTemplate;

    private final OrderService orderService;

    private final TableSessionRepository
            tableSessionRepository;

    private final DecisionTrainingRecordRepository
            decisionTrainingRecordRepository;

    private final String retrainToken;

    public DecisionService(
            OrderService orderService,
            TableSessionRepository tableSessionRepository,
            DecisionTrainingRecordRepository
                    decisionTrainingRecordRepository,
            @Value("${ai.service.retrain-token}")
            String retrainToken
    ) {
        SimpleClientHttpRequestFactory requestFactory =
                new SimpleClientHttpRequestFactory();

        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        SimpleClientHttpRequestFactory
                retrainingRequestFactory =
                new SimpleClientHttpRequestFactory();

        retrainingRequestFactory.setConnectTimeout(3000);

        /*
         * Reantrenarea poate dura mai mult decât
         * realizarea unei singure predicții.
         */
        retrainingRequestFactory.setReadTimeout(120000);

        this.restTemplate =
                new RestTemplate(requestFactory);

        this.retrainingRestTemplate =
                new RestTemplate(
                        retrainingRequestFactory
                );

        this.orderService = orderService;

        this.tableSessionRepository =
                tableSessionRepository;

        this.decisionTrainingRecordRepository =
                decisionTrainingRecordRepository;

        this.retrainToken = retrainToken;
    }

    public DecisionResponse getDecisionSummary() {
        System.out.println(
                "=== AM INTRAT IN DECISION SERVICE ==="
        );

        DecisionRequest request =
                buildCurrentDecisionRequest();

        System.out.println(
                "=== TRIMIT REQUEST CATRE AI SERVICE ==="
        );

        try {
            DecisionResponse response =
                    restTemplate.postForObject(
                            AI_SERVICE_URL,
                            request,
                            DecisionResponse.class
                    );

            if (response == null) {
                System.out.println(
                        "=== AI SERVICE A RETURNAT RASPUNS GOL ==="
                );

                saveDecisionTrainingRecord(
                        request,
                        null
                );

                return buildFallbackResponse();
            }

            System.out.println(
                    "=== RASPUNS PRIMIT DE LA AI SERVICE ==="
            );

            saveDecisionTrainingRecord(
                    request,
                    response
            );

            return response;

        } catch (RestClientException exception) {
            System.out.println(
                    "=== AI SERVICE NU A RASPUNS ==="
            );

            System.out.println(
                    exception.getMessage()
            );

            saveDecisionTrainingRecord(
                    request,
                    null
            );

            return buildFallbackResponse();
        }
    }

    public ResponseEntity<String> retrainModels() {
        System.out.println(
                "=== TRIMIT CERERE DE REANTRENARE ==="
        );

        HttpHeaders headers =
                new HttpHeaders();

        headers.set(
                "X-Retrain-Token",
                retrainToken
        );

        headers.setAccept(
                List.of(
                        MediaType.APPLICATION_JSON
                )
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    retrainingRestTemplate.exchange(
                            AI_RETRAIN_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            System.out.println(
                    "=== RASPUNS REANTRENARE PRIMIT ==="
            );

            return response;

        } catch (
                HttpStatusCodeException exception
        ) {
            /*
             * Păstrăm statusul și corpul trimise
             * de AI Service. De exemplu, status 400
             * când nu există încă 30 de înregistrări.
             */
            MediaType responseContentType =
                    MediaType.APPLICATION_JSON;

            if (
                    exception.getResponseHeaders()
                            != null
                            && exception
                            .getResponseHeaders()
                            .getContentType()
                            != null
            ) {
                responseContentType =
                        exception
                                .getResponseHeaders()
                                .getContentType();
            }

            System.out.println(
                    "=== REANTRENAREA A FOST RESPINSA ==="
            );

            System.out.println(
                    exception
                            .getResponseBodyAsString()
            );

            return ResponseEntity
                    .status(
                            exception.getStatusCode()
                    )
                    .contentType(
                            responseContentType
                    )
                    .body(
                            exception
                                    .getResponseBodyAsString()
                    );

        } catch (RestClientException exception) {
            System.out.println(
                    "=== AI SERVICE NU ESTE DISPONIBIL "
                            + "PENTRU REANTRENARE ==="
            );

            System.out.println(
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(
                            HttpStatus.SERVICE_UNAVAILABLE
                    )
                    .contentType(
                            MediaType.APPLICATION_JSON
                    )
                    .body(
                            """
                            {
                              "status": "error",
                              "modelsReplaced": false,
                              "message": "AI Service nu este disponibil."
                            }
                            """
                    );
        }
    }

    private void saveDecisionTrainingRecord(
            DecisionRequest request,
            DecisionResponse response
    ) {
        DecisionTrainingRecord record =
                new DecisionTrainingRecord();

        record.setCreatedAt(
                LocalDateTime.now()
        );

        record.setDayOfWeek(
                request.getDayOfWeek()
        );

        record.setHour(
                request.getHour()
        );

        record.setActiveOrders(
                request.getActiveOrders()
        );

        record.setOccupiedTables(
                request.getOccupiedTables()
        );

        record.setEstimatedOccupancy(
                request.getEstimatedOccupancy()
        );

        record.setKitchenLoad(
                request.getKitchenLoad()
        );

        record.setBarLoad(
                request.getBarLoad()
        );

        record.setAvgPreparationTime(
                request.getAvgPreparationTime()
        );

        record.setOrdersLast30Min(
                request.getOrdersLast30Min()
        );

        record.setOrderAgeMinutes(
                request.getOrderAgeMinutes()
        );

        record.setItemCount(
                request.getItemCount()
        );

        if (response != null) {
            record.setPredictedTrafficLevel(
                    response.getTrafficLevel()
            );

            record.setRecommendedWaiters(
                    response.getRecommendedWaiters()
            );

            record.setRecommendedKitchenStaff(
                    response
                            .getRecommendedKitchenStaff()
            );

            record.setRecommendedBarStaff(
                    response.getRecommendedBarStaff()
            );

            record.setPredictedDelayRisk(
                    response.getDelayRisk()
            );
        }

        decisionTrainingRecordRepository.save(
                record
        );

        System.out.println(
                "=== DATELE PENTRU REANTRENARE "
                        + "AU FOST SALVATE ==="
        );
    }

    private DecisionRequest
    buildCurrentDecisionRequest() {
        System.out.println(
                "=== INCEP SA CONSTRUIESC DATELE "
                        + "PENTRU AI ==="
        );

        LocalDateTime now =
                LocalDateTime.now();

        int dayOfWeek =
                now.getDayOfWeek()
                        .getValue() - 1;

        int hour =
                now.getHour();

        System.out.println(
                "Citesc comenzile active..."
        );

        List<Order> activeOrdersList =
                orderService.getActiveOrders();

        System.out.println(
                "Comenzi active citite: "
                        + activeOrdersList.size()
        );

        System.out.println(
                "Calculez activeOrders..."
        );

        int activeOrders =
                activeOrdersList.size();

        System.out.println(
                "Calculez occupiedTables..."
        );

        int occupiedTables =
                countOccupiedTables();

        System.out.println(
                "Calculez estimatedOccupancy..."
        );

        int estimatedOccupancy =
                calculateEstimatedOccupancy(
                        occupiedTables
                );

        System.out.println(
                "Calculez kitchenLoad..."
        );

        int kitchenLoad =
                calculateKitchenLoad(
                        activeOrdersList
                );

        System.out.println(
                "Calculez barLoad..."
        );

        int barLoad =
                calculateBarLoad(
                        activeOrdersList
                );

        System.out.println(
                "Setez avgPreparationTime..."
        );

        int avgPreparationTime = 20;

        System.out.println(
                "Calculez ordersLast30Min..."
        );

        int ordersLast30Min =
                orderService
                        .countOrdersCreatedAfter(
                                now.minusMinutes(30)
                        );

        System.out.println(
                "Calculez orderAgeMinutes..."
        );

        int orderAgeMinutes =
                calculateOldestActiveOrderAge(
                        activeOrdersList,
                        now
                );

        System.out.println(
                "Calculez itemCount..."
        );

        int itemCount =
                calculateUnfinishedItemCount(
                        activeOrdersList
                );

        System.out.println(
                "=== DATE TRIMISE CATRE "
                        + "AI SERVICE ==="
        );

        System.out.println(
                "dayOfWeek = " + dayOfWeek
        );

        System.out.println(
                "hour = " + hour
        );

        System.out.println(
                "activeOrders = " + activeOrders
        );

        System.out.println(
                "occupiedTables = "
                        + occupiedTables
        );

        System.out.println(
                "estimatedOccupancy = "
                        + estimatedOccupancy
        );

        System.out.println(
                "kitchenLoad = " + kitchenLoad
        );

        System.out.println(
                "barLoad = " + barLoad
        );

        System.out.println(
                "avgPreparationTime = "
                        + avgPreparationTime
        );

        System.out.println(
                "ordersLast30Min = "
                        + ordersLast30Min
        );

        System.out.println(
                "orderAgeMinutes = "
                        + orderAgeMinutes
        );

        System.out.println(
                "itemCount = " + itemCount
        );

        System.out.println(
                "====================================="
        );

        return new DecisionRequest(
                dayOfWeek,
                hour,
                activeOrders,
                occupiedTables,
                estimatedOccupancy,
                kitchenLoad,
                barLoad,
                avgPreparationTime,
                ordersLast30Min,
                orderAgeMinutes,
                itemCount
        );
    }

    private int countOccupiedTables() {
        System.out.println(
                "Citesc sesiunile active ale meselor..."
        );

        List<TableSession> sessions =
                tableSessionRepository
                        .findByActiveTrue();

        int occupiedTables =
                sessions.size();

        System.out.println(
                "Mese ocupate: "
                        + occupiedTables
        );

        return occupiedTables;
    }

    private int calculateEstimatedOccupancy(
            int occupiedTables
    ) {
        int totalTables = 12;

        if (totalTables == 0) {
            return 0;
        }

        return Math.min(
                100,
                (occupiedTables * 100)
                        / totalTables
        );
    }

    private int calculateKitchenLoad(
            List<Order> activeOrdersList
    ) {
        int kitchenLoad = 0;

        for (
                Order order :
                activeOrdersList
        ) {
            for (
                    OrderItem item :
                    order.getItems()
            ) {
                if (
                        isUnfinishedItem(item)
                                && isKitchenItem(item)
                ) {
                    kitchenLoad++;
                }
            }
        }

        return kitchenLoad;
    }

    private int calculateBarLoad(
            List<Order> activeOrdersList
    ) {
        int barLoad = 0;

        for (
                Order order :
                activeOrdersList
        ) {
            for (
                    OrderItem item :
                    order.getItems()
            ) {
                if (
                        isUnfinishedItem(item)
                                && isBarItem(item)
                ) {
                    barLoad++;
                }
            }
        }

        return barLoad;
    }

    private boolean isUnfinishedItem(
            OrderItem item
    ) {
        return item.getStatus()
                != OrderStatus.GATA;
    }

    private boolean isKitchenItem(
            OrderItem item
    ) {
        if (
                item.getProduct() == null
                        || item
                        .getProduct()
                        .getCategory()
                        == null
        ) {
            return false;
        }

        String categoryName =
                item.getProduct()
                        .getCategory()
                        .getName();

        return !categoryName
                .equalsIgnoreCase(
                        "Bauturi"
                );
    }

    private boolean isBarItem(
            OrderItem item
    ) {
        if (
                item.getProduct() == null
                        || item
                        .getProduct()
                        .getCategory()
                        == null
        ) {
            return false;
        }

        String categoryName =
                item.getProduct()
                        .getCategory()
                        .getName();

        return categoryName
                .equalsIgnoreCase(
                        "Bauturi"
                );
    }

    private int calculateOldestActiveOrderAge(
            List<Order> activeOrdersList,
            LocalDateTime now
    ) {
        int age =
                activeOrdersList.stream()
                        .filter(
                                order ->
                                        order.getCreatedAt()
                                                != null
                        )
                        .mapToInt(
                                order ->
                                        (int) Duration
                                                .between(
                                                        order
                                                                .getCreatedAt(),
                                                        now
                                                )
                                                .toMinutes()
                        )
                        .max()
                        .orElse(0);

        return Math.min(
                age,
                40
        );
    }

    private int calculateUnfinishedItemCount(
            List<Order> activeOrdersList
    ) {
        int itemCount = 0;

        for (
                Order order :
                activeOrdersList
        ) {
            for (
                    OrderItem item :
                    order.getItems()
            ) {
                if (
                        isUnfinishedItem(item)
                ) {
                    itemCount++;
                }
            }
        }

        return itemCount;
    }

    private DecisionResponse
    buildFallbackResponse() {
        DecisionResponse response =
                new DecisionResponse();

        response.setTrafficLevel(
                "NECUNOSCUT"
        );

        response.setRecommendedWaiters(0);

        response.setRecommendedKitchenStaff(0);

        response.setRecommendedBarStaff(0);

        response.setDelayRisk(
                "NECUNOSCUT"
        );

        return response;
    }
}