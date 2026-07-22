package com.licenta.horeca.order.controller;

import com.licenta.horeca.order.dto.OrderStatisticsResponse;
import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.order.enums.OrderStatus;
import com.licenta.horeca.order.service.OrderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(
            @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(
                request.getSessionCode(),
                request.getItems()
        );
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @GetMapping("/active")
    public List<Order> getActiveOrders() {
        return orderService.getActiveOrders();
    }

    @GetMapping("/statistics/today")
    public OrderStatisticsResponse getTodayStatistics() {
        return orderService.getTodayStatistics();
    }

    @GetMapping("/statistics")
    public OrderStatisticsResponse getStatistics(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm")
            LocalTime startTime,

            @RequestParam(required = false)
            @DateTimeFormat(pattern = "HH:mm")
            LocalTime endTime
    ) {
        return orderService.getStatistics(
                date,
                startTime,
                endTime
        );
    }

    @GetMapping("/kitchen")
    public List<Order> getKitchenOrders() {
        return orderService.getKitchenOrders();
    }

    @GetMapping("/bar")
    public List<Order> getBarOrders() {
        return orderService.getBarOrders();
    }

    @PutMapping("/{orderId}/status")
    public Order updateOrderStatus(
            @PathVariable Long orderId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(
                orderId,
                request.getStatus()
        );
    }

    @PutMapping("/items/{itemId}/status")
    public OrderItem updateOrderItemStatus(
            @PathVariable Long itemId,
            @RequestBody UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderItemStatus(
                itemId,
                request.getStatus()
        );
    }

    public static class CreateOrderRequest {

        private String sessionCode;
        private List<OrderService.OrderItemRequest> items;

        public String getSessionCode() {
            return sessionCode;
        }

        public void setSessionCode(
                String sessionCode
        ) {
            this.sessionCode = sessionCode;
        }

        public List<OrderService.OrderItemRequest>
        getItems() {
            return items;
        }

        public void setItems(
                List<OrderService.OrderItemRequest> items
        ) {
            this.items = items;
        }
    }

    public static class UpdateOrderStatusRequest {

        private OrderStatus status;

        public OrderStatus getStatus() {
            return status;
        }

        public void setStatus(
                OrderStatus status
        ) {
            this.status = status;
        }
    }
}