package com.licenta.horeca.decision.service;

import com.licenta.horeca.decision.dto.DecisionRequest;
import com.licenta.horeca.decision.dto.DecisionResponse;
import com.licenta.horeca.dto.shift.ActiveStaffSummaryResponse;
import com.licenta.horeca.decision.entity.DecisionTrainingRecord;
import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.service.EmployeeShiftService;
import com.licenta.horeca.table.entity.TableSession;
import com.licenta.horeca.order.enums.OrderStatus;
import com.licenta.horeca.order.service.OrderService;
import com.licenta.horeca.decision.repository.DecisionTrainingRecordRepository;
import com.licenta.horeca.table.repository.TableSessionRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@Service
public class DecisionService {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(DecisionService.class);

    private static final String AI_SERVICE_URL =
            "http://127.0.0.1:5000/predict/all";

    private static final String AI_RETRAIN_URL =
            "http://127.0.0.1:5000/retrain";

    private final RestTemplate restTemplate;
    private final RestTemplate retrainingRestTemplate;
    private final OrderService orderService;
    private final EmployeeShiftService employeeShiftService;
    private final TableSessionRepository tableSessionRepository;
    private final DecisionTrainingRecordRepository
            decisionTrainingRecordRepository;
    private final String retrainToken;

    public DecisionService(
            OrderService orderService,
            EmployeeShiftService employeeShiftService,
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

        this.employeeShiftService =
                employeeShiftService;

        this.tableSessionRepository =
                tableSessionRepository;

        this.decisionTrainingRecordRepository =
                decisionTrainingRecordRepository;

        this.retrainToken = retrainToken;
    }

    public DecisionResponse getDecisionSummary() {
        DecisionRequest request =
                buildCurrentDecisionRequest();

        try {
            DecisionResponse response =
                    restTemplate.postForObject(
                            AI_SERVICE_URL,
                            request,
                            DecisionResponse.class
                    );

            if (response == null) {
                LOGGER.warn(
                        "AI Service a returnat un răspuns gol. "
                                + "Se folosește răspunsul de rezervă."
                );

                saveDecisionTrainingRecord(
                        request,
                        null
                );

                return buildFallbackResponse(request);
            }

            applyStaffingComparison(
                    request,
                    response
            );

            LOGGER.info(
                    "Predicția AI a fost primită cu succes."
            );

            saveDecisionTrainingRecord(
                    request,
                    response
            );

            return response;

        } catch (RestClientException exception) {
            LOGGER.warn(
                    "AI Service nu este disponibil. "
                            + "Se folosește răspunsul de rezervă: {}",
                    exception.getMessage()
            );

            saveDecisionTrainingRecord(
                    request,
                    null
            );

            return buildFallbackResponse(request);
        }
    }

