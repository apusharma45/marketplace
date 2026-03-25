package com.software.marketplace.service.impl;

import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.repository.UserRepository;
import com.software.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> getAllUsersForAdmin() {
        return userRepository.findAll()
                .stream()
                .map(user -> UserResponseDto.builder()
                        .id(user.getId())
                        .username(user.getName())
                        .email(user.getEmail())
                        .enabled(user.isEnabled())
                        .roles(user.getRoles().stream()
                                .map(role -> role.getName())
                                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new)))
                        .createdAt(null)
                        .updatedAt(null)
                        .build())
                .toList();
    }

    @Override
    @Transactional
    public void setUserEnabledByAdmin(Long userId, boolean enabled) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found."));
        user.setEnabled(enabled);
        userRepository.save(user);
    }
}
