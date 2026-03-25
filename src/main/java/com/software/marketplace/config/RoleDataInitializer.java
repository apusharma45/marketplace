package com.software.marketplace.config;

import com.software.marketplace.entity.Role;
import com.software.marketplace.entity.enums.RoleType;
import com.software.marketplace.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    @Override
    public void run(String... args) {
        ensureRoleExists(RoleType.ROLE_ADMIN);
        ensureRoleExists(RoleType.ROLE_SELLER);
        ensureRoleExists(RoleType.ROLE_BUYER);
    }

    private void ensureRoleExists(RoleType roleType) {
        roleRepository.findByName(roleType.name())
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleType.name()).build()));
    }
}
