package com.software.marketplace.repository;

import com.software.marketplace.entity.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class RoleRepositoryTest {

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByNameReturnsRoleWhenItExists() {
        Role role = Role.builder().name("ROLE_BUYER").build();
        roleRepository.save(role);

        Optional<Role> found = roleRepository.findByName("ROLE_BUYER");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("ROLE_BUYER");
    }
}
