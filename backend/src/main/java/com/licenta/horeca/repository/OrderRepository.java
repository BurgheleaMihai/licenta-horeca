package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    List<Order> findByStatusInOrderByCreatedAtAsc(
            List<OrderStatus> statuses
    );

    long countByCreatedAtAfter(LocalDateTime limit);
}