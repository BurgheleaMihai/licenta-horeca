package com.licenta.horeca;

import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.Product;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.repository.OrderRepository;
import com.licenta.horeca.repository.ProductRepository;
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

    @InjectMocks
    private OrderService orderService;

    @Test
    void createOrder_shouldCreateOrderWithCorrectTotal() {
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

        when(productRepository.findById(1L)).thenReturn(Optional.of(product1));
        when(productRepository.findById(2L)).thenReturn(Optional.of(product2));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order order = orderService.createOrder(List.of(item1, item2));

        assertEquals(OrderStatus.NEW, order.getStatus());
        assertEquals(BigDecimal.valueOf(72), order.getTotalPrice());
        assertEquals(2, order.getItems().size());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    void createOrder_shouldRejectUnavailableProduct() {
        Product product = new Product();
        product.setName("Papanasi");
        product.setPrice(BigDecimal.valueOf(24));
        product.setAvailable(false);

        OrderService.OrderItemRequest item = new OrderService.OrderItemRequest();
        item.setProductId(1L);
        item.setQuantity(1);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        RuntimeException exception = assertThrows(
                RuntimeException.class,
                () -> orderService.createOrder(List.of(item))
        );

        assertTrue(exception.getMessage().contains("Produsul nu este disponibil"));
        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void updateOrderStatus_shouldChangeStatus() {
        Order order = new Order();
        order.setStatus(OrderStatus.NEW);

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order updatedOrder = orderService.updateOrderStatus(1L, OrderStatus.READY);

        assertEquals(OrderStatus.READY, updatedOrder.getStatus());
        verify(orderRepository, times(1)).save(order);
    }
}