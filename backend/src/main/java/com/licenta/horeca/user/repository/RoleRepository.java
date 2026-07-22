package com.licenta.horeca.user.repository;

import com.licenta.horeca.user.entity.Role;
import com.licenta.horeca.user.enums.RoleType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleType name);
}