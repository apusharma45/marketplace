package com.software.marketplace.repository;

import com.software.marketplace.entity.Order;
import com.software.marketplace.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByBuyerIdReturnsOnlyBuyerOrders() {
        User buyerA = userRepository.save(User.builder()
                .name("Buyer A")
                .email("buyer-a@example.com")
                .password("secret123")
                .enabled(true)
                .build());

        User buyerB = userRepository.save(User.builder()
                .name("Buyer B")
                .email("buyer-b@example.com")
                .password("secret123")
                .enabled(true)
                .build());

        orderRepository.save(Order.builder()
                .buyer(buyerA)
                .createdAt(LocalDateTime.now())
                .status("PENDING")
                .build());

        orderRepository.save(Order.builder()
                .buyer(buyerB)
                .createdAt(LocalDateTime.now())
                .status("PAID")
                .build());

        List<Order> buyerAOrders = orderRepository.findByBuyerId(buyerA.getId());

        assertThat(buyerAOrders).hasSize(1);
        assertThat(buyerAOrders.get(0).getBuyer().getId()).isEqualTo(buyerA.getId());
        assertThat(buyerAOrders.get(0).getStatus()).isEqualTo("PENDING");
    }
}
