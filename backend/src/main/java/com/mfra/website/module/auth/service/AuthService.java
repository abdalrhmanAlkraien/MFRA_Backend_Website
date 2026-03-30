package com.mfra.website.module.auth.service;

import com.mfra.website.common.security.JwtUtil;
import com.mfra.website.module.auth.dto.*;
import com.mfra.website.module.auth.entity.AdminUserEntity;
import com.mfra.website.module.auth.repository.AdminUserRepository;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate redisTemplate;
    private final RateLimitService rateLimitService;

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";
    private static final String REFRESH_PREFIX = "auth:refresh:";

    @Transactional
    public LoginResponse login(LoginRequest request, String ipAddress) {
        // Check rate limit: 5 attempts per 15 minutes per IP
//        rateLimitService.checkRateLimit(ipAddress, "login", 5, 15);

        // Find user by email (case-insensitive)
        AdminUserEntity user = adminUserRepository
                .findByEmailIgnoreCaseAndDeletedAtIsNull(request.getEmail())
                .orElse(null);

        if (user == null || !passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Failed login attempt for email={} from IP={}", request.getEmail(), ipAddress);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        if (!user.getIsActive()) {
            log.warn("Inactive user login attempt: email={} from IP={}", request.getEmail(), ipAddress);
            throw new InvalidCredentialsException("Invalid email or password");
        }

        // Update last login
        user.setLastLogin(Instant.now());
        adminUserRepository.save(user);

        // Generate tokens
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), request.isRememberDevice());

        // Store refresh token in Redis
        long refreshTtl = request.isRememberDevice()
                ? jwtUtil.getRememberExpirationMs()
                : jwtUtil.getRefreshExpirationMs();
        redisTemplate.opsForValue().set(
                REFRESH_PREFIX + user.getId(),
                refreshToken,
                refreshTtl,
                TimeUnit.MILLISECONDS
        );

        log.info("Successful login: userId={}, email={}, IP={}", user.getId(), user.getEmail(), ipAddress);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessExpirationMs() / 1000)
                .user(toUserInfo(user))
                .build();
    }

    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.validateToken(refreshToken)) {
            throw new InvalidCredentialsException("Invalid or expired refresh token");
        }

        Claims claims = jwtUtil.parseToken(refreshToken);
        String type = claims.get("type", String.class);
        if (!"refresh".equals(type)) {
            throw new InvalidCredentialsException("Invalid token type");
        }

        UUID userId = UUID.fromString(claims.getSubject());

        // Verify refresh token matches stored one
        String storedToken = redisTemplate.opsForValue().get(REFRESH_PREFIX + userId);
        if (storedToken == null || !storedToken.equals(refreshToken)) {
            throw new InvalidCredentialsException("Refresh token revoked or expired");
        }

        AdminUserEntity user = adminUserRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new InvalidCredentialsException("User not found"));

        if (!user.getIsActive()) {
            throw new InvalidCredentialsException("Account is deactivated");
        }

        String newAccessToken = jwtUtil.generateAccessToken(user.getId(), user.getEmail(), user.getRole());

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getAccessExpirationMs() / 1000)
                .build();
    }

    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return;
        }
        String token = authHeader.substring(7);

        try {
            String jti = jwtUtil.getJtiFromToken(token);
            long remainingMs = jwtUtil.getRemainingExpirationMs(token);

            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        BLACKLIST_PREFIX + jti,
                        "blacklisted",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
            }

            String userId = jwtUtil.getUserIdFromToken(token);
            redisTemplate.delete(REFRESH_PREFIX + userId);

            log.info("User logged out: userId={}", userId);
        } catch (Exception e) {
            log.debug("Logout with invalid token: {}", e.getMessage());
        }
    }

    private UserInfoResponse toUserInfo(AdminUserEntity user) {
        return UserInfoResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .avatarUrl(user.getAvatarUrl())
                .build();
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException(String message) {
            super(message);
        }
    }
}
