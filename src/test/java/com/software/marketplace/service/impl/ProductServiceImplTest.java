package com.software.marketplace.service.impl;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
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
class ProductServiceImplTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    @Test
    void getAllAvailableProductsReturnsOnlyProductsWithPositiveStock() {
        Product available = Product.builder().id(1L).name("Laptop").price(new BigDecimal("1000")).stock(5).build();
        Product unavailable = Product.builder().id(2L).name("Mouse").price(new BigDecimal("20")).stock(0).build();
        when(productRepository.findAll()).thenReturn(List.of(available, unavailable));

        List<ProductResponseDto> result = productService.getAllAvailableProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
        assertThat(result.get(0).getActive()).isTrue();
    }

    @Test
    void createProductForSellerSetsStockZeroWhenMarkedInactive() {
        User seller = User.builder()
                .id(3L)
                .name("seller1")
                .roles(Set.of(Role.builder().name("ROLE_SELLER").build()))
                .build();
        ProductUpsertRequestDto request = ProductUpsertRequestDto.builder()
                .name("  Keyboard  ")
                .description("mechanical")
                .price(new BigDecimal("50"))
                .stockQuantity(10)
                .active(false)
                .build();

        when(userRepository.findById(3L)).thenReturn(Optional.of(seller));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0, Product.class);
            p.setId(99L);
            return p;
        });

        ProductResponseDto response = productService.createProductForSeller(3L, request);

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(productCaptor.capture());
        Product savedProduct = productCaptor.getValue();

        assertThat(savedProduct.getName()).isEqualTo("Keyboard");
        assertThat(savedProduct.getStock()).isZero();
        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getActive()).isFalse();
    }

    @Test
    void createProductForSellerThrowsWhenUserIsNotSeller() {
        User buyer = User.builder()
                .id(4L)
                .name("buyer1")
                .roles(Set.of(Role.builder().name("ROLE_BUYER").build()))
                .build();
        ProductUpsertRequestDto request = ProductUpsertRequestDto.builder()
                .name("Item")
                .price(new BigDecimal("10"))
                .stockQuantity(2)
                .active(true)
                .build();

        when(userRepository.findById(4L)).thenReturn(Optional.of(buyer));

        assertThatThrownBy(() -> productService.createProductForSeller(4L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("User does not have SELLER role.");
    }
}
