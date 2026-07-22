package com.licenta.horeca.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.licenta.horeca.auth.security.CustomUserDetailsService;
import com.licenta.horeca.auth.security.JwtService;
import com.licenta.horeca.auth.security.SecurityConfig;
import com.licenta.horeca.order.dto.OrderStatisticsResponse;
import com.licenta.horeca.order.entity.Order;
import com.licenta.horeca.order.entity.OrderItem;
import com.licenta.horeca.order.enums.OrderStatus;
import com.licenta.horeca.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "admin@test.com", roles = "ADMIN")
class OrderControllerTest {

    private static final String STATUS_JSON_PATH = "$.status";

    private static final String TOTAL_PRICE_JSON_PATH = "$.totalPrice";

    private static final String FIRST_STATUS_JSON_PATH = "$[0].status";

    private static final String FIRST_TOTAL_PRICE_JSON_PATH = "$[0].totalPrice";

    private static final String NOUA_STATUS = "NOUA";

    private static final String GATA_STATUS = "GATA";

    private static final String IN_PREPARARE_STATUS = "IN_PREPARARE";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    @Test
    void createOrderShouldReturnCreatedOrder() throws Exception {

        Order order = new Order();

        order.setStatus(OrderStatus.NOUA);
        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.createOrder(anyString(), anyList())).thenReturn(order);

