package com.licenta.horeca;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.entity.RestaurantTable;
import com.licenta.horeca.entity.TableSession;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.repository.OrderItemRepository;
import com.licenta.horeca.repository.OrderRepository;
import com.licenta.horeca.repository.ProductRepository;
import com.licenta.horeca.repository.TableSessionRepository;
import com.licenta.horeca.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private TableSessionRepository tableSessionRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldCreateOrderWithCorrectTotal() {
        RestaurantTable table = new RestaurantTable(1, 4);
        TableSession tableSession = new TableSession(table, "TEST123");

        Product product1 = new Product();
        product1.setName("Pizza Margherita");
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName("Limonada");
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderService.OrderItemRequest item1 = new OrderService.OrderItemRequest();
        item1.setProductId(1L);
        item1.setQuantity(2);

        OrderService.OrderItemRequest item2 = new OrderService.OrderItemRequest();
        item2.setProductId(2L);
        item2.setQuantity(1);

        when(tableSessionRepository.findBySessionCodeAndActiveTrue("TEST123"))
                .thenReturn(Optional.of(tableSession));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder("TEST123", List.of(item1, item2));

        assertEquals(OrderStatus.NOUA, order.getStatus());
        assertEquals(BigDecimal.valueOf(72), order.getTotalPrice());
        assertEquals(2, order.getItems().size());

        assertNotNull(order.getTableSession());
        assertEquals("TEST123", order.getTableSession().getSessionCode());
        assertEquals(1, order.getTableSession().getRestaurantTable().getTableNumber());

        assertTrue(
                order.getItems().stream()
                        .allMatch(item -> item.getStatus() == OrderStatus.NOUA)
        );

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectUnavailableProduct() {
        RestaurantTable table = new RestaurantTable(1, 4);
        TableSession tableSession = new TableSession(table, "TEST123");

        Product product = new Product();
        product.setName("Papanasi");
        product.setPrice(BigDecimal.valueOf(24));
        product.setAvailable(false);

        OrderService.OrderItemRequest item = new OrderService.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        when(tableSessionRepository.findBySessionCodeAndActiveTrue("TEST123"))
                .thenReturn(Optional.of(tableSession));

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder("TEST123", List.of(item))
        );

        assertTrue(exception.getMessage().contains("Produsul nu este disponibil"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectInvalidSessionCode() {
        OrderService.OrderItemRequest item = new OrderService.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        when(tableSessionRepository.findBySessionCodeAndActiveTrue("INVALID"))
                .thenReturn(Optional.empty());

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder("INVALID", List.of(item))
        );

        assertTrue(exception.getMessage().contains("Sesiunea mesei nu exista"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_shouldChangeStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateOrderStatus(1L, OrderStatus.GATA);

        assertEquals(OrderStatus.GATA, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderStatus_shouldSetItemsToInPreparationWhenOrderIsSentToPreparation() {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);

        Product product1 = new Product();
        product1.setName("Pizza Margherita");
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName("Apa plata");
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 = new OrderItem(product1, 1, BigDecimal.valueOf(32));
        item1.setStatus(OrderStatus.NOUA);

        OrderItem item2 = new OrderItem(product2, 1, BigDecimal.valueOf(8));
        item2.setStatus(OrderStatus.NOUA);

        order.addItem(item1);
        order.addItem(item2);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateOrderStatus(1L, OrderStatus.IN_PREPARARE);

        assertEquals(OrderStatus.IN_PREPARARE, updatedOrder.getStatus());

        assertTrue(
                updatedOrder.getItems().stream()
                        .allMatch(item -> item.getStatus() == OrderStatus.IN_PREPARARE)
        );

        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void updateOrderItemStatus_shouldKeepOrderInPreparationWhenNotAllItemsAreReady() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        Product product1 = new Product();
        product1.setName("Pizza Margherita");
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName("Apa plata");
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 = new OrderItem(product1, 1, BigDecimal.valueOf(32));
        item1.setStatus(OrderStatus.IN_PREPARARE);

        OrderItem item2 = new OrderItem(product2, 1, BigDecimal.valueOf(8));
        item2.setStatus(OrderStatus.IN_PREPARARE);

        order.addItem(item1);
        order.addItem(item2);

        when(orderItemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem updatedItem = orderService.updateOrderItemStatus(1L, OrderStatus.GATA);

        assertEquals(OrderStatus.GATA, updatedItem.getStatus());
        assertEquals(OrderStatus.IN_PREPARARE, order.getStatus());

        verify(orderItemRepository, times(1)).save(item1);
        verify(orderRepository, never()).save(order);
    }

    @Test
    void updateOrderItemStatus_shouldSetOrderReadyWhenAllItemsAreReady() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        Product product1 = new Product();
        product1.setName("Pizza Margherita");
        product1.setPrice(BigDecimal.valueOf(32));
        product1.setAvailable(true);

        Product product2 = new Product();
        product2.setName("Apa plata");
        product2.setPrice(BigDecimal.valueOf(8));
        product2.setAvailable(true);

        OrderItem item1 = new OrderItem(product1, 1, BigDecimal.valueOf(32));
        item1.setStatus(OrderStatus.GATA);

        OrderItem item2 = new OrderItem(product2, 1, BigDecimal.valueOf(8));
        item2.setStatus(OrderStatus.IN_PREPARARE);

        order.addItem(item1);
        order.addItem(item2);

        when(orderItemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(orderItemRepository.save(any(OrderItem.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        OrderItem updatedItem = orderService.updateOrderItemStatus(2L, OrderStatus.GATA);

        assertEquals(OrderStatus.GATA, updatedItem.getStatus());
        assertEquals(OrderStatus.GATA, order.getStatus());

        verify(orderItemRepository, times(1)).save(item2);
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    void getActiveOrders_shouldReturnOnlyOrdersWithActiveStatusesInFcfsOrder() {
        Order order1 = new Order();
        order1.setStatus(OrderStatus.NOUA);

        Order order2 = new Order();
        order2.setStatus(OrderStatus.IN_PREPARARE);

        Order order3 = new Order();
        order3.setStatus(OrderStatus.GATA);

        List<OrderStatus> activeStatuses = List.of(
                OrderStatus.NOUA,
                OrderStatus.IN_PREPARARE,
                OrderStatus.GATA
        );

        when(orderRepository.findByStatusInOrderByCreatedAtAsc(activeStatuses))
                .thenReturn(List.of(order1, order2, order3));

        List<Order> activeOrders = orderService.getActiveOrders();

        assertEquals(3, activeOrders.size());

        assertTrue(activeOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.NOUA));

        assertTrue(activeOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.IN_PREPARARE));

        assertTrue(activeOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.GATA));

        assertFalse(activeOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.SERVITA));

        assertFalse(activeOrders.stream()
                .anyMatch(order -> order.getStatus() == OrderStatus.ANULATA));

        verify(orderRepository, times(1))
                .findByStatusInOrderByCreatedAtAsc(activeStatuses);
    }

    @Test
    void getKitchenOrders_shouldReturnOrdersInPreparationInFcfsOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE))
                .thenReturn(List.of(order));

        List<Order> result = orderService.getKitchenOrders();

        assertEquals(1, result.size());
        assertEquals(OrderStatus.IN_PREPARARE, result.get(0).getStatus());

        verify(orderRepository, times(1))
                .findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE);
    }

    @Test
    void getBarOrders_shouldReturnOrdersInPreparationInFcfsOrder() {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);

        when(orderRepository.findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE))
                .thenReturn(List.of(order));

        List<Order> result = orderService.getBarOrders();

        assertEquals(1, result.size());
        assertEquals(OrderStatus.IN_PREPARARE, result.get(0).getStatus());

        verify(orderRepository, times(1))
                .findByStatusOrderByCreatedAtAsc(OrderStatus.IN_PREPARARE);
    }
}