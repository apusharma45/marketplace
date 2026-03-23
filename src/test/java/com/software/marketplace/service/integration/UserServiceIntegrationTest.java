package com.software.marketplace.service.integration;

import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class UserServiceIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();
    }

    @Test
    void getAllUsersForAdminReturnsMappedUsers() {
        Role buyerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_BUYER.name()).build());
        Role sellerRole = roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());

        userRepository.save(User.builder()
                .name("alice")
                .email("alice@example.com")
                .password("secret")
                .enabled(true)
                .roles(Set.of(buyerRole, sellerRole))
                .build());

        List<UserResponseDto> users = userService.getAllUsersForAdmin();

        assertThat(users).hasSize(1);
        UserResponseDto dto = users.get(0);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getRoles()).contains(RoleType.ROLE_BUYER.name(), RoleType.ROLE_SELLER.name());
    }
}
