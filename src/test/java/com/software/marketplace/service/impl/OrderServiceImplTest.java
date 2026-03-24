package com.software.marketplace.service.impl;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.entity.Order;
import com.software.marketplace.entity.OrderItem;
import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.repository.OrderItemRepository;
import com.software.marketplace.repository.OrderRepository;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderItemRepository orderItemRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void placeOrderForBuyerCreatesOrderAndDecrementsStock() {
        User buyer = User.builder()
                .id(1L)
                .name("buyer")
                .roles(Set.of(Role.builder().name("ROLE_BUYER").build()))
                .build();
        User seller = User.builder()
                .id(2L)
                .name("seller")
                .roles(Set.of(Role.builder().name("ROLE_SELLER").build()))
                .build();
        Product product = Product.builder()
                .id(7L)
                .name("Phone")
                .price(new BigDecimal("500"))
                .stock(3)
                .seller(seller)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0, Order.class);
            order.setId(50L);
            return order;
        });
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponseDto response = orderService.placeOrderForBuyer(1L, OrderCreateRequestDto.builder().productId(7L).build());

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());

        assertThat(productCaptor.getValue().getStock()).isEqualTo(2);
        assertThat(response.getId()).isEqualTo(50L);
        assertThat(response.getBuyerId()).isEqualTo(1L);
        assertThat(response.getProductId()).isEqualTo(7L);
        assertThat(response.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("500");
    }

    @Test
    void placeOrderForBuyerThrowsWhenProductIsOutOfStock() {
        User buyer = User.builder()
                .id(1L)
                .name("buyer")
                .roles(Set.of(Role.builder().name("ROLE_BUYER").build()))
                .build();
        Product product = Product.builder()
                .id(7L)
                .stock(0)
                .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(buyer));
        when(productRepository.findById(7L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> orderService.placeOrderForBuyer(1L, OrderCreateRequestDto.builder().productId(7L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product is out of stock.");
    }

    @Test
    void getOrdersForSellerReturnsOnlyOrdersContainingSellerProducts() {
        User seller = User.builder()
                .id(2L)
                .name("seller")
                .roles(Set.of(Role.builder().name("ROLE_SELLER").build()))
                .build();
        User otherSeller = User.builder()
                .id(3L)
                .name("other")
                .roles(Set.of(Role.builder().name("ROLE_SELLER").build()))
                .build();

        Product ownProduct = Product.builder().id(10L).name("A").price(new BigDecimal("10")).seller(seller).build();
        Product otherProduct = Product.builder().id(11L).name("B").price(new BigDecimal("20")).seller(otherSeller).build();

        Order orderForSeller = Order.builder()
                .id(100L)
                .status("PENDING")
                .items(List.of(OrderItem.builder().product(ownProduct).price(new BigDecimal("10")).quantity(1).build()))
                .build();
        Order orderForOther = Order.builder()
                .id(101L)
                .status("PENDING")
                .items(List.of(OrderItem.builder().product(otherProduct).price(new BigDecimal("20")).quantity(1).build()))
                .build();

        when(userRepository.findById(2L)).thenReturn(Optional.of(seller));
        when(orderRepository.findAll()).thenReturn(List.of(orderForSeller, orderForOther));

        List<OrderResponseDto> response = orderService.getOrdersForSeller(2L);

        assertThat(response).hasSize(1);
        assertThat(response.get(0).getId()).isEqualTo(100L);
        assertThat(response.get(0).getProductId()).isEqualTo(10L);
    }

    @Test
    void placeOrderForBuyerThrowsWhenUserIsNotBuyerRole() {
        User sellerRoleUser = User.builder()
                .id(5L)
                .name("seller-like")
                .roles(Set.of(Role.builder().name("ROLE_SELLER").build()))
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(sellerRoleUser));

        assertThatThrownBy(() -> orderService.placeOrderForBuyer(5L, OrderCreateRequestDto.builder().productId(1L).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Buyer not found or invalid role.");
    }
}
