package com.software.marketplace.service.integration;

import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.OrderStatus;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.OrderItemRepository;
import com.software.marketplace.repository.OrderRepository;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.OrderService;
import com.software.marketplace.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OrderServiceIntegrationTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ProductService productService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    private Long buyerId;
    private Long sellerId;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role buyerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_BUYER.name()).build());
        Role sellerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());

        User buyer = userRepository.save(User.builder()
                .name("buyer1")
                .email("buyer1@example.com")
                .password("secret")
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());
        User seller = userRepository.save(User.builder()
                .name("seller1")
                .email("seller1@example.com")
                .password("secret")
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        buyerId = buyer.getId();
        sellerId = seller.getId();
    }

    @Test
    void placeOrderForBuyerCreatesOrderAndDecrementsStock() {
        Long productId = productService.createProductForSeller(sellerId, ProductUpsertRequestDto.builder()
                .name("Tablet")
                .description("test")
                .price(new BigDecimal("200"))
                .stockQuantity(2)
                .active(true)
                .build()).getId();

        OrderResponseDto placed = orderService.placeOrderForBuyer(
                buyerId,
                OrderCreateRequestDto.builder().productId(productId).build()
        );

        int currentStock = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(placed.getId()).isNotNull();
        assertThat(placed.getBuyerId()).isEqualTo(buyerId);
        assertThat(placed.getProductId()).isEqualTo(productId);
        assertThat(placed.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(currentStock).isEqualTo(1);
    }

    @Test
    void getOrdersForSellerReturnsOnlySellerOrders() {
        Long productId = productService.createProductForSeller(sellerId, ProductUpsertRequestDto.builder()
                .name("Headphone")
                .description("test")
                .price(new BigDecimal("80"))
                .stockQuantity(1)
                .active(true)
                .build()).getId();

        orderService.placeOrderForBuyer(
                buyerId,
                OrderCreateRequestDto.builder().productId(productId).build()
        );

        List<OrderResponseDto> sellerOrders = orderService.getOrdersForSeller(sellerId);

        assertThat(sellerOrders).hasSize(1);
        assertThat(sellerOrders.get(0).getProductId()).isEqualTo(productId);
        assertThat(sellerOrders.get(0).getSellerUsername()).isEqualTo("seller1");
    }
}
