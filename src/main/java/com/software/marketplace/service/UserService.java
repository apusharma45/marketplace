package com.software.marketplace.service;

import com.software.marketplace.dto.user.UserResponseDto;

import java.util.List;

public interface UserService {

    List<UserResponseDto> getAllUsersForAdmin();
}
