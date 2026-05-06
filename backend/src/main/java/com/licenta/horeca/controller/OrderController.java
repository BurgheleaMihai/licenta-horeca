package com.licenta.horeca.controller;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.service.OrderService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@CrossOrigin(origins = "http://localhost:5173")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public Order createOrder(@RequestBody CreateOrderRequest request) {
        return orderService.createOrder(request.getItems());
    }

    @GetMapping
    public List<Order> getAllOrders() {
        return orderService.getAllOrders();
    }

    @PatchMapping("/{orderId}/status")
    public Order updateOrderStatus(
            @PathVariable Long orderId,
            @RequestParam OrderStatus status
    ) {
        return orderService.updateOrderStatus(orderId, status);
    }

    public static class CreateOrderRequest {
        private List<OrderService.OrderItemRequest> items;

        public List<OrderService.OrderItemRequest> getItems() {
            return items;
        }

        public void setItems(List<OrderService.OrderItemRequest> items) {
            this.items = items;
        }
    }
}