        Map<String, Object> request = Map.of("sessionCode", "TEST123", "items", List.of(Map.of("productId", 1, "quantity", 2)));

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath(STATUS_JSON_PATH).value(NOUA_STATUS)).andExpect(jsonPath(TOTAL_PRICE_JSON_PATH).value(64));

        ArgumentCaptor<String> sessionCodeCaptor = ArgumentCaptor.forClass(String.class);

        ArgumentCaptor<List<OrderService.OrderItemRequest>> itemsCaptor = ArgumentCaptor.forClass(List.class);

        verify(orderService).createOrder(sessionCodeCaptor.capture(), itemsCaptor.capture());

        assertEquals("TEST123", sessionCodeCaptor.getValue());

        assertEquals(1, itemsCaptor.getValue().size());
    }

    @Test
    void getAllOrdersShouldReturnAllOrders() throws Exception {

        Order firstOrder = new Order();

        firstOrder.setStatus(OrderStatus.NOUA);
        firstOrder.setTotalPrice(BigDecimal.valueOf(40));

        Order secondOrder = new Order();

        secondOrder.setStatus(OrderStatus.IN_PREPARARE);

        secondOrder.setTotalPrice(BigDecimal.valueOf(75));

        when(orderService.getAllOrders()).thenReturn(List.of(firstOrder, secondOrder));

        mockMvc.perform(get("/api/orders").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.length()").value(2)).andExpect(jsonPath("$[0].status").value(NOUA_STATUS)).andExpect(jsonPath("$[0].totalPrice").value(40)).andExpect(jsonPath("$[1].status").value(IN_PREPARARE_STATUS)).andExpect(jsonPath("$[1].totalPrice").value(75));

        verify(orderService).getAllOrders();
    }

    @Test
    void getAllOrdersShouldReturnEmptyArray() throws Exception {

        when(orderService.getAllOrders()).thenReturn(List.of());

        mockMvc.perform(get("/api/orders")).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));

        verify(orderService).getAllOrders();
    }

    @Test
    void getActiveOrdersShouldReturnActiveOrders() throws Exception {

        Order order = new Order();

        order.setStatus(OrderStatus.NOUA);

        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getActiveOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/active")).andExpect(status().isOk()).andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(NOUA_STATUS)).andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));

        verify(orderService).getActiveOrders();
    }

    @Test
    void getTodayStatisticsShouldReturnStatistics() throws Exception {

        OrderStatisticsResponse statistics = new OrderStatisticsResponse(5, 12, new BigDecimal("485.50"), 4.25);

        when(orderService.getTodayStatistics()).thenReturn(statistics);

        mockMvc.perform(get("/api/orders/statistics/today").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)).andExpect(jsonPath("$.activeOrders").value(5)).andExpect(jsonPath("$.servedOrders").value(12)).andExpect(jsonPath("$.sales").value(485.5)).andExpect(jsonPath("$.averageRating").value(4.25));

        verify(orderService).getTodayStatistics();
    }

    @Test
    void getStatisticsShouldParseAndForwardAllParameters() throws Exception {

        LocalDate date = LocalDate.of(2026, 7, 11);

        LocalTime startTime = LocalTime.of(10, 30);

        LocalTime endTime = LocalTime.of(18, 45);

        OrderStatisticsResponse statistics = new OrderStatisticsResponse(3, 8, new BigDecimal("320.75"), 4.5);

        when(orderService.getStatistics(date, startTime, endTime)).thenReturn(statistics);

        mockMvc.perform(get("/api/orders/statistics").param("date", "2026-07-11").param("startTime", "10:30").param("endTime", "18:45").accept(MediaType.APPLICATION_JSON)).andExpect(status().isOk()).andExpect(jsonPath("$.activeOrders").value(3)).andExpect(jsonPath("$.servedOrders").value(8)).andExpect(jsonPath("$.sales").value(320.75)).andExpect(jsonPath("$.averageRating").value(4.5));

        verify(orderService).getStatistics(date, startTime, endTime);
    }

    @Test
    void getStatisticsShouldAcceptMissingOptionalParameters() throws Exception {

        OrderStatisticsResponse statistics = new OrderStatisticsResponse(0, 0, BigDecimal.ZERO, 0.0);

        when(orderService.getStatistics(null, null, null)).thenReturn(statistics);

        mockMvc.perform(get("/api/orders/statistics")).andExpect(status().isOk()).andExpect(jsonPath("$.activeOrders").value(0)).andExpect(jsonPath("$.servedOrders").value(0)).andExpect(jsonPath("$.sales").value(0)).andExpect(jsonPath("$.averageRating").value(0.0));

        verify(orderService).getStatistics(null, null, null);
    }

    @Test
    void getStatisticsShouldRejectInvalidDate() throws Exception {

        mockMvc.perform(get("/api/orders/statistics").param("date", "2026-25-99")).andExpect(status().isBadRequest());

        verify(orderService, never()).getStatistics(any(), any(), any());
    }

    @Test
    void getStatisticsShouldRejectInvalidStartTime() throws Exception {

        mockMvc.perform(get("/api/orders/statistics").param("date", "2026-07-11").param("startTime", "27:90").param("endTime", "18:00")).andExpect(status().isBadRequest());

        verify(orderService, never()).getStatistics(any(), any(), any());
    }

    @Test
    void getStatisticsShouldRejectInvalidEndTime() throws Exception {

        mockMvc.perform(get("/api/orders/statistics").param("date", "2026-07-11").param("startTime", "10:00").param("endTime", "invalid")).andExpect(status().isBadRequest());

        verify(orderService, never()).getStatistics(any(), any(), any());
    }

    @Test
    void getKitchenOrdersShouldReturnOrdersInPreparation() throws Exception {

        Order order = new Order();

        order.setStatus(OrderStatus.IN_PREPARARE);

        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getKitchenOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/kitchen")).andExpect(status().isOk()).andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(IN_PREPARARE_STATUS)).andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));

        verify(orderService).getKitchenOrders();
    }

    @Test
    void getBarOrdersShouldReturnOrdersInPreparation() throws Exception {

        Order order = new Order();

        order.setStatus(OrderStatus.IN_PREPARARE);

        order.setTotalPrice(BigDecimal.valueOf(40));

        when(orderService.getBarOrders()).thenReturn(List.of(order));

        mockMvc.perform(get("/api/orders/bar")).andExpect(status().isOk()).andExpect(jsonPath(FIRST_STATUS_JSON_PATH).value(IN_PREPARARE_STATUS)).andExpect(jsonPath(FIRST_TOTAL_PRICE_JSON_PATH).value(40));

        verify(orderService).getBarOrders();
    }

    @Test
    void updateOrderStatusShouldReturnUpdatedOrder() throws Exception {

        Order order = new Order();

        order.setStatus(OrderStatus.GATA);

        order.setTotalPrice(BigDecimal.valueOf(64));

        when(orderService.updateOrderStatus(1L, OrderStatus.GATA)).thenReturn(order);

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc.perform(put("/api/orders/1/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath(STATUS_JSON_PATH).value(GATA_STATUS)).andExpect(jsonPath(TOTAL_PRICE_JSON_PATH).value(64));

        verify(orderService).updateOrderStatus(1L, OrderStatus.GATA);
    }

    @Test
    void updateOrderStatusShouldRejectInvalidStatus() throws Exception {

        Map<String, Object> request = Map.of("status", "STATUS_INEXISTENT");

        mockMvc.perform(put("/api/orders/1/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(anyLong(), any(OrderStatus.class));
    }

    @Test
    void updateOrderStatusShouldRejectInvalidOrderId() throws Exception {

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc.perform(put("/api/orders/invalid/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderStatus(anyLong(), any(OrderStatus.class));
    }

    @Test
    void updateOrderItemStatusShouldReturnUpdatedOrderItem() throws Exception {

        OrderItem item = new OrderItem();

        item.setStatus(OrderStatus.GATA);

        when(orderService.updateOrderItemStatus(7L, OrderStatus.GATA)).thenReturn(item);

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc.perform(put("/api/orders/items/7/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isOk()).andExpect(jsonPath(STATUS_JSON_PATH).value(GATA_STATUS));

        verify(orderService).updateOrderItemStatus(7L, OrderStatus.GATA);
    }

    @Test
    void updateOrderItemStatusShouldRejectInvalidStatus() throws Exception {

        Map<String, Object> request = Map.of("status", "INVALID");

        mockMvc.perform(put("/api/orders/items/7/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderItemStatus(anyLong(), any(OrderStatus.class));
    }

    @Test
    void updateOrderItemStatusShouldRejectInvalidItemId() throws Exception {

        Map<String, Object> request = Map.of("status", GATA_STATUS);

        mockMvc.perform(put("/api/orders/items/abc/status").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request))).andExpect(status().isBadRequest());

        verify(orderService, never()).updateOrderItemStatus(anyLong(), any(OrderStatus.class));
    }

    @Test
    void createOrderShouldRejectMalformedJson() throws Exception {

        String malformedJson = """
                {
                  "sessionCode": "TEST123",
                  "items":
                }
                """;

        mockMvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(malformedJson)).andExpect(status().isBadRequest());

        verify(orderService, never()).createOrder(anyString(), anyList());
    }
}
