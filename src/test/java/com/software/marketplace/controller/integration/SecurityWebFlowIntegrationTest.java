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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import java.util.Set;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class SecurityWebFlowIntegrationTest {

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

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role buyerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_BUYER.name()).build());
        Role sellerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());
        Role adminRole = roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN.name()).build());

        userRepository.save(User.builder()
                .name("buyer_web")
                .email("buyer_web@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        userRepository.save(User.builder()
                .name("seller_web")
                .email("seller_web@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        userRepository.save(User.builder()
                .name("admin_web")
                .email("admin_web@example.com")
                .password(passwordEncoder.encode("password123"))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());
    }

    @Test
    void anonymousUserCannotAccessProtectedApiAndCanOpenLoginPage() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    void buyerLoginSucceedsAndDashboardRedirectsToBuyerHome() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user("buyer_web")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn();

        mockMvc.perform(get("/dashboard").session((org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer"));
    }

    @Test
    void buyerCannotAccessSellerOrAdminPages() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user("buyer_web")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/seller/products").session(session))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void sellerCanAccessSellerPagesButCannotAccessAdminPages() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin("/login")
                        .user("seller_web")
                        .password("password123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        org.springframework.mock.web.MockHttpSession session =
                (org.springframework.mock.web.MockHttpSession) loginResult.getRequest().getSession(false);

        mockMvc.perform(get("/seller/products").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/users").session(session))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiRoleRestrictionsAllowOnlyMatchingRole() throws Exception {
        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("admin_web", "password123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/admin/users")
                        .with(httpBasic("buyer_web", "password123")))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/sellers/1/products")
                        .with(httpBasic("seller_web", "password123")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/sellers/1/products")
                        .with(httpBasic("buyer_web", "password123")))
                .andExpect(status().isForbidden());
    }
}
