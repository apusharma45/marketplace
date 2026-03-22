package com.software.marketplace.repository;

import com.software.marketplace.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByEmailReturnsUserWhenPresent() {
        User user = User.builder()
                .name("Alice")
                .email("alice@example.com")
                .password("secret123")
                .enabled(true)
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("alice@example.com");

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("Alice");
    }

    @Test
    void existsByEmailReflectsPresence() {
        User user = User.builder()
                .name("Bob")
                .email("bob@example.com")
                .password("secret123")
                .enabled(true)
                .build();
        userRepository.save(user);

        assertThat(userRepository.existsByEmail("bob@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }
}
