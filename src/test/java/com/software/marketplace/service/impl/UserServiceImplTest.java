package com.software.marketplace.service.impl;

import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashSet;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void getAllUsersForAdminMapsUsersToResponseDtos() {
        User user = User.builder()
                .id(10L)
                .name("alice")
                .email("alice@example.com")
                .enabled(true)
                .roles(new LinkedHashSet<>(List.of(
                        Role.builder().id(1L).name("ROLE_BUYER").build(),
                        Role.builder().id(2L).name("ROLE_SELLER").build()
                )))
                .build();
        when(userRepository.findAll()).thenReturn(List.of(user));

        List<UserResponseDto> result = userService.getAllUsersForAdmin();

        assertThat(result).hasSize(1);
        UserResponseDto dto = result.get(0);
        assertThat(dto.getId()).isEqualTo(10L);
        assertThat(dto.getUsername()).isEqualTo("alice");
        assertThat(dto.getEmail()).isEqualTo("alice@example.com");
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getRoles()).containsExactly("ROLE_BUYER", "ROLE_SELLER");
    }
}
