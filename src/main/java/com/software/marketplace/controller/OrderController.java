package com.software.marketplace.controller;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buyers/{buyerId}/orders")
    public ResponseEntity<OrderResponseDto> placeOrderForBuyer(
            @PathVariable Long buyerId,
            @Valid @RequestBody OrderCreateRequestDto request
    ) {
        OrderResponseDto placed = orderService.placeOrderForBuyer(buyerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(placed);
    }

    @GetMapping("/buyers/{buyerId}/orders")
    public List<OrderResponseDto> getOrdersForBuyer(@PathVariable Long buyerId) {
        return orderService.getOrdersForBuyer(buyerId);
    }

    @GetMapping("/sellers/{sellerId}/orders")
    public List<OrderResponseDto> getOrdersForSeller(@PathVariable Long sellerId) {
        return orderService.getOrdersForSeller(sellerId);
    }

    @GetMapping("/admin/orders")
    public List<OrderResponseDto> getAllOrdersForAdmin() {
        return orderService.getAllOrders();
    }
}
