package com.software.marketplace.service.integration;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class RegistrationServiceIntegrationTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        roleRepository.deleteAll();

        roleRepository.save(Role.builder().name(RoleType.ROLE_BUYER.name()).build());
        roleRepository.save(Role.builder().name(RoleType.ROLE_SELLER.name()).build());
        roleRepository.save(Role.builder().name(RoleType.ROLE_ADMIN.name()).build());
    }

    @Test
    void registerUserPersistsUserWithExpectedRole() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("newbuyer")
                .email("newbuyer@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        registrationService.registerUser(request);

        User saved = userRepository.findByEmail("newbuyer@example.com").orElseThrow();
        assertThat(saved.getName()).isEqualTo("newbuyer");
        assertThat(saved.isEnabled()).isTrue();
        assertThat(saved.getPassword()).isNotEqualTo("password123");
        assertThat(saved.getRoles()).extracting(Role::getName).containsExactly(RoleType.ROLE_BUYER.name());
    }

    @Test
    void registerUserRejectsDuplicateEmail() {
        Role buyerRole = roleRepository.findByName(RoleType.ROLE_BUYER.name()).orElseThrow();
        userRepository.save(User.builder()
                .name("existing")
                .email("dup@example.com")
                .password("secret")
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("another")
                .email("dup@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered.");
    }
}
