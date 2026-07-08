package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);

    long countByCreatedAtAfter(LocalDateTime limit);
}