package com.example.marketplace.repository;

import com.example.marketplace.entity.Order;
import com.example.marketplace.entity.enums.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyerId(Long buyerId);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findByBuyerUsername(String username);

    List<Order> findDistinctByProductsSellerId(Long sellerId);
}
