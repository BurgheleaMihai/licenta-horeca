package com.licenta.horeca.order.repository;

import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.order.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    /*
     * Verifică dacă mai există cel puțin un produs al comenzii
     * care nu a ajuns în statusul GATA.
     */
    boolean existsByOrder_IdAndStatusNot(Long orderId, OrderStatus status);
}