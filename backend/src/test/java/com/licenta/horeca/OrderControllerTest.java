package com.licenta.horeca;

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

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        order.setStatus(OrderStatus.NEW);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.createOrder(anyList())).thenReturn(order);

        Map<String, Object> request = Map.of(
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
                .andExpect(jsonPath("$.status").value("NEW"))
                .andExpect(jsonPath("$.totalPrice").value(64));
    }

    @Test
    void updateOrderStatus_shouldReturnUpdatedOrder() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.READY);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.READY)))
                .thenReturn(order);

        mockMvc.perform(patch("/api/orders/1/status")
                        .param("status", "READY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"))
                .andExpect(jsonPath("$.totalPrice").value(64));
    }
}