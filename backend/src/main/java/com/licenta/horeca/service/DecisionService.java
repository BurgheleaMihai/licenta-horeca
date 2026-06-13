package com.licenta.horeca.service;

import com.licenta.horeca.dto.DecisionRequest;
import com.licenta.horeca.dto.DecisionResponse;
import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class DecisionService {

    private static final String AI_SERVICE_URL = "http://localhost:5000/predict/all";

    private final RestTemplate restTemplate;
    private final OrderService orderService;
    private final TableSessionRepository tableSessionRepository;

    public DecisionService(OrderService orderService,
                           TableSessionRepository tableSessionRepository) {

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000);
        requestFactory.setReadTimeout(3000);

        this.restTemplate = new RestTemplate(requestFactory);
        this.orderService = orderService;
        this.tableSessionRepository = tableSessionRepository;
    }

    public DecisionResponse getDecisionSummary() {
        System.out.println("=== AM INTRAT IN DECISION SERVICE ===");

        DecisionRequest request = buildCurrentDecisionRequest();

        System.out.println("=== TRIMIT REQUEST CATRE AI SERVICE ===");

        try {
            DecisionResponse response = restTemplate.postForObject(
                    AI_SERVICE_URL,
                    request,
                    DecisionResponse.class
            );

            System.out.println("=== RASPUNS PRIMIT DE LA AI SERVICE ===");

            return response;
        } catch (RestClientException exception) {
            System.out.println("=== AI SERVICE NU A RASPUNS ===");
            System.out.println(exception.getMessage());

            return buildFallbackResponse();
        }
    }

    private DecisionRequest buildCurrentDecisionRequest() {
        System.out.println("=== INCEP SA CONSTRUIESC DATELE PENTRU AI ===");

        LocalDateTime now = LocalDateTime.now();

        int dayOfWeek = now.getDayOfWeek().getValue() - 1;
        int hour = now.getHour();

        System.out.println("Citesc comenzile active...");
        List<Order> activeOrdersList = orderService.getActiveOrders();
        System.out.println("Comenzi active citite: " + activeOrdersList.size());

        /*
         * Pentru sistemul de decizie ne intereseaza starea curenta.
         * De aceea, pentru moment, folosim comenzile active si pentru calculul
         * comenzilor din ultimele 30 de minute.
         */
        List<Order> allOrders = activeOrdersList;

        System.out.println("Calculez activeOrders...");
        int activeOrders = activeOrdersList.size();

        System.out.println("Calculez occupiedTables...");
        int occupiedTables = countOccupiedTables();

        System.out.println("Calculez estimatedOccupancy...");
        int estimatedOccupancy = calculateEstimatedOccupancy(occupiedTables);

        System.out.println("Calculez kitchenLoad...");
        int kitchenLoad = calculateKitchenLoad(activeOrdersList);

        System.out.println("Calculez barLoad...");
        int barLoad = calculateBarLoad(activeOrdersList);

        System.out.println("Setez avgPreparationTime...");
        int avgPreparationTime = 20;

        System.out.println("Calculez ordersLast30Min...");
        int ordersLast30Min = countOrdersLast30Minutes(allOrders, now);

        System.out.println("Calculez orderAgeMinutes...");
        int orderAgeMinutes = calculateOldestActiveOrderAge(activeOrdersList, now);

        System.out.println("Calculez itemCount...");
        int itemCount = calculateUnfinishedItemCount(activeOrdersList);

        System.out.println("=== DATE TRIMISE CATRE AI SERVICE ===");
        System.out.println("dayOfWeek = " + dayOfWeek);
        System.out.println("hour = " + hour);
        System.out.println("activeOrders = " + activeOrders);
        System.out.println("occupiedTables = " + occupiedTables);
        System.out.println("estimatedOccupancy = " + estimatedOccupancy);
        System.out.println("kitchenLoad = " + kitchenLoad);
        System.out.println("barLoad = " + barLoad);
        System.out.println("avgPreparationTime = " + avgPreparationTime);
        System.out.println("ordersLast30Min = " + ordersLast30Min);
        System.out.println("orderAgeMinutes = " + orderAgeMinutes);
        System.out.println("itemCount = " + itemCount);
        System.out.println("=====================================");

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
        System.out.println("Citesc sesiunile active ale meselor...");

        List<TableSession> sessions = tableSessionRepository.findAll();

        int occupiedTables = (int) sessions.stream()
                .filter(TableSession::isActive)
                .count();

        System.out.println("Mese ocupate: " + occupiedTables);

        return occupiedTables;
    }

    private int calculateEstimatedOccupancy(int occupiedTables) {
        int totalTables = 12;

        if (totalTables == 0) {
            return 0;
        }

        return Math.min(100, (occupiedTables * 100) / totalTables);
    }

    private int calculateKitchenLoad(List<Order> activeOrdersList) {
        int kitchenLoad = 0;

        for (Order order : activeOrdersList) {
            for (OrderItem item : order.getItems()) {
                if (isUnfinishedItem(item) && isKitchenItem(item)) {
                    kitchenLoad++;
                }
            }
        }

        return kitchenLoad;
    }

    private int calculateBarLoad(List<Order> activeOrdersList) {
        int barLoad = 0;

        for (Order order : activeOrdersList) {
            for (OrderItem item : order.getItems()) {
                if (isUnfinishedItem(item) && isBarItem(item)) {
                    barLoad++;
                }
            }
        }

        return barLoad;
    }

    private boolean isUnfinishedItem(OrderItem item) {
        return item.getStatus() != OrderStatus.GATA;
    }

    private boolean isKitchenItem(OrderItem item) {
        if (item.getProduct() == null || item.getProduct().getCategory() == null) {
            return false;
        }

        String categoryName = item.getProduct()
                .getCategory()
                .getName();

        return !categoryName.equalsIgnoreCase("Bauturi");
    }

    private boolean isBarItem(OrderItem item) {
        if (item.getProduct() == null || item.getProduct().getCategory() == null) {
            return false;
        }

        String categoryName = item.getProduct()
                .getCategory()
                .getName();

        return categoryName.equalsIgnoreCase("Bauturi");
    }

    private int countOrdersLast30Minutes(List<Order> allOrders, LocalDateTime now) {
        LocalDateTime limit = now.minusMinutes(30);

        return (int) allOrders.stream()
                .filter(order -> order.getCreatedAt() != null)
                .filter(order -> order.getCreatedAt().isAfter(limit))
                .count();
    }

    private int calculateOldestActiveOrderAge(List<Order> activeOrdersList, LocalDateTime now) {
        int age = activeOrdersList.stream()
                .filter(order -> order.getCreatedAt() != null)
                .mapToInt(order -> (int) Duration.between(order.getCreatedAt(), now).toMinutes())
                .max()
                .orElse(0);

        return Math.min(age, 40);
    }

    private int calculateUnfinishedItemCount(List<Order> activeOrdersList) {
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

    private DecisionResponse buildFallbackResponse() {
        DecisionResponse response = new DecisionResponse();

        response.setTrafficLevel("NECUNOSCUT");
        response.setRecommendedWaiters(0);
        response.setRecommendedKitchenStaff(0);
        response.setRecommendedBarStaff(0);
        response.setDelayRisk("NECUNOSCUT");

        return response;
    }
}