package com.software.marketplace.service.integration;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.OrderItemRepository;
import com.software.marketplace.repository.OrderRepository;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ProductServiceIntegrationTest {

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

    private Long sellerId;

    @BeforeEach
    void setUp() {
        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role sellerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());
        User seller = userRepository.save(User.builder()
                .name("seller1")
                .email("seller1@example.com")
                .password("secret")
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());
        sellerId = seller.getId();
    }

    @Test
    void createProductForSellerPersistsAndReturnsAvailableProduct() {
        ProductUpsertRequestDto request = ProductUpsertRequestDto.builder()
                .name("Monitor")
                .description("27 inch")
                .price(new BigDecimal("300"))
                .stockQuantity(4)
                .active(true)
                .build();

        ProductResponseDto created = productService.createProductForSeller(sellerId, request);
        List<ProductResponseDto> available = productService.getAllAvailableProducts();

        assertThat(created.getId()).isNotNull();
        assertThat(created.getName()).isEqualTo("Monitor");
        assertThat(created.getStockQuantity()).isEqualTo(4);
        assertThat(available).hasSize(1);
        assertThat(available.get(0).getId()).isEqualTo(created.getId());
    }

    @Test
    void updateProductForSellerWithInactiveFalseMakesProductUnavailable() {
        ProductResponseDto created = productService.createProductForSeller(sellerId, ProductUpsertRequestDto.builder()
                .name("Keyboard")
                .description("mechanical")
                .price(new BigDecimal("50"))
                .stockQuantity(5)
                .active(true)
                .build());

        ProductResponseDto updated = productService.updateProductForSeller(created.getId(), sellerId, ProductUpsertRequestDto.builder()
                .name("Keyboard V2")
                .description("updated")
                .price(new BigDecimal("60"))
                .stockQuantity(10)
                .active(false)
                .build());

        assertThat(updated.getStockQuantity()).isZero();
        assertThat(updated.getActive()).isFalse();
        assertThatThrownBy(() -> productService.getAvailableProductById(created.getId()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Product not found or unavailable.");
    }
}
