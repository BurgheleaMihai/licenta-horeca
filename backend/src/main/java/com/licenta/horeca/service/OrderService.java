package com.licenta.horeca.service;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.repository.OrderRepository;
import com.licenta.horeca.repository.ProductRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public OrderService(OrderRepository orderRepository,
                        ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    public Order createOrder(List<OrderItemRequest> itemRequests) {
        Order order = new Order();
        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new RuntimeException("Produsul nu exista."));

            if (!product.isAvailable()) {
                throw new RuntimeException("Produsul nu este disponibil: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();
            OrderItem orderItem = new OrderItem(product, itemRequest.getQuantity(), unitPrice);

            order.addItem(orderItem);
            totalPrice = totalPrice.add(orderItem.getSubtotal());
        }

        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.NEW);

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Comanda nu exista."));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    public static class OrderItemRequest {
        private Long productId;
        private Integer quantity;

        public Long getProductId() {
            return productId;
        }

        public void setProductId(Long productId) {
            this.productId = productId;
        }

        public Integer getQuantity() {
            return quantity;
        }

        public void setQuantity(Integer quantity) {
            this.quantity = quantity;
        }
    }
}