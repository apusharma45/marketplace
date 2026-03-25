package com.software.marketplace.service;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.entity.enums.OrderStatus;

import java.util.List;

public interface OrderService {

    OrderResponseDto placeOrderForBuyer(Long buyerId, OrderCreateRequestDto request);

    OrderResponseDto placeOrderForBuyer(Long buyerId, List<OrderCreateRequestDto> requests);

    List<OrderResponseDto> getOrdersForBuyer(Long buyerId);

    List<OrderResponseDto> getOrdersForSeller(Long sellerId);

    List<OrderResponseDto> getAllOrders();

    void markOrderAsShippedForSeller(Long orderId, Long sellerId);

    void updateOrderStatusByAdmin(Long orderId, OrderStatus status);
}
