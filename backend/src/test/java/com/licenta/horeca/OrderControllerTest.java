package com.licenta.horeca;

import com.licenta.horeca.entity.OrderItem;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.controller.OrderController;
import com.licenta.horeca.entity.Order;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @Test
    void createOrder_shouldReturnCreatedOrder() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.createOrder(anyString(), anyList()))
                .thenReturn(order);

        Map<String, Object> request = Map.of(
                "sessionCode", "TEST123",
                "items", List.of(
                        Map.of(
                                "productId", 1,
                                "quantity", 2
                        )
                )
        );

        mockMvc.perform(post("/api/orders")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NOUA"))
                .andExpect(jsonPath("$.totalPrice").value(64));
    }

    @Test
    void updateOrderStatus_shouldReturnUpdatedOrder() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.GATA);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.GATA)))
                .thenReturn(order);

        Map<String, Object> request = Map.of(
                "status", "GATA"
        );

        mockMvc.perform(put("/api/orders/1/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GATA"))
                .andExpect(jsonPath("$.totalPrice").value(64));
    }

    @Test
    void getActiveOrders_shouldReturnActiveOrders() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getActiveOrders())
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NOUA"))
                .andExpect(jsonPath("$[0].totalPrice").value(40));
    }

    @Test
    void updateOrderItemStatus_shouldReturnUpdatedOrderItem() throws Exception {
        OrderItem item = new OrderItem();
        item.setStatus(OrderStatus.GATA);

        when(orderService.updateOrderItemStatus(eq(7L), eq(OrderStatus.GATA)))
                .thenReturn(item);

        Map<String, Object> request = Map.of(
                "status", "GATA"
        );

        mockMvc.perform(put("/api/orders/items/7/status")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("GATA"));
    }

    @Test
    void getKitchenOrders_shouldReturnOrdersInPreparation() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getKitchenOrders())
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/kitchen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_PREPARARE"))
                .andExpect(jsonPath("$[0].totalPrice").value(40));
    }

    @Test
    void getBarOrders_shouldReturnOrdersInPreparation() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getBarOrders())
                .thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/bar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("IN_PREPARARE"))
                .andExpect(jsonPath("$[0].totalPrice").value(40));
    }
}
