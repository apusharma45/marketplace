package com.software.marketplace.controller;

import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdminUserControllerTest {

    private UserService userService;
    private AdminUserController adminUserController;

    @BeforeEach
    void setUp() {
        userService = mock(UserService.class);
        adminUserController = new AdminUserController(userService);
    }

    @Test
    void getAllUsersForAdminReturnsServiceResult() {
        List<UserResponseDto> expected = List.of(
                UserResponseDto.builder()
                        .id(1L)
                        .username("user1")
                        .email("user1@example.com")
                        .enabled(true)
                        .roles(Set.of("ROLE_BUYER"))
                        .build()
        );
        when(userService.getAllUsersForAdmin()).thenReturn(expected);

        List<UserResponseDto> result = adminUserController.getAllUsersForAdmin();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUsername()).isEqualTo("user1");
        assertThat(result.get(0).getEmail()).isEqualTo("user1@example.com");
    }
}
