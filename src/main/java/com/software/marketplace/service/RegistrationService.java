package com.software.marketplace.service;

import com.software.marketplace.dto.auth.UserRegistrationRequestDto;

public interface RegistrationService {

    void registerUser(UserRegistrationRequestDto request);
}
