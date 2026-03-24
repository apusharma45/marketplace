package com.software.marketplace.controller;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.service.RegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AuthControllerTest {

    private RegistrationService registrationService;
    private AuthController authController;

    @BeforeEach
    void setUp() {
        registrationService = mock(RegistrationService.class);
        authController = new AuthController(registrationService);
    }

    @Test
    void registerReturnsCreatedResponseWhenRegistrationSucceeds() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("buyer1")
                .email("buyer1@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        ResponseEntity<Map<String, String>> response = authController.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).containsEntry("message", "User registered successfully.");
        verify(registrationService).registerUser(any(UserRegistrationRequestDto.class));
    }

    @Test
    void handleRegistrationExceptionReturnsBadRequestBody() {
        doThrow(new IllegalArgumentException("Email is already registered."))
                .when(registrationService).registerUser(any(UserRegistrationRequestDto.class));

        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("buyer1")
                .email("buyer1@example.com")
                .password("password123")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        IllegalArgumentException thrown =
                catchThrowableOfType(() -> authController.register(request), IllegalArgumentException.class);
        assertThat(thrown).isNotNull();
        ResponseEntity<Map<String, String>> response =
                authController.handleRegistrationException(thrown);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Email is already registered.");
    }
}
