package com.software.marketplace.controller.integration;

import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.OrderItemRepository;
import com.software.marketplace.repository.OrderRepository;
import com.software.marketplace.repository.ProductRepository;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration"
})
@ActiveProfiles("test")
@Transactional
class MarketplaceApiIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

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
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();

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
    void apiWorkflowRegisterCreateProductAndPlaceOrder() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "seller_api",
                                  "email": "seller_api@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_SELLER"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "buyer_api",
                                  "email": "buyer_api@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_BUYER"
                                }
                                """))
                .andExpect(status().isCreated());

        User seller = userRepository.findByEmail("seller_api@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer_api@example.com").orElseThrow();

        mockMvc.perform(post("/api/sellers/{sellerId}/products", seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "API Laptop",
                                  "description": "For integration test",
                                  "price": 999.99,
                                  "stockQuantity": 2,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("API Laptop"));

        Long productId = productRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/buyers/{buyerId}/orders", buyer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.productId").value(productId));

        int stockAfterOrder = productRepository.findById(productId).orElseThrow().getStock();
        assertThat(stockAfterOrder).isEqualTo(1);

        mockMvc.perform(get("/api/buyers/{buyerId}/orders", buyer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId));

        mockMvc.perform(get("/api/sellers/{sellerId}/orders", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].productId").value(productId));
    }

    @Test
    void apiRejectsSecondOrderWhenProductIsOutOfStock() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "seller_stock",
                                  "email": "seller_stock@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_SELLER"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "buyer_stock",
                                  "email": "buyer_stock@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_BUYER"
                                }
                                """))
                .andExpect(status().isCreated());

        User seller = userRepository.findByEmail("seller_stock@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer_stock@example.com").orElseThrow();

        mockMvc.perform(post("/api/sellers/{sellerId}/products", seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Single Stock Item",
                                  "description": "only one",
                                  "price": 100,
                                  "stockQuantity": 1,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated());

        Long productId = productRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/buyers/{buyerId}/orders", buyer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/buyers/{buyerId}/orders", buyer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d
                                }
                                """.formatted(productId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Product is out of stock."));
    }

    @Test
    void apiAdminEndpointsReturnUsersProductsAndOrders() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "seller_admin",
                                  "email": "seller_admin@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_SELLER"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "buyer_admin",
                                  "email": "buyer_admin@example.com",
                                  "password": "password123",
                                  "roleType": "ROLE_BUYER"
                                }
                                """))
                .andExpect(status().isCreated());

        User seller = userRepository.findByEmail("seller_admin@example.com").orElseThrow();
        User buyer = userRepository.findByEmail("buyer_admin@example.com").orElseThrow();

        mockMvc.perform(post("/api/sellers/{sellerId}/products", seller.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Admin Visible Product",
                                  "description": "admin list test",
                                  "price": 300,
                                  "stockQuantity": 2,
                                  "active": true
                                }
                                """))
                .andExpect(status().isCreated());

        Long productId = productRepository.findAll().get(0).getId();

        mockMvc.perform(post("/api/buyers/{buyerId}/orders", buyer.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": %d
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));

        mockMvc.perform(get("/api/admin/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
