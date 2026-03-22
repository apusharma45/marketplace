package com.software.marketplace.repository;

import com.software.marketplace.entity.Product;
import com.software.marketplace.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findBySellerIdReturnsOnlyProductsForThatSeller() {
        User sellerA = userRepository.save(User.builder()
                .name("Seller A")
                .email("seller-a@example.com")
                .password("secret123")
                .enabled(true)
                .build());

        User sellerB = userRepository.save(User.builder()
                .name("Seller B")
                .email("seller-b@example.com")
                .password("secret123")
                .enabled(true)
                .build());

        productRepository.save(Product.builder()
                .name("Phone")
                .description("For seller A")
                .price(new BigDecimal("500.00"))
                .stock(3)
                .seller(sellerA)
                .build());

        productRepository.save(Product.builder()
                .name("Laptop")
                .description("For seller B")
                .price(new BigDecimal("900.00"))
                .stock(2)
                .seller(sellerB)
                .build());

        List<Product> sellerAProducts = productRepository.findBySellerId(sellerA.getId());

        assertThat(sellerAProducts).hasSize(1);
        assertThat(sellerAProducts.get(0).getSeller().getId()).isEqualTo(sellerA.getId());
        assertThat(sellerAProducts.get(0).getName()).isEqualTo("Phone");
    }
}
