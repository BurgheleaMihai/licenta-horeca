package com.licenta.horeca.service;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.repository.OrderItemRepository;
import com.licenta.horeca.repository.OrderRepository;
import com.licenta.horeca.repository.ProductRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TableSessionRepository tableSessionRepository;
    private final OrderItemRepository orderItemRepository;

    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            TableSessionRepository tableSessionRepository,
            OrderItemRepository orderItemRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public Order createOrder(
            String sessionCode,
            List<OrderItemRequest> itemRequests
    ) {
        TableSession tableSession = tableSessionRepository
                .findBySessionCodeAndActiveTrue(sessionCode)
                .orElseThrow(() -> new BusinessException(
                        "Sesiunea mesei nu exista sau nu este activa."
                ));

        Order order = new Order();
        order.setTableSession(tableSession);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository
                    .findById(itemRequest.getProductId())
                    .orElseThrow(() -> new BusinessException(
                            "Produsul nu exista."
                    ));

            if (!product.isAvailable()) {
                throw new BusinessException(
                        "Produsul nu este disponibil: "
                                + product.getName()
                );
            }

            BigDecimal unitPrice = product.getPrice();

            OrderItem orderItem = new OrderItem(
                    product,
                    itemRequest.getQuantity(),
                    unitPrice
            );

            orderItem.setStatus(OrderStatus.NOUA);

            order.addItem(orderItem);

            totalPrice = totalPrice.add(
                    orderItem.getSubtotal()
            );
        }

        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.NOUA);

        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> getActiveOrders() {
        return orderRepository.findByStatusInOrderByCreatedAtAsc(
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                )
        );
    }

    public int countOrdersCreatedAfter(LocalDateTime limit) {
        long count = orderRepository.countByCreatedAtAfter(limit);

        return Math.toIntExact(count);
    }

    public List<Order> getKitchenOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(
                OrderStatus.IN_PREPARARE
        );
    }

    public List<Order> getBarOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(
                OrderStatus.IN_PREPARARE
        );
    }

    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository
                .findById(orderId)
                .orElseThrow(() -> new BusinessException(
                        "Comanda nu exista."
                ));
        order.setStatus(status);

        if (status == OrderStatus.IN_PREPARARE) {
            for (OrderItem item : order.getItems()) {
                if (item.getStatus() == OrderStatus.NOUA) {
                    item.setStatus(OrderStatus.IN_PREPARARE);
                }
            }
        }

        return orderRepository.save(order);
    }

    public OrderItem updateOrderItemStatus(
            Long orderItemId,
            OrderStatus status
    ) {
        OrderItem orderItem = orderItemRepository
                .findById(orderItemId)
                .orElseThrow(() -> new BusinessException(
                        "Produsul din comanda nu exista."
                ));

        orderItem.setStatus(status);

        OrderItem savedItem =
                orderItemRepository.save(orderItem);

        Order order = savedItem.getOrder();

        boolean allItemsReady = order.getItems()
                .stream()
                .allMatch(
                        item -> item.getStatus() == OrderStatus.GATA
                );

        if (allItemsReady) {
            order.setStatus(OrderStatus.GATA);
            orderRepository.save(order);
        }

        return savedItem;
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