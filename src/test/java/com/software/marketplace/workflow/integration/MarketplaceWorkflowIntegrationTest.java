package com.software.marketplace.workflow.integration;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;
import com.software.marketplace.dto.order.OrderCreateRequestDto;
import com.software.marketplace.dto.order.OrderResponseDto;
import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.dto.user.UserResponseDto;
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
import com.software.marketplace.service.RegistrationService;
import com.software.marketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class MarketplaceWorkflowIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private ProductService productService;

    @Autowired
    private OrderService orderService;

    @Autowired
    private UserService userService;

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

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(Role.builder().name(RoleType.ROLE_BUYER.name()).build());
        roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());
        roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN.name()).build());
    }

    @Test
    void fullMarketplaceFlowWorksAcrossServices() {
        registrationService.registerUser(UserRegistrationRequestDto.builder()
                .username("seller1")
                .email("seller1@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_SELLER)
                .build());

        registrationService.registerUser(UserRegistrationRequestDto.builder()
                .username("buyer1")
                .email("buyer1@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build());

        User seller = userRepository.findByEmail("seller1@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer1@example.com").orElseThrow();

        ProductResponseDto createdProduct = productService.createProductForSeller(
                seller.getId(),
                ProductUpsertRequestDto.builder()
                        .name("Laptop")
                        .description("Ultrabook")
                        .price(new BigDecimal("1200"))
                        .stockQuantity(2)
                        .active(true)
                        .build()
        );

        OrderResponseDto placedOrder = orderService.placeOrderForBuyer(
                buyer.getId(),
                OrderCreateRequestDto.builder().productId(createdProduct.getId()).build()
        );

        int stockAfterOrder = productRepository.findById(createdProduct.getId()).orElseThrow().getStock();
        List<OrderResponseDto> buyerOrders = orderService.getOrdersForBuyer(buyer.getId());
        List<OrderResponseDto> sellerOrders = orderService.getOrdersForSeller(seller.getId());
        List<OrderResponseDto> allOrders = orderService.getAllOrders();
        List<UserResponseDto> adminUserView = userService.getAllUsersForAdmin();

        assertThat(placedOrder.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stockAfterOrder).isEqualTo(1);

        assertThat(buyerOrders).hasSize(1);
        assertThat(buyerOrders.get(0).getProductId()).isEqualTo(createdProduct.getId());

        assertThat(sellerOrders).hasSize(1);
        assertThat(sellerOrders.get(0).getProductId()).isEqualTo(createdProduct.getId());

        assertThat(allOrders).hasSize(1);
        assertThat(adminUserView).hasSize(2);
    }

    @Test
    void placingSecondOrderFailsWhenStockIsDepleted() {
        registrationService.registerUser(UserRegistrationRequestDto.builder()
                .username("seller2")
                .email("seller2@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_SELLER)
                .build());

        registrationService.registerUser(UserRegistrationRequestDto.builder()
                .username("buyer2")
                .email("buyer2@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build());

        User seller = userRepository.findByEmail("seller2@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer2@example.com").orElseThrow();

        ProductResponseDto product = productService.createProductForSeller(
                seller.getId(),
                ProductUpsertRequestDto.builder()
                        .name("Mouse")
                        .description("Wireless")
                        .price(new BigDecimal("25"))
                        .stockQuantity(1)
                        .active(true)
                        .build()
        );

        orderService.placeOrderForBuyer(
                buyer.getId(),
                OrderCreateRequestDto.builder().productId(product.getId()).build()
        );

        assertThatThrownBy(() -> orderService.placeOrderForBuyer(
                buyer.getId(),
                OrderCreateRequestDto.builder().productId(product.getId()).build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product is out of stock.");
    }
}
