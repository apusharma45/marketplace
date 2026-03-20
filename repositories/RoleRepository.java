package com.example.marketplace.repository;

import com.example.marketplace.entity.Role;
import com.example.marketplace.entity.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);

    boolean existsByName(RoleType name);
}
