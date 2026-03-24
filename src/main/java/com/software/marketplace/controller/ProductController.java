package com.software.marketplace.controller;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping("/products")
    public List<ProductResponseDto> getAllAvailableProducts() {
        return productService.getAllAvailableProducts();
    }

    @GetMapping("/products/{productId}")
    public ProductResponseDto getAvailableProductById(@PathVariable Long productId) {
        return productService.getAvailableProductById(productId);
    }

    @GetMapping("/sellers/{sellerId}/products")
    public List<ProductResponseDto> getProductsBySeller(@PathVariable Long sellerId) {
        return productService.getProductsBySeller(sellerId);
    }

    @PostMapping("/sellers/{sellerId}/products")
    public ResponseEntity<ProductResponseDto> createProductForSeller(
            @PathVariable Long sellerId,
            @Valid @RequestBody ProductUpsertRequestDto request
    ) {
        ProductResponseDto created = productService.createProductForSeller(sellerId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/sellers/{sellerId}/products/{productId}")
    public ProductResponseDto getProductForSeller(
            @PathVariable Long sellerId,
            @PathVariable Long productId
    ) {
        return productService.getProductForSeller(productId, sellerId);
    }

    @PutMapping("/sellers/{sellerId}/products/{productId}")
    public ProductResponseDto updateProductForSeller(
            @PathVariable Long sellerId,
            @PathVariable Long productId,
            @Valid @RequestBody ProductUpsertRequestDto request
    ) {
        return productService.updateProductForSeller(productId, sellerId, request);
    }

    @DeleteMapping("/sellers/{sellerId}/products/{productId}")
    public ResponseEntity<Void> deleteProductForSeller(
            @PathVariable Long sellerId,
            @PathVariable Long productId
    ) {
        productService.deleteProductForSeller(productId, sellerId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/products")
    public List<ProductResponseDto> getAllProductsForAdmin() {
        return productService.getAllProductsForAdmin();
    }
}
