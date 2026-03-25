package com.software.marketplace.service;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;

import java.util.List;

public interface ProductService {

    List<ProductResponseDto> getAllProductsForAdmin();

    List<ProductResponseDto> getAllAvailableProducts();

    ProductResponseDto getAvailableProductById(Long productId);

    ProductResponseDto createProductForSeller(Long sellerId, ProductUpsertRequestDto request);

    List<ProductResponseDto> getProductsBySeller(Long sellerId);

    ProductResponseDto getProductForSeller(Long productId, Long sellerId);

    ProductResponseDto updateProductForSeller(Long productId, Long sellerId, ProductUpsertRequestDto request);

    void deleteProductForSeller(Long productId, Long sellerId);

    void deleteProductForAdmin(Long productId);
}
