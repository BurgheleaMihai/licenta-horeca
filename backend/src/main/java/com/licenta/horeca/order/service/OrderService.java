package com.licenta.horeca.order.service;

import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.feedback.service.FeedbackService;
import com.licenta.horeca.order.dto.OrderStatisticsResponse;
import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.order.enums.OrderStatus;
import com.licenta.horeca.order.repository.OrderItemRepository;
import com.licenta.horeca.order.repository.OrderRepository;
import com.licenta.horeca.product.entity.Product;
import com.licenta.horeca.product.repository.ProductRepository;
import com.licenta.horeca.table.entity.TableSession;
import com.licenta.horeca.table.repository.TableSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Gestionează ciclul de viață al comenzilor:
 * creare, trimitere la preparare, actualizarea produselor,
 * servire și calcularea statisticilor.
 * <p>
 * Tranzacțiile de citire păstrează sesiunea Hibernate activă pe
 * durata logicii serviciului. OrderRepository încarcă explicit și
 * colecția items, astfel încât răspunsurile pot fi serializate chiar
 * dacă spring.jpa.open-in-view este dezactivat.
 */
@Service
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final TableSessionRepository tableSessionRepository;
    private final OrderItemRepository orderItemRepository;
    private final FeedbackService feedbackService;

    public OrderService(OrderRepository orderRepository, ProductRepository productRepository, TableSessionRepository tableSessionRepository, OrderItemRepository orderItemRepository, FeedbackService feedbackService) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.tableSessionRepository = tableSessionRepository;
        this.orderItemRepository = orderItemRepository;
        this.feedbackService = feedbackService;
    }

    /**
     * Creează o comandă pentru o sesiune activă și calculează
     * valoarea totală folosind prețurile curente ale produselor.
     */
    @Transactional
    public Order createOrder(String sessionCode, List<OrderItemRequest> itemRequests) {
        TableSession tableSession = tableSessionRepository.findBySessionCodeAndActiveTrue(sessionCode).orElseThrow(() -> new BusinessException("Sesiunea mesei nu exista sau nu este activa."));

        Order order = new Order();
        order.setTableSession(tableSession);

        BigDecimal totalPrice = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : itemRequests) {
            Product product = productRepository.findById(itemRequest.getProductId()).orElseThrow(() -> new BusinessException("Produsul nu exista."));

            if (!product.isAvailable()) {
                throw new BusinessException("Produsul nu este disponibil: " + product.getName());
            }

            BigDecimal unitPrice = product.getPrice();

            OrderItem orderItem = new OrderItem(product, itemRequest.getQuantity(), unitPrice);

            orderItem.setStatus(OrderStatus.NOUA);
            order.addItem(orderItem);

            totalPrice = totalPrice.add(orderItem.getSubtotal());
        }

        order.setTotalPrice(totalPrice);
        order.setStatus(OrderStatus.NOUA);

        return orderRepository.save(order);
    }

    /**
     * Returnează comenzile împreună cu produsele, categoria și masa.
     * Detaliile sunt încărcate explicit de OrderRepository.
     */
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    /**
     * Returnează comenzile active în ordinea creării lor.
     */
    public List<Order> getActiveOrders() {
        return orderRepository.findByStatusInOrderByCreatedAtAsc(getActiveStatuses());
    }

    public OrderStatisticsResponse getTodayStatistics() {
        return getStatistics(LocalDate.now(), LocalTime.of(0, 0), LocalTime.of(23, 59));
    }

    public OrderStatisticsResponse getStatistics(LocalDate date, LocalTime startTime, LocalTime endTime) {
        LocalDate selectedDate = date == null ? LocalDate.now() : date;

        LocalTime selectedStartTime = startTime == null ? LocalTime.of(0, 0) : startTime;

        LocalTime selectedEndTime = endTime == null ? LocalTime.of(23, 59) : endTime;

        if (selectedEndTime.isBefore(selectedStartTime)) {
            throw new BusinessException("Ora de sfarsit trebuie sa fie dupa ora de inceput.");
        }

        LocalDateTime intervalStart = selectedDate.atTime(selectedStartTime);

        /*
         * Adăugăm un minut deoarece inputurile HTML sunt introduse
         * cu precizie de minut. Astfel, ora de sfârșit este inclusă.
         *
         * Exemplu: 12:00 - 15:00 include și comenzile sau
         * feedbackurile din minutul 15:00.
         */
        LocalDateTime intervalEndExclusive = selectedDate.atTime(selectedEndTime).plusMinutes(1);

        long activeOrdersCount = orderRepository.countByStatusIn(getActiveStatuses());

        List<Order> servedOrders = orderRepository.findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(OrderStatus.SERVITA, intervalStart, intervalEndExclusive);

        BigDecimal sales = servedOrders.stream().map(Order::getTotalPrice).filter(totalPrice -> totalPrice != null).reduce(BigDecimal.ZERO, BigDecimal::add);

        double averageRating = feedbackService.getAverageRatingBetween(intervalStart, intervalEndExclusive);

        return new OrderStatisticsResponse(Math.toIntExact(activeOrdersCount), servedOrders.size(), sales, averageRating);
    }

    private List<OrderStatus> getActiveStatuses() {
        return List.of(OrderStatus.NOUA, OrderStatus.IN_PREPARARE, OrderStatus.GATA);
    }

    public int countOrdersCreatedAfter(LocalDateTime limit) {
        long count = orderRepository.countByCreatedAtAfter(limit);

        return Math.toIntExact(count);
    }

    /**
     * Bucătăria și barul primesc aceleași comenzi aflate în
     * preparare, iar frontend-ul filtrează produsele după categorie.
     */
    public List<Order> getKitchenOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE);
    }

    public List<Order> getBarOrders() {
        return orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE);
    }

    /**
     * Actualizează starea generală a comenzii.
     * <p>
     * La trimiterea în preparare, produsele noi sunt mutate în aceeași
     * stare. La servire se memorează momentul finalizării.
     */
    @Transactional
    public Order updateOrderStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BusinessException("Comanda nu exista."));

        order.setStatus(status);

        if (status == OrderStatus.IN_PREPARARE) {
            for (OrderItem item : order.getItems()) {
                if (item.getStatus() == OrderStatus.NOUA) {
                    item.setStatus(OrderStatus.IN_PREPARARE);
                }
            }
        }

        if (status == OrderStatus.SERVITA) {
            if (order.getCompletedAt() == null) {
                order.setCompletedAt(LocalDateTime.now());
            }
        } else {
            order.setCompletedAt(null);
        }

        return orderRepository.save(order);
    }

    /**
     * Actualizează un produs din comandă și marchează automat
     * întreaga comandă ca GATA atunci când toate produsele sunt gata.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public OrderItem updateOrderItemStatus(Long orderItemId, OrderStatus status) {
        /*
         * Blocarea comenzii trebuie să fie prima operație efectuată
         * în cadrul tranzacției.
         */
        Order order = orderRepository.findOrderByItemIdForUpdate(orderItemId).orElseThrow(() -> new BusinessException("Produsul din comanda nu exista."));

        /*
         * Itemul este citit numai după obținerea blocării comenzii.
         */
        OrderItem orderItem = orderItemRepository.findById(orderItemId).orElseThrow(() -> new BusinessException("Produsul din comanda nu exista."));

        orderItem.setStatus(status);

        /*
         * Modificarea itemului este trimisă în baza de date înainte
         * de verificarea tuturor itemelor comenzii.
         */
        orderItemRepository.flush();

        boolean hasItemsNotReady = orderItemRepository.existsByOrder_IdAndStatusNot(order.getId(), OrderStatus.GATA);

        if (hasItemsNotReady) {
            order.setStatus(OrderStatus.IN_PREPARARE);
        } else {
            order.setStatus(OrderStatus.GATA);
        }

        /*
         * Order este deja o entitate administrată de Hibernate.
         * Este suficient flush-ul, fără un nou save/merge.
         */
        orderRepository.flush();

        return orderItem;
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