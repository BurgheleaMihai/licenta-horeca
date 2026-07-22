package com.licenta.horeca.user.repository;

import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}