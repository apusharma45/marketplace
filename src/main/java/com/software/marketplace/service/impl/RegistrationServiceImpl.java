package com.software.marketplace.service.impl;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;
import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.User;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.RoleRepository;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.RegistrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RegistrationServiceImpl implements RegistrationService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void registerUser(UserRegistrationRequestDto request) {
        if (userRepository.existsByName(request.getUsername())) {
            throw new IllegalArgumentException("Username is already taken.");
        }

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered.");
        }

        RoleType selectedRole = request.getRoleType();
        if (selectedRole != RoleType.ROLE_BUYER && selectedRole != RoleType.ROLE_SELLER) {
            throw new IllegalArgumentException("Only BUYER or SELLER registration is allowed.");
        }

        Role role = roleRepository.findByName(selectedRole.name())
                .orElseThrow(() -> new IllegalArgumentException("Selected role does not exist in the database."));

        Set<Role> roles = new LinkedHashSet<>();
        roles.add(role);

        User user = User.builder()
                .name(request.getUsername().trim())
                .email(request.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .roles(roles)
                .build();

        userRepository.save(user);
    }
}
