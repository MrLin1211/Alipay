package com.example.mallhome.repository;

import com.example.mallhome.entity.AdminSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface AdminSessionRepository extends JpaRepository<AdminSession, Long> {

    Optional<AdminSession> findByToken(String token);

    void deleteByToken(String token);

    void deleteByAdminUserId(Long adminUserId);

    @Modifying
    @Query("delete from AdminSession session where session.expiresAt < :now")
    void deleteExpired(LocalDateTime now);
}
