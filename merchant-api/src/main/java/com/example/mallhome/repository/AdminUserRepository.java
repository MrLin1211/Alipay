package com.example.mallhome.repository;

import com.example.mallhome.entity.AdminUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface AdminUserRepository extends JpaRepository<AdminUser, Long>, JpaSpecificationExecutor<AdminUser> {

    Optional<AdminUser> findByUsername(String username);

    boolean existsByUsername(String username);

    long countByEnabledTrue();
}
