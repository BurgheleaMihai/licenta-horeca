package com.licenta.horeca;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.controller.OrderController;
import com.licenta.horeca.entity.Order;
import com.licenta.horeca.entity.OrderItem;
import com.licenta.horeca.enums.OrderStatus;
import com.licenta.horeca.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(OrderController.class)
class OrderControllerTest {
    private static final String STATUS_JSON_PATH = "$.status";
    private static final String TOTAL_PRICE_JSON_PATH = "$.totalPrice";
    private static final String FIRST_STATUS_JSON_PATH = "$[0].status";
    private static final String FIRST_TOTAL_PRICE_JSON_PATH = "$[0].totalPrice";

    private static final String NOUA_STATUS = "NOUA";
    private static final String GATA_STATUS = "GATA";
    private static final String IN_PREPARARE_STATUS = "IN_PREPARARE";

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private OrderService orderService;

    @Test
    void createOrderShouldReturnCreatedOrder() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.createOrder(anyString(), anyList())).thenReturn(order);

        Map<String, Object> request = Map.of("sessionCode", "TEST123", "items",
                List.of(Map.of("productId", 1, "quantity", 2)));

        mockMvc
                .perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value(NOUA_STATUS))
                .andExpect(jsonPath(TOTAL_PRICE_JSON_PATH).value(64));
    }

    @Test
    void updateOrderStatusShouldReturnUpdatedOrder() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.GATA);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.updateOrderStatus(eq(1L), eq(OrderStatus.GATA)))
                .thenReturn(order);

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc
                .perform(put("/api/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value(GATA_STATUS))
                .andExpect(jsonPath(TOTAL_PRICE_JSON_PATH).value(64));
    }

    @Test
    void getActiveOrdersShouldReturnActiveOrders() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.NOUA);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getActiveOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(NOUA_STATUS))
                .andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));
    }

    @Test
    void updateOrderItemStatusShouldReturnUpdatedOrderItem() throws Exception {
        OrderItem item = new OrderItem();
        item.setStatus(OrderStatus.GATA);

        when(orderService.updateOrderItemStatus(eq(7L), eq(OrderStatus.GATA)))
                .thenReturn(item);

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc
                .perform(put("/api/orders/items/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath(STATUS_JSON_PATH).value(GATA_STATUS));
    }

    @Test
    void getKitchenOrdersShouldReturnOrdersInPreparation() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getKitchenOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/kitchen"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(IN_PREPARARE_STATUS))
                .andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));
    }

    @Test
    void getBarOrdersShouldReturnOrdersInPreparation() throws Exception {
        Order order = new Order();
        order.setStatus(OrderStatus.IN_PREPARARE);
        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getBarOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/bar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(IN_PREPARARE_STATUS))
                .andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));
    }
}