package com.software.marketplace.service.impl;

import com.software.marketplace.dto.product.ProductResponseDto;
import com.software.marketplace.dto.product.ProductUpsertRequestDto;
import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllProductsForAdmin() {
        return productRepository.findAll().stream().map(this::toResponseDto).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getAllAvailableProducts() {
        return productRepository.findAll()
                .stream()
                .filter(product -> product.getStock() > 0)
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getAvailableProductById(Long productId) {
        Product product = productRepository.findById(productId)
                .filter(p -> p.getStock() > 0)
                .orElseThrow(() -> new IllegalArgumentException("Product not found or unavailable."));
        return toResponseDto(product);
    }

    @Override
    @Transactional
    public ProductResponseDto createProductForSeller(Long sellerId, ProductUpsertRequestDto request) {
        User seller = findValidatedSeller(sellerId);

        int stock = request.getStockQuantity();
        if (Boolean.FALSE.equals(request.getActive())) {
            stock = 0;
        }

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(stock)
                .seller(seller)
                .build();

        return toResponseDto(productRepository.save(product));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductResponseDto> getProductsBySeller(Long sellerId) {
        return productRepository.findBySellerId(sellerId)
                .stream()
                .map(this::toResponseDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProductResponseDto getProductForSeller(Long productId, Long sellerId) {
        Product product = findProductOwnedBySeller(productId, sellerId);
        return toResponseDto(product);
    }

    @Override
    @Transactional
    public ProductResponseDto updateProductForSeller(Long productId, Long sellerId, ProductUpsertRequestDto request) {
        Product product = findProductOwnedBySeller(productId, sellerId);

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStockQuantity());

        if (Boolean.FALSE.equals(request.getActive())) {
            product.setStock(0);
        }

        return toResponseDto(productRepository.save(product));
    }

    @Override
    @Transactional
    public void deleteProductForSeller(Long productId, Long sellerId) {
        Product product = findProductOwnedBySeller(productId, sellerId);
        productRepository.delete(product);
    }

    private User findValidatedSeller(Long sellerId) {
        User seller = userRepository.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found."));

        boolean isSeller = seller.getRoles().stream()
                .anyMatch(role -> RoleType.ROLE_SELLER.name().equals(role.getName()));

        if (!isSeller) {
            throw new IllegalArgumentException("User does not have SELLER role.");
        }
        return seller;
    }

    private Product findProductOwnedBySeller(Long productId, Long sellerId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found."));

        if (product.getSeller() == null || !sellerId.equals(product.getSeller().getId())) {
            throw new IllegalArgumentException("Product not found for this seller.");
        }
        return product;
    }

    private ProductResponseDto toResponseDto(Product product) {
        return ProductResponseDto.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stockQuantity(product.getStock())
                .active(product.getStock() > 0)
                .sellerId(product.getSeller() != null ? product.getSeller().getId() : null)
                .sellerUsername(product.getSeller() != null ? product.getSeller().getName() : null)
                .createdAt(null)
                .updatedAt(null)
                .build();
    }
}
