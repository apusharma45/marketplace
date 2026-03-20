package com.example.marketplace.repository;

import com.example.marketplace.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByActiveTrue();

    List<Product> findBySellerId(Long sellerId);

    Optional<Product> findByIdAndSellerId(Long id, Long sellerId);

    Optional<Product> findByIdAndActiveTrue(Long id);

    List<Product> findByNameContainingIgnoreCase(String keyword);
}
