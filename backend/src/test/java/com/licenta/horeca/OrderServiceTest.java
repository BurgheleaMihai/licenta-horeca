package com.licenta.horeca;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.licenta.horeca.dto.OrderStatisticsResponse;
import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.exception.BusinessException;
import com.licenta.horeca.repository.OrderItemRepository;
import com.licenta.horeca.repository.OrderRepository;
import com.licenta.horeca.repository.ProductRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import com.licenta.horeca.service.FeedbackService;
import com.licenta.horeca.service.OrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    private static final String SESSION_CODE = "TEST123";
    private static final String INVALID_SESSION_CODE = "INVALID";

    private static final String PIZZA_MARGHERITA =
            "Pizza Margherita";

    private static final String STILL_WATER =
            "Apa plata";

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TableSessionRepository tableSessionRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    /*
     * Este necesar deoarece OrderService foloseste
     * FeedbackService pentru calcularea ratingului mediu.
     */
    @Mock
    private FeedbackService feedbackService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrderShouldCreateOrderWithCorrectTotal() {
        RestaurantTable table =
                new RestaurantTable(1, 4);

        TableSession tableSession =
                new TableSession(table, SESSION_CODE);

        Product product1 = new Product();
        product1.setName(PIZZA_MARGHERITA);
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName("Limonada");
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderService.OrderItemRequest item1 =
                new OrderService.OrderItemRequest();

        item1.setProductId(1L);
        item1.setQuantity(2);

        OrderService.OrderItemRequest item2 =
                new OrderService.OrderItemRequest();

        item2.setProductId(2L);
        item2.setQuantity(1);

        when(
                tableSessionRepository
                        .findBySessionCodeAndActiveTrue(
                                SESSION_CODE
                        )
        ).thenReturn(Optional.of(tableSession));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product1));

        when(productRepository.findById(2L))
                .thenReturn(Optional.of(product2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Order order =
                orderService.createOrder(
                        SESSION_CODE,
                        List.of(item1, item2)
                );

        assertEquals(
                OrderStatus.NOUA,
                order.getStatus()
        );

        assertEquals(
                BigDecimal.valueOf(72),
                order.getTotalPrice()
        );

        assertEquals(
                2,
                order.getItems().size()
        );

        assertNotNull(order.getTableSession());

        assertEquals(
                SESSION_CODE,
                order.getTableSession().getSessionCode()
        );

        assertEquals(
                1,
                order.getTableSession()
                        .getRestaurantTable()
                        .getTableNumber()
        );

        assertTrue(
                order.getItems()
                        .stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderStatus.NOUA
                        )
        );

        verify(orderRepository, times(1))
                .save(any(Order.class));
    }

    @Test
    void createOrderShouldRejectUnavailableProduct() {
        RestaurantTable table =
                new RestaurantTable(1, 4);

        TableSession tableSession =
                new TableSession(table, SESSION_CODE);

        Product product = new Product();
        product.setName("Papanasi");
        product.setPrice(BigDecimal.valueOf(24));
        product.setAvailable(false);

        OrderService.OrderItemRequest item =
                new OrderService.OrderItemRequest();

        item.setProductId(1L);
        item.setQuantity(1);

        when(
                tableSessionRepository
                        .findBySessionCodeAndActiveTrue(
                                SESSION_CODE
                        )
        ).thenReturn(Optional.of(tableSession));

        when(productRepository.findById(1L))
                .thenReturn(Optional.of(product));

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.createOrder(
                                SESSION_CODE,
                                List.of(item)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Produsul nu este disponibil"
                )
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void createOrderShouldRejectInvalidSessionCode() {
        OrderService.OrderItemRequest item =
                new OrderService.OrderItemRequest();

        item.setProductId(1L);
        item.setQuantity(1);

        when(
                tableSessionRepository
                        .findBySessionCodeAndActiveTrue(
                                INVALID_SESSION_CODE
                        )
        ).thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.createOrder(
                                INVALID_SESSION_CODE,
                                List.of(item)
                        )
                );

        assertTrue(
                exception.getMessage().contains(
                        "Sesiunea mesei nu exista"
                )
        );

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void createOrderShouldRejectMissingProduct() {
        RestaurantTable table =
                new RestaurantTable(1, 4);

        TableSession tableSession =
                new TableSession(table, SESSION_CODE);

        OrderService.OrderItemRequest item =
                new OrderService.OrderItemRequest();

        item.setProductId(99L);
        item.setQuantity(1);

        when(
                tableSessionRepository
                        .findBySessionCodeAndActiveTrue(
                                SESSION_CODE
                        )
        ).thenReturn(Optional.of(tableSession));

        when(productRepository.findById(99L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.createOrder(
                                SESSION_CODE,
                                List.of(item)
                        )
                );

        assertEquals(
                "Produsul nu exista.",
                exception.getMessage()
        );

        verify(productRepository, times(1))
                .findById(99L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void getAllOrdersShouldReturnRepositoryOrders() {
        Order firstOrder = new Order();
        firstOrder.setStatus(OrderStatus.NOUA);

        Order secondOrder = new Order();
        secondOrder.setStatus(OrderStatus.SERVITA);

        List<Order> repositoryOrders =
                List.of(firstOrder, secondOrder);

        when(orderRepository.findAll())
                .thenReturn(repositoryOrders);

        List<Order> result =
                orderService.getAllOrders();

        assertSame(
                repositoryOrders,
                result
        );

        assertEquals(
                2,
                result.size()
        );

        assertSame(
                firstOrder,
                result.get(0)
        );

        assertSame(
                secondOrder,
                result.get(1)
        );

        verify(orderRepository, times(1))
                .findAll();
    }

    @Test
    void updateOrderStatusShouldChangeStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Order updatedOrder =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.GATA
                );

        assertEquals(
                OrderStatus.GATA,
                updatedOrder.getStatus()
        );

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderStatusShouldRejectMissingOrder() {
        when(orderRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.updateOrderStatus(
                                999L,
                                OrderStatus.GATA
                        )
                );

        assertEquals(
                "Comanda nu exista.",
                exception.getMessage()
        );

        verify(orderRepository, times(1))
                .findById(999L);

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void updateOrderStatusShouldSetCompletedAtWhenOrderIsServed() {
        Order order = new Order();
        order.setStatus(OrderStatus.GATA);
        order.setCompletedAt(null);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        LocalDateTime beforeUpdate =
                LocalDateTime.now();

        Order updatedOrder =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.SERVITA
                );

        LocalDateTime afterUpdate =
                LocalDateTime.now();

        assertEquals(
                OrderStatus.SERVITA,
                updatedOrder.getStatus()
        );

        assertNotNull(
                updatedOrder.getCompletedAt()
        );

        assertFalse(
                updatedOrder
                        .getCompletedAt()
                        .isBefore(beforeUpdate)
        );

        assertFalse(
                updatedOrder
                        .getCompletedAt()
                        .isAfter(afterUpdate)
        );

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderStatusShouldKeepOriginalCompletedAtWhenAlreadyServed() {
        LocalDateTime originalCompletedAt =
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        16,
                        30
                );

        Order order = new Order();
        order.setStatus(OrderStatus.SERVITA);
        order.setCompletedAt(originalCompletedAt);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Order updatedOrder =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.SERVITA
                );

        assertEquals(
                originalCompletedAt,
                updatedOrder.getCompletedAt()
        );

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderStatusShouldClearCompletedAtWhenOrderIsReopened() {
        Order order = new Order();
        order.setStatus(OrderStatus.SERVITA);

        order.setCompletedAt(
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        16,
                        30
                )
        );

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Order updatedOrder =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.GATA
                );

        assertEquals(
                OrderStatus.GATA,
                updatedOrder.getStatus()
        );

        assertNull(
                updatedOrder.getCompletedAt()
        );

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderStatusShouldSetItemsToInPreparationWhenOrderIsSentToPreparation() {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);

        Product product1 = new Product();
        product1.setName(PIZZA_MARGHERITA);
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName(STILL_WATER);
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 =
                new OrderItem(
                        product1,
                        1,
                        BigDecimal.valueOf(32)
                );

        item1.setStatus(OrderStatus.NOUA);

        OrderItem item2 =
                new OrderItem(
                        product2,
                        1,
                        BigDecimal.valueOf(8)
                );

        item2.setStatus(OrderStatus.NOUA);

        order.addItem(item1);
        order.addItem(item2);

        when(orderRepository.findById(1L))
                .thenReturn(Optional.of(order));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        Order updatedOrder =
                orderService.updateOrderStatus(
                        1L,
                        OrderStatus.IN_PREPARARE
                );

        assertEquals(
                OrderStatus.IN_PREPARARE,
                updatedOrder.getStatus()
        );

        assertTrue(
                updatedOrder.getItems()
                        .stream()
                        .allMatch(item ->
                                item.getStatus()
                                        == OrderStatus.IN_PREPARARE
                        )
        );

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderItemStatusShouldKeepOrderInPreparationWhenNotAllItemsAreReady() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        Product product1 = new Product();
        product1.setName(PIZZA_MARGHERITA);
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName(STILL_WATER);
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 =
                new OrderItem(
                        product1,
                        1,
                        BigDecimal.valueOf(32)
                );

        item1.setStatus(OrderStatus.IN_PREPARARE);

        OrderItem item2 =
                new OrderItem(
                        product2,
                        1,
                        BigDecimal.valueOf(8)
                );

        item2.setStatus(OrderStatus.IN_PREPARARE);

        order.addItem(item1);
        order.addItem(item2);

        when(orderItemRepository.findById(1L))
                .thenReturn(Optional.of(item1));

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        OrderItem updatedItem =
                orderService.updateOrderItemStatus(
                        1L,
                        OrderStatus.GATA
                );

        assertEquals(
                OrderStatus.GATA,
                updatedItem.getStatus()
        );

        assertEquals(
                OrderStatus.IN_PREPARARE,
                order.getStatus()
        );

        verify(orderItemRepository, times(1))
                .save(item1);

        verify(orderRepository, never())
                .save(order);
    }

    @Test
    void updateOrderItemStatusShouldSetOrderReadyWhenAllItemsAreReady() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        Product product1 = new Product();
        product1.setName(PIZZA_MARGHERITA);
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName(STILL_WATER);
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 =
                new OrderItem(
                        product1,
                        1,
                        BigDecimal.valueOf(32)
                );

        item1.setStatus(OrderStatus.GATA);

        OrderItem item2 =
                new OrderItem(
                        product2,
                        1,
                        BigDecimal.valueOf(8)
                );

        item2.setStatus(OrderStatus.IN_PREPARARE);

        order.addItem(item1);
        order.addItem(item2);

        when(orderItemRepository.findById(2L))
                .thenReturn(Optional.of(item2));

        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation ->
                        invocation.getArgument(0)
                );

        OrderItem updatedItem =
                orderService.updateOrderItemStatus(
                        2L,
                        OrderStatus.GATA
                );

        assertEquals(
                OrderStatus.GATA,
                updatedItem.getStatus()
        );

        assertEquals(
                OrderStatus.GATA,
                order.getStatus()
        );

        verify(orderItemRepository, times(1))
                .save(item2);

        verify(orderRepository, times(1))
                .save(order);
    }

    @Test
    void updateOrderItemStatusShouldRejectMissingOrderItem() {
        when(orderItemRepository.findById(999L))
                .thenReturn(Optional.empty());

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.updateOrderItemStatus(
                                999L,
                                OrderStatus.GATA
                        )
                );

        assertEquals(
                "Produsul din comanda nu exista.",
                exception.getMessage()
        );

        verify(orderItemRepository, times(1))
                .findById(999L);

        verify(orderItemRepository, never())
                .save(any(OrderItem.class));

        verify(orderRepository, never())
                .save(any(Order.class));
    }

    @Test
    void getActiveOrdersShouldReturnOnlyOrdersWithActiveStatusesInFcfsOrder() {
        Order order1 = new Order();
        order1.setStatus(OrderStatus.NOUA);

        Order order2 = new Order();
        order2.setStatus(OrderStatus.IN_PREPARARE);

        Order order3 = new Order();
        order3.setStatus(OrderStatus.GATA);

        List<OrderStatus> activeStatuses =
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                );

        when(
                orderRepository
                        .findByStatusInOrderByCreatedAtAsc(
                                activeStatuses
                        )
        ).thenReturn(
                List.of(order1, order2, order3)
        );

        List<Order> activeOrders =
                orderService.getActiveOrders();

        assertEquals(
                3,
                activeOrders.size()
        );

        assertTrue(
                activeOrders.stream().anyMatch(order ->
                        order.getStatus()
                                == OrderStatus.NOUA
                )
        );

        assertTrue(
                activeOrders.stream().anyMatch(order ->
                        order.getStatus()
                                == OrderStatus.IN_PREPARARE
                )
        );

        assertTrue(
                activeOrders.stream().anyMatch(order ->
                        order.getStatus()
                                == OrderStatus.GATA
                )
        );

        assertFalse(
                activeOrders.stream().anyMatch(order ->
                        order.getStatus()
                                == OrderStatus.SERVITA
                )
        );

        assertFalse(
                activeOrders.stream().anyMatch(order ->
                        order.getStatus()
                                == OrderStatus.ANULATA
                )
        );

        verify(orderRepository, times(1))
                .findByStatusInOrderByCreatedAtAsc(
                        activeStatuses
                );
    }

    @Test
    void countOrdersCreatedAfterShouldReturnRepositoryCount() {
        LocalDateTime limit =
                LocalDateTime.of(
                        2026,
                        7,
                        13,
                        14,
                        30
                );

        when(orderRepository.countByCreatedAtAfter(limit))
                .thenReturn(4L);

        int result =
                orderService.countOrdersCreatedAfter(limit);

        assertEquals(
                4,
                result
        );

        verify(orderRepository, times(1))
                .countByCreatedAtAfter(limit);
    }

    @Test
    void getStatisticsShouldCalculateValuesForSelectedInterval() {
        LocalDate selectedDate =
                LocalDate.of(2026, 7, 11);

        LocalTime startTime =
                LocalTime.of(16, 0);

        LocalTime endTime =
                LocalTime.of(17, 0);

        LocalDateTime intervalStart =
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        16,
                        0
                );

        /*
         * OrderService adauga un minut la ora finala,
         * deci 17:00 devine limita exclusiva 17:01.
         */
        LocalDateTime intervalEndExclusive =
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        17,
                        1
                );

        Order firstOrder = new Order();
        firstOrder.setStatus(OrderStatus.SERVITA);
        firstOrder.setTotalPrice(
                new BigDecimal("30.00")
        );

        Order secondOrder = new Order();
        secondOrder.setStatus(OrderStatus.SERVITA);
        secondOrder.setTotalPrice(
                new BigDecimal("39.00")
        );

        List<OrderStatus> activeStatuses =
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                );

        when(
                orderRepository.countByStatusIn(
                        activeStatuses
                )
        ).thenReturn(2L);

        when(
                orderRepository
                        .findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
                                OrderStatus.SERVITA,
                                intervalStart,
                                intervalEndExclusive
                        )
        ).thenReturn(
                List.of(firstOrder, secondOrder)
        );

        when(
                feedbackService.getAverageRatingBetween(
                        intervalStart,
                        intervalEndExclusive
                )
        ).thenReturn(4.5);

        OrderStatisticsResponse response =
                orderService.getStatistics(
                        selectedDate,
                        startTime,
                        endTime
                );

        assertEquals(
                2,
                response.getActiveOrders()
        );

        assertEquals(
                2,
                response.getServedOrders()
        );

        assertEquals(
                new BigDecimal("69.00"),
                response.getSales()
        );

        assertEquals(
                4.5,
                response.getAverageRating(),
                0.001
        );

        verify(orderRepository, times(1))
                .countByStatusIn(activeStatuses);

        verify(orderRepository, times(1))
                .findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
                        OrderStatus.SERVITA,
                        intervalStart,
                        intervalEndExclusive
                );

        verify(feedbackService, times(1))
                .getAverageRatingBetween(
                        intervalStart,
                        intervalEndExclusive
                );
    }

    @Test
    void getStatisticsShouldIgnoreOrdersWithNullTotalPrice() {
        LocalDate selectedDate =
                LocalDate.of(2026, 7, 12);

        LocalTime startTime =
                LocalTime.of(12, 0);

        LocalTime endTime =
                LocalTime.of(13, 0);

        LocalDateTime intervalStart =
                LocalDateTime.of(
                        2026,
                        7,
                        12,
                        12,
                        0
                );

        LocalDateTime intervalEndExclusive =
                LocalDateTime.of(
                        2026,
                        7,
                        12,
                        13,
                        1
                );

        Order orderWithPrice = new Order();
        orderWithPrice.setStatus(OrderStatus.SERVITA);

        orderWithPrice.setTotalPrice(
                new BigDecimal("45.00")
        );

        Order orderWithoutPrice = new Order();
        orderWithoutPrice.setStatus(OrderStatus.SERVITA);
        orderWithoutPrice.setTotalPrice(null);

        List<OrderStatus> activeStatuses =
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                );

        when(
                orderRepository.countByStatusIn(
                        activeStatuses
                )
        ).thenReturn(1L);

        when(
                orderRepository
                        .findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
                                OrderStatus.SERVITA,
                                intervalStart,
                                intervalEndExclusive
                        )
        ).thenReturn(
                List.of(
                        orderWithPrice,
                        orderWithoutPrice
                )
        );

        when(
                feedbackService.getAverageRatingBetween(
                        intervalStart,
                        intervalEndExclusive
                )
        ).thenReturn(4.0);

        OrderStatisticsResponse response =
                orderService.getStatistics(
                        selectedDate,
                        startTime,
                        endTime
                );

        assertEquals(
                1,
                response.getActiveOrders()
        );

        /*
         * Ambele comenzi sunt servite, chiar daca una
         * nu are pretul total salvat.
         */
        assertEquals(
                2,
                response.getServedOrders()
        );

        /*
         * Comanda cu totalPrice null trebuie ignorata
         * la calculul vanzarilor.
         */
        assertEquals(
                new BigDecimal("45.00"),
                response.getSales()
        );

        assertEquals(
                4.0,
                response.getAverageRating(),
                0.001
        );
    }

    @Test
    void getTodayStatisticsShouldUseCurrentDayInterval() {
        LocalDate today = LocalDate.now();

        LocalDateTime startOfDay =
                today.atStartOfDay();

        LocalDateTime startOfNextDay =
                today.plusDays(1).atStartOfDay();

        List<OrderStatus> activeStatuses =
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                );

        Order servedOrder = new Order();
        servedOrder.setStatus(OrderStatus.SERVITA);

        servedOrder.setTotalPrice(
                new BigDecimal("69.00")
        );

        when(
                orderRepository.countByStatusIn(
                        activeStatuses
                )
        ).thenReturn(0L);

        when(
                orderRepository
                        .findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
                                OrderStatus.SERVITA,
                                startOfDay,
                                startOfNextDay
                        )
        ).thenReturn(List.of(servedOrder));

        when(
                feedbackService.getAverageRatingBetween(
                        startOfDay,
                        startOfNextDay
                )
        ).thenReturn(5.0);

        OrderStatisticsResponse response =
                orderService.getTodayStatistics();

        assertEquals(
                0,
                response.getActiveOrders()
        );

        assertEquals(
                1,
                response.getServedOrders()
        );

        assertEquals(
                new BigDecimal("69.00"),
                response.getSales()
        );

        assertEquals(
                5.0,
                response.getAverageRating(),
                0.001
        );
    }

    @Test
    void getStatisticsShouldReturnZeroWhenNoOrdersOrFeedbackExist() {
        LocalDate selectedDate =
                LocalDate.of(2026, 7, 11);

        LocalTime startTime =
                LocalTime.of(10, 0);

        LocalTime endTime =
                LocalTime.of(11, 0);

        LocalDateTime intervalStart =
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        10,
                        0
                );

        LocalDateTime intervalEndExclusive =
                LocalDateTime.of(
                        2026,
                        7,
                        11,
                        11,
                        1
                );

        List<OrderStatus> activeStatuses =
                List.of(
                        OrderStatus.NOUA,
                        OrderStatus.IN_PREPARARE,
                        OrderStatus.GATA
                );

        when(
                orderRepository.countByStatusIn(
                        activeStatuses
                )
        ).thenReturn(0L);

        when(
                orderRepository
                        .findByStatusAndCompletedAtGreaterThanEqualAndCompletedAtLessThanOrderByCompletedAtAsc(
                                OrderStatus.SERVITA,
                                intervalStart,
                                intervalEndExclusive
                        )
        ).thenReturn(List.of());

        when(
                feedbackService.getAverageRatingBetween(
                        intervalStart,
                        intervalEndExclusive
                )
        ).thenReturn(0.0);

        OrderStatisticsResponse response =
                orderService.getStatistics(
                        selectedDate,
                        startTime,
                        endTime
                );

        assertEquals(
                0,
                response.getActiveOrders()
        );

        assertEquals(
                0,
                response.getServedOrders()
        );

        assertEquals(
                BigDecimal.ZERO,
                response.getSales()
        );

        assertEquals(
                0.0,
                response.getAverageRating(),
                0.001
        );
    }

    @Test
    void getStatisticsShouldRejectInvalidTimeInterval() {
        LocalDate selectedDate =
                LocalDate.of(2026, 7, 11);

        LocalTime startTime =
                LocalTime.of(17, 0);

        LocalTime endTime =
                LocalTime.of(16, 0);

        BusinessException exception =
                assertThrows(
                        BusinessException.class,
                        () -> orderService.getStatistics(
                                selectedDate,
                                startTime,
                                endTime
                        )
                );

        assertEquals(
                "Ora de sfarsit trebuie sa fie dupa ora de inceput.",
                exception.getMessage()
        );

        verifyNoInteractions(
                orderRepository,
                feedbackService
        );
    }

    @Test
    void getKitchenOrdersShouldReturnOrdersInPreparationInFcfsOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        when(
                orderRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OrderStatus.IN_PREPARARE
                        )
        ).thenReturn(List.of(order));

        List<Order> result =
                orderService.getKitchenOrders();

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                OrderStatus.IN_PREPARARE,
                result.get(0).getStatus()
        );

        verify(orderRepository, times(1))
                .findByStatusOrderByCreatedAtAsc(
                        OrderStatus.IN_PREPARARE
                );
    }

    @Test
    void getBarOrdersShouldReturnOrdersInPreparationInFcfsOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        when(
                orderRepository
                        .findByStatusOrderByCreatedAtAsc(
                                OrderStatus.IN_PREPARARE
                        )
        ).thenReturn(List.of(order));

        List<Order> result =
                orderService.getBarOrders();

        assertEquals(
                1,
                result.size()
        );

        assertEquals(
                OrderStatus.IN_PREPARARE,
                result.get(0).getStatus()
        );

        verify(orderRepository, times(1))
                .findByStatusOrderByCreatedAtAsc(
                        OrderStatus.IN_PREPARARE
                );
    }
}