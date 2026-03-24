package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderControllerTest {

    private OrderService orderService;
    private OrderController orderController;

    @BeforeEach
    void setUp() {
        orderService = mock(OrderService.class);
        orderController = new OrderController(orderService);
    }

    @Test
    void placeOrderForBuyerReturnsCreatedResponse() {
        OrderCreateRequestDto request = OrderCreateRequestDto.builder().productId(5L).build();
        OrderResponseDto placed = OrderResponseDto.builder()
                .id(100L)
                .buyerId(2L)
                .productId(5L)
                .status(OrderStatus.PENDING)
                .totalAmount(new BigDecimal("1200"))
                .build();
        when(orderService.placeOrderForBuyer(anyLong(), any(OrderCreateRequestDto.class))).thenReturn(placed);

        ResponseEntity<OrderResponseDto> response = orderController.placeOrderForBuyer(2L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(100L);
    }

    @Test
    void getOrdersForBuyerReturnsServiceResult() {
        when(orderService.getOrdersForBuyer(2L)).thenReturn(List.of(
                OrderResponseDto.builder().id(100L).buyerId(2L).build()
        ));

        List<OrderResponseDto> result = orderController.getOrdersForBuyer(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getBuyerId()).isEqualTo(2L);
    }

    @Test
    void handleOrderErrorsReturnsBadRequest() {
        doThrow(new IllegalArgumentException("Product is out of stock."))
                .when(orderService).placeOrderForBuyer(anyLong(), any(OrderCreateRequestDto.class));

        IllegalArgumentException thrown = catchThrowableOfType(
                () -> orderController.placeOrderForBuyer(2L, OrderCreateRequestDto.builder().productId(5L).build()),
                IllegalArgumentException.class
        );
        assertThat(thrown).isNotNull();
        ResponseEntity<Map<String, String>> response = orderController.handleOrderErrors(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Product is out of stock.");
    }
}
