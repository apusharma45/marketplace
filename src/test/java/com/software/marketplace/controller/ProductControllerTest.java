package com.software.marketplace.controller;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    private ProductService productService;
    private ProductController productController;

    @BeforeEach
    void setUp() {
        productService = mock(ProductService.class);
        productController = new ProductController(productService);
    }

    @Test
    void getAllAvailableProductsReturnsServiceResult() {
        when(productService.getAllAvailableProducts()).thenReturn(List.of(
                ProductResponseDto.builder()
                        .id(10L)
                        .name("Laptop")
                        .price(new BigDecimal("1200"))
                        .stockQuantity(2)
                        .active(true)
                        .build()
        ));

        List<ProductResponseDto> result = productController.getAllAvailableProducts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Laptop");
    }

    @Test
    void createProductForSellerReturnsCreatedResponse() {
        ProductUpsertRequestDto request = ProductUpsertRequestDto.builder()
                .name("Monitor")
                .description("27 inch")
                .price(new BigDecimal("300"))
                .stockQuantity(4)
                .active(true)
                .build();
        ProductResponseDto created = ProductResponseDto.builder().id(21L).name("Monitor").build();
        when(productService.createProductForSeller(anyLong(), any(ProductUpsertRequestDto.class))).thenReturn(created);

        ResponseEntity<ProductResponseDto> response = productController.createProductForSeller(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(21L);
    }

    @Test
    void deleteProductForSellerReturnsNoContentAndCallsService() {
        ResponseEntity<Void> response = productController.deleteProductForSeller(1L, 9L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(productService).deleteProductForSeller(9L, 1L);
    }

    @Test
    void handleProductErrorsReturnsBadRequest() {
        doThrow(new IllegalArgumentException("Product not found or unavailable."))
                .when(productService).getAvailableProductById(99L);

        IllegalArgumentException thrown =
                catchThrowableOfType(() -> productController.getAvailableProductById(99L), IllegalArgumentException.class);
        assertThat(thrown).isNotNull();
        ResponseEntity<Map<String, String>> response = productController.handleProductErrors(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Product not found or unavailable.");
    }
}
