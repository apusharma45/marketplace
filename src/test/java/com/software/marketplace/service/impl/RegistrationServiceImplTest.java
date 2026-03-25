package com.software.marketplace.service.impl;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private RegistrationServiceImpl registrationService;

    @Test
    void registerUserSavesNormalizedUserWithEncodedPassword() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("  Alice  ")
                .email("ALICE@EXAMPLE.COM ")
                .password("plain-password")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        when(userRepository.existsByName("  Alice  ")).thenReturn(false);
        when(userRepository.existsByEmail("ALICE@EXAMPLE.COM ")).thenReturn(false);
        when(passwordEncoder.encode("plain-password")).thenReturn("$2a$10$hashedPasswordValue");
        when(roleRepository.findByName(RoleType.ROLE_BUYER.name()))
                .thenReturn(Optional.of(Role.builder().id(1L).name(RoleType.ROLE_BUYER.name()).build()));

        registrationService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getName()).isEqualTo("Alice");
        assertThat(savedUser.getEmail()).isEqualTo("alice@example.com");
        assertThat(savedUser.isEnabled()).isTrue();
        assertThat(savedUser.getPassword()).isNotEqualTo("plain-password");
        assertThat(savedUser.getPassword()).startsWith("$2");
        assertThat(savedUser.getRoles()).extracting(Role::getName)
                .containsExactly(RoleType.ROLE_BUYER.name());
    }

    @Test
    void registerUserThrowsWhenUsernameAlreadyExists() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("taken")
                .email("taken@example.com")
                .password("plain-password")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        when(userRepository.existsByName("taken")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Username is already taken.");

        verifyNoInteractions(roleRepository);
    }

    @Test
    void registerUserThrowsWhenRoleIsNotBuyerOrSeller() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("admin")
                .email("admin@example.com")
                .password("plain-password")
                .roleType(RoleType.ROLE_ADMIN)
                .build();

        when(userRepository.existsByName("admin")).thenReturn(false);
        when(userRepository.existsByEmail("admin@example.com")).thenReturn(false);

        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Only BUYER or SELLER registration is allowed.");

        verifyNoInteractions(roleRepository);
        verify(userRepository).existsByName("admin");
        verify(userRepository).existsByEmail("admin@example.com");
    }

    @Test
    void registerUserThrowsWhenEmailAlreadyExists() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("new-user")
                .email("existing@example.com")
                .password("plain-password")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        when(userRepository.existsByName("new-user")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email is already registered.");

        verifyNoInteractions(roleRepository);
    }

    @Test
    void registerUserThrowsWhenSelectedRoleMissingInDatabase() {
        UserRegistrationRequestDto request = UserRegistrationRequestDto.builder()
                .username("buyer-user")
                .email("buyer@example.com")
                .password("plain-password")
                .roleType(RoleType.ROLE_BUYER)
                .build();

        when(userRepository.existsByName("buyer-user")).thenReturn(false);
        when(userRepository.existsByEmail("buyer@example.com")).thenReturn(false);
        when(roleRepository.findByName(RoleType.ROLE_BUYER.name())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerUser(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Selected role does not exist in the database.");
    }
}
