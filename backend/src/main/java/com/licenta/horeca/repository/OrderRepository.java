package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface OrderRepository
        extends JpaRepository<Order, Long> {

    /*
     * Încarcă toate datele necesare afișării unei comenzi înainte
     * de închiderea sesiunii Hibernate.
     *
     * Aplicația păstrează spring.jpa.open-in-view=false, iar această
     * configurație previne LazyInitializationException pentru items.
     */
    @Override
    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.product",
                    "items.product.category",
                    "tableSession",
                    "tableSession.restaurantTable"
            }
    )
    List<Order> findAll();

    /*
     * Este folosită și la actualizarea statusului unei comenzi.
     * Elementele trebuie încărcate deoarece pot fi actualizate odată
     * cu trecerea comenzii în starea IN_PREPARARE.
     */
    @Override
    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.product",
                    "items.product.category",
                    "tableSession",
                    "tableSession.restaurantTable"
            }
    )
    Optional<Order> findById(Long orderId);

    List<Order> findByStatus(
            OrderStatus status
    );

    List<Order> findByStatusIn(
            List<OrderStatus> statuses
    );

    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.product",
                    "items.product.category",
                    "tableSession",
                    "tableSession.restaurantTable"
            }
    )
    List<Order> findByStatusOrderByCreatedAtAsc(
            OrderStatus status
    );

    @EntityGraph(
            attributePaths = {
                    "items",
                    "items.product",
                    "items.product.category",
                    "tableSession",
                    "tableSession.restaurantTable"
            }
    )
    List<Order> findByStatusInOrderByCreatedAtAsc(
            List<OrderStatus> statuses
    );

    long countByStatusIn(
            List<OrderStatus> statuses
    );

    long countByCreatedAtAfter(
            LocalDateTime limit
    );

    List<Order>
    findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
            OrderStatus status,
            LocalDateTime start,
            LocalDateTime end
    );
}