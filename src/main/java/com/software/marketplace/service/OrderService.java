package com.software.marketplace.service;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;

import java.util.List;

public interface OrderService {

    OrderResponseDto placeOrderForBuyer(Long buyerId, OrderCreateRequestDto request);

    List<OrderResponseDto> getOrdersForBuyer(Long buyerId);

    List<OrderResponseDto> getOrdersForSeller(Long sellerId);

    List<OrderResponseDto> getAllOrders();
}
