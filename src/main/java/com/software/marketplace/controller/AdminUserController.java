package com.software.marketplace.controller;

import com.software.marketplace.dto.user.UserResponseDto;
import com.software.marketplace.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final UserService userService;

    @GetMapping
    public List<UserResponseDto> getAllUsersForAdmin() {
        return userService.getAllUsersForAdmin();
    }
}
