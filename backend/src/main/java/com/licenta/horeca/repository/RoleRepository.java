package com.licenta.horeca.repository;

import com.licenta.horeca.entity.Role;
import com.licenta.horeca.enums.RoleType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleType name);
}