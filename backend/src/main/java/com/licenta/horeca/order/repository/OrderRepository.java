package com.licenta.horeca.order.repository;

import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.enums.OrderStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /*
     * Încarcă toate datele necesare afișării unei comenzi înainte
     * de închiderea sesiunii Hibernate.
     *
     * Aplicația păstrează spring.jpa.open-in-view=false, iar această
     * configurație previne LazyInitializationException pentru items.
     */
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "tableSession", "tableSession.restaurantTable"})
    List<Order> findAll();

    /*
     * Blochează comanda asociată unui item înainte ca itemul să fie
     * citit și actualizat.
     *
     * Astfel, două actualizări simultane pentru aceeași comandă sunt
     * executate pe rând.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT o
            FROM OrderItem oi
            JOIN oi.order o
            WHERE oi.id = :orderItemId
            """)
    Optional<Order> findOrderByItemIdForUpdate(@Param("orderItemId") Long orderItemId);

    /*
     * Este folosită și la actualizarea statusului unei comenzi.
     * Elementele trebuie încărcate deoarece pot fi actualizate odată
     * cu trecerea comenzii în starea IN_PREPARARE.
     */
    @Override
    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "tableSession", "tableSession.restaurantTable"})
    Optional<Order> findById(Long orderId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByStatusIn(List<OrderStatus> statuses);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "tableSession", "tableSession.restaurantTable"})
    List<Order> findByStatusOrderByCreatedAtAsc(OrderStatus status);

    @EntityGraph(attributePaths = {"items", "items.product", "items.product.category", "tableSession", "tableSession.restaurantTable"})
    List<Order> findByStatusInOrderByCreatedAtAsc(List<OrderStatus> statuses);

    long countByStatusIn(List<OrderStatus> statuses);

    long countByCreatedAtAfter(LocalDateTime limit);

    List<Order> findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(OrderStatus status, LocalDateTime start, LocalDateTime end);
}