package com.example.mallhome.service;

import com.example.mallhome.domain.AdminLoginRequest;
import com.example.mallhome.domain.AdminLoginResponse;
import com.example.mallhome.domain.AdminUserView;
import com.example.mallhome.domain.CreateAdminUserRequest;
import com.example.mallhome.domain.UpdateAdminUserRequest;
import com.example.mallhome.entity.AdminSession;
import com.example.mallhome.entity.AdminUser;
import com.example.mallhome.repository.AdminSessionRepository;
import com.example.mallhome.repository.AdminUserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;

@Service
public class AdminAuthService {

    private static final int SESSION_HOURS = 12;

    private final AdminUserRepository adminUserRepository;
    private final AdminSessionRepository adminSessionRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminAuthService(AdminUserRepository adminUserRepository, AdminSessionRepository adminSessionRepository) {
        this.adminUserRepository = adminUserRepository;
        this.adminSessionRepository = adminSessionRepository;
    }

    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        AdminUser user = adminUserRepository.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled()) || !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "账号或密码错误");
        }

        adminSessionRepository.deleteExpired(LocalDateTime.now());

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(SESSION_HOURS);
        AdminSession session = new AdminSession();
        session.setAdminUser(user);
        session.setToken(token);
        session.setExpiresAt(expiresAt);
        adminSessionRepository.save(session);

        user.setLastLoginAt(LocalDateTime.now());
        adminUserRepository.save(user);

        return new AdminLoginResponse(token, expiresAt, AdminUserView.from(user));
    }

    @Transactional(readOnly = true)
    public AdminUser requireUser(String token) {
        if (!StringUtils.hasText(token)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "请先登录");
        }
        AdminSession session = adminSessionRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效"));
        if (session.getExpiresAt().isBefore(LocalDateTime.now()) || !Boolean.TRUE.equals(session.getAdminUser().getEnabled())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "登录已失效");
        }
        return session.getAdminUser();
    }

    @Transactional
    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            adminSessionRepository.deleteByToken(token);
        }
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> listUsers() {
        return adminUserRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(AdminUserView::from)
                .toList();
    }

    @Transactional
    public AdminUserView createUser(CreateAdminUserRequest request) {
        String username = request.getUsername().trim();
        if (adminUserRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "账号已存在");
        }

        AdminUser user = new AdminUser();
        user.setUsername(username);
        user.setDisplayName(request.getDisplayName().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setEnabled(request.getEnabled() == null || request.getEnabled());
        return AdminUserView.from(adminUserRepository.save(user));
    }

    @Transactional
    public AdminUserView updateUser(Long id, UpdateAdminUserRequest request) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在"));

        if (StringUtils.hasText(request.getDisplayName())) {
            user.setDisplayName(request.getDisplayName().trim());
        }
        if (StringUtils.hasText(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getEnabled() != null) {
            if (!request.getEnabled() && Boolean.TRUE.equals(user.getEnabled()) && adminUserRepository.countByEnabledTrue() <= 1) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要保留一个启用的管理员账号");
            }
            user.setEnabled(request.getEnabled());
            if (!request.getEnabled()) {
                adminSessionRepository.deleteByAdminUserId(id);
            }
        }
        return AdminUserView.from(adminUserRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long id) {
        AdminUser user = adminUserRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "账号不存在"));
        if (Boolean.TRUE.equals(user.getEnabled()) && adminUserRepository.countByEnabledTrue() <= 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "至少需要保留一个启用的管理员账号");
        }
        adminSessionRepository.deleteByAdminUserId(id);
        adminUserRepository.delete(user);
    }

    public String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

    private String generateToken() {
        byte[] bytes = new byte[48];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