    public ResponseEntity<String> retrainModels() {
        HttpHeaders headers = new HttpHeaders();

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

        LOGGER.info(
                "A fost trimisă cererea de reantrenare."
        );

        try {
            ResponseEntity<String> response =
                    retrainingRestTemplate.exchange(
                            AI_RETRAIN_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );

            LOGGER.info(
                    "Reantrenarea modelelor s-a încheiat "
                            + "cu statusul {}.",
                    response.getStatusCode()
            );

            return response;

        } catch (HttpStatusCodeException exception) {
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

            LOGGER.warn(
                    "Cererea de reantrenare a fost respinsă "
                            + "cu statusul {}.",
                    exception.getStatusCode()
            );

            LOGGER.debug(
                    "Răspunsul AI Service la reantrenare: {}",
                    exception.getResponseBodyAsString()
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
            LOGGER.warn(
                    "AI Service nu este disponibil "
                            + "pentru reantrenare: {}",
                    exception.getMessage()
            );

            return ResponseEntity
                    .status(
                            HttpStatus
                                    .SERVICE_UNAVAILABLE
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

        LOGGER.debug(
                "Înregistrarea pentru reantrenare "
                        + "a fost salvată."
        );
    }

    private DecisionRequest
    buildCurrentDecisionRequest() {
        LocalDateTime now =
                LocalDateTime.now();

        int dayOfWeek =
                now.getDayOfWeek()
                        .getValue() - 1;

        int hour =
                now.getHour();

        List<Order> activeOrdersList =
                orderService.getActiveOrders();

        int activeOrders =
                activeOrdersList.size();

        int occupiedTables =
                countOccupiedTables();

        int estimatedOccupancy =
                calculateEstimatedOccupancy(
                        occupiedTables
                );

        int kitchenLoad =
                calculateKitchenLoad(
                        activeOrdersList
                );

        int barLoad =
                calculateBarLoad(
                        activeOrdersList
                );

        int avgPreparationTime = 20;

        int ordersLast30Min =
                orderService
                        .countOrdersCreatedAfter(
                                now.minusMinutes(30)
                        );

        int orderAgeMinutes =
                calculateOldestActiveOrderAge(
                        activeOrdersList,
                        now
                );

        int itemCount =
                calculateUnfinishedItemCount(
                        activeOrdersList
                );

        ActiveStaffSummaryResponse activeStaffSummary =
                employeeShiftService
                        .getActiveStaffSummary();

        int activeWaiters =
                Math.toIntExact(
                        activeStaffSummary.getWaiters()
                );

        int activeKitchen =
                Math.toIntExact(
                        activeStaffSummary
                                .getKitchenEmployees()
                );

        int activeBar =
                Math.toIntExact(
                        activeStaffSummary.getBarEmployees()
                );

        LOGGER.debug(
                "Date AI: dayOfWeek={}, hour={}, "
                        + "activeOrders={}, occupiedTables={}, "
                        + "estimatedOccupancy={}, kitchenLoad={}, "
                        + "barLoad={}, avgPreparationTime={}, "
                        + "ordersLast30Min={}, orderAgeMinutes={}, "
                        + "itemCount={}, activeWaiters={}, "
                        + "activeKitchen={}, activeBar={}",
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
                itemCount,
                activeWaiters,
                activeKitchen,
                activeBar
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
                itemCount,
                activeWaiters,
                activeKitchen,
                activeBar
        );
    }

    private int countOccupiedTables() {
        List<TableSession> sessions =
                tableSessionRepository
                        .findByActiveTrue();

        return sessions.size();
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

        for (Order order : activeOrdersList) {
            for (OrderItem item : order.getItems()) {
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

        for (Order order : activeOrdersList) {
            for (OrderItem item : order.getItems()) {
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
                        || item.getProduct()
                        .getCategory() == null
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
                        || item.getProduct()
                        .getCategory() == null
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
                activeOrdersList
                        .stream()
                        .filter(
                                order ->
                                        order.getCreatedAt()
                                                != null
                        )
                        .mapToInt(
                                order ->
                                        (int) Duration
                                                .between(
                                                        order.getCreatedAt(),
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

        for (Order order : activeOrdersList) {
            for (OrderItem item : order.getItems()) {
                if (isUnfinishedItem(item)) {
                    itemCount++;
                }
            }
        }

        return itemCount;
    }

    private void applyStaffingComparison(
            DecisionRequest request,
            DecisionResponse response
    ) {
        int activeWaiters =
                request.getActiveWaiters();

        int activeKitchen =
                request.getActiveKitchen();

        int activeBar =
                request.getActiveBar();

        int waiterDeficit =
                response.getRecommendedWaiters()
                        - activeWaiters;

        int kitchenDeficit =
                response.getRecommendedKitchenStaff()
                        - activeKitchen;

        int barDeficit =
                response.getRecommendedBarStaff()
                        - activeBar;

        /*
         * Un surplus într-un rol nu anulează automat
         * deficitul din alt rol.
         */
        int totalStaffDeficit =
                Math.max(waiterDeficit, 0)
                        + Math.max(kitchenDeficit, 0)
                        + Math.max(barDeficit, 0);

        response.setActiveWaiters(
                activeWaiters
        );

        response.setActiveKitchenStaff(
                activeKitchen
        );

        response.setActiveBarStaff(
                activeBar
        );

        response.setWaiterDeficit(
                waiterDeficit
        );

        response.setKitchenDeficit(
                kitchenDeficit
        );

        response.setBarDeficit(
                barDeficit
        );

        response.setTotalStaffDeficit(
                totalStaffDeficit
        );
    }

    private DecisionResponse
    buildFallbackResponse(
            DecisionRequest request
    ) {
        DecisionResponse response =
                new DecisionResponse();

        response.setTrafficLevel(
                "NECUNOSCUT"
        );

        response.setRecommendedWaiters(
                0
        );

        response.setRecommendedKitchenStaff(
                0
        );

        response.setRecommendedBarStaff(
                0
        );

        response.setDelayRisk(
                "NECUNOSCUT"
        );

        response.setActiveWaiters(
                request.getActiveWaiters()
        );

        response.setActiveKitchenStaff(
                request.getActiveKitchen()
        );

        response.setActiveBarStaff(
                request.getActiveBar()
        );

        /*
         * Nu declarăm deficit sau surplus când
         * serviciul AI nu a returnat recomandări.
         */
        response.setWaiterDeficit(0);
        response.setKitchenDeficit(0);
        response.setBarDeficit(0);
        response.setTotalStaffDeficit(0);

        return response;
    }
}