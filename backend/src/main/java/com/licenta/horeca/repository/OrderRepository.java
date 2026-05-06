package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByStatus(OrderStatus status);
}
