package com.mfra.website.module.auth;

import com.mfra.website.common.exception.RateLimitExceededException;
import com.mfra.website.common.security.JwtUtil;
import com.mfra.website.module.auth.dto.*;
import com.mfra.website.module.auth.entity.AdminUserEntity;
import com.mfra.website.module.auth.repository.AdminUserRepository;
import com.mfra.website.module.auth.service.AuthService;
import com.mfra.website.module.auth.service.RateLimitService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private AdminUserRepository adminUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtUtil jwtUtil;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private RateLimitService rateLimitService;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private AuthService authService;

    private AdminUserEntity testUser;
    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        testUser = new AdminUserEntity();
        testUser.setId(userId);
        testUser.setFullName("Test Admin");
        testUser.setEmail("admin@mfra.com");
        testUser.setPassword("$2a$10$hashedPassword");
        testUser.setRole("ADMIN");
        testUser.setIsActive(true);
    }

    @Nested
    @DisplayName("Login")
    class LoginTests {

        @Test
        @DisplayName("Should login successfully with valid credentials")
        void login_success() {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");
            request.setRememberDevice(false);

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@mfra.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("admin123", "$2a$10$hashedPassword")).thenReturn(true);
            when(jwtUtil.generateAccessToken(userId, "admin@mfra.com", "ADMIN")).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(userId, false)).thenReturn("refresh-token");
            when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
            when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            LoginResponse response = authService.login(request, "127.0.0.1");

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("access-token");
            assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600);
            assertThat(response.getUser()).isNotNull();
            assertThat(response.getUser().getEmail()).isEqualTo("admin@mfra.com");
            assertThat(response.getUser().getRole()).isEqualTo("ADMIN");

            verify(rateLimitService).checkRateLimit("127.0.0.1", "login", 5, 15);
            verify(adminUserRepository).save(testUser);
        }

        @Test
        @DisplayName("Should login with remember device and use extended refresh token TTL")
        void login_rememberDevice() {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");
            request.setRememberDevice(true);

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@mfra.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("admin123", "$2a$10$hashedPassword")).thenReturn(true);
            when(jwtUtil.generateAccessToken(userId, "admin@mfra.com", "ADMIN")).thenReturn("access-token");
            when(jwtUtil.generateRefreshToken(userId, true)).thenReturn("refresh-token");
            when(jwtUtil.getRememberExpirationMs()).thenReturn(2592000000L);
            when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            LoginResponse response = authService.login(request, "127.0.0.1");

            assertThat(response.getAccessToken()).isEqualTo("access-token");
            verify(jwtUtil).generateRefreshToken(userId, true);
            verify(jwtUtil).getRememberExpirationMs();
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for wrong email")
        void login_wrongEmail() {
            LoginRequest request = new LoginRequest();
            request.setEmail("nonexistent@mfra.com");
            request.setPassword("admin123");

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("nonexistent@mfra.com"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for wrong password")
        void login_wrongPassword() {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("wrongpass");

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@mfra.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("wrongpass", "$2a$10$hashedPassword")).thenReturn(false);

            assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for inactive user")
        void login_inactiveUser() {
            testUser.setIsActive(false);

            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@mfra.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("admin123", "$2a$10$hashedPassword")).thenReturn(true);

            assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Invalid email or password");
        }

        @Test
        @DisplayName("Should throw RateLimitExceededException when rate limited")
        void login_rateLimited() {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            doThrow(new RateLimitExceededException("Access temporarily restricted. Try again later."))
                    .when(rateLimitService).checkRateLimit("127.0.0.1", "login", 5, 15);

            assertThatThrownBy(() -> authService.login(request, "127.0.0.1"))
                    .isInstanceOf(RateLimitExceededException.class)
                    .hasMessage("Access temporarily restricted. Try again later.");
        }

        @Test
        @DisplayName("Should update lastLogin timestamp on successful login")
        void login_updatesLastLogin() {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            when(adminUserRepository.findByEmailIgnoreCaseAndDeletedAtIsNull("admin@mfra.com"))
                    .thenReturn(Optional.of(testUser));
            when(passwordEncoder.matches("admin123", "$2a$10$hashedPassword")).thenReturn(true);
            when(jwtUtil.generateAccessToken(any(), any(), any())).thenReturn("token");
            when(jwtUtil.generateRefreshToken(any(), anyBoolean())).thenReturn("refresh");
            when(jwtUtil.getRefreshExpirationMs()).thenReturn(604800000L);
            when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            Instant before = Instant.now();
            authService.login(request, "127.0.0.1");

            assertThat(testUser.getLastLogin()).isNotNull();
            assertThat(testUser.getLastLogin()).isAfterOrEqualTo(before);
            verify(adminUserRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("Refresh")
    class RefreshTests {

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        void refresh_success() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("valid-refresh-token");

            Claims claims = new DefaultClaims(Map.of(
                    "sub", userId.toString(),
                    "type", "refresh"
            ));

            when(jwtUtil.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtUtil.parseToken("valid-refresh-token")).thenReturn(claims);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:refresh:" + userId)).thenReturn("valid-refresh-token");
            when(adminUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));
            when(jwtUtil.generateAccessToken(userId, "admin@mfra.com", "ADMIN")).thenReturn("new-access-token");
            when(jwtUtil.getAccessExpirationMs()).thenReturn(3600000L);

            TokenResponse response = authService.refresh(request);

            assertThat(response).isNotNull();
            assertThat(response.getAccessToken()).isEqualTo("new-access-token");
            assertThat(response.getTokenType()).isEqualTo("Bearer");
            assertThat(response.getExpiresIn()).isEqualTo(3600);
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for invalid refresh token")
        void refresh_invalidToken() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("invalid-token");

            when(jwtUtil.validateToken("invalid-token")).thenReturn(false);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Invalid or expired refresh token");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for non-refresh token type")
        void refresh_wrongTokenType() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("access-token-not-refresh");

            Claims claims = new DefaultClaims(Map.of(
                    "sub", userId.toString(),
                    "type", "access"
            ));

            when(jwtUtil.validateToken("access-token-not-refresh")).thenReturn(true);
            when(jwtUtil.parseToken("access-token-not-refresh")).thenReturn(claims);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Invalid token type");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when stored token does not match")
        void refresh_tokenMismatch() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("refresh-token-1");

            Claims claims = new DefaultClaims(Map.of(
                    "sub", userId.toString(),
                    "type", "refresh"
            ));

            when(jwtUtil.validateToken("refresh-token-1")).thenReturn(true);
            when(jwtUtil.parseToken("refresh-token-1")).thenReturn(claims);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:refresh:" + userId)).thenReturn("different-token");

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Refresh token revoked or expired");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException when refresh token expired in Redis")
        void refresh_expiredInRedis() {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("expired-refresh-token");

            Claims claims = new DefaultClaims(Map.of(
                    "sub", userId.toString(),
                    "type", "refresh"
            ));

            when(jwtUtil.validateToken("expired-refresh-token")).thenReturn(true);
            when(jwtUtil.parseToken("expired-refresh-token")).thenReturn(claims);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:refresh:" + userId)).thenReturn(null);

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Refresh token revoked or expired");
        }

        @Test
        @DisplayName("Should throw InvalidCredentialsException for deactivated user on refresh")
        void refresh_inactiveUser() {
            testUser.setIsActive(false);

            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("valid-refresh-token");

            Claims claims = new DefaultClaims(Map.of(
                    "sub", userId.toString(),
                    "type", "refresh"
            ));

            when(jwtUtil.validateToken("valid-refresh-token")).thenReturn(true);
            when(jwtUtil.parseToken("valid-refresh-token")).thenReturn(claims);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("auth:refresh:" + userId)).thenReturn("valid-refresh-token");
            when(adminUserRepository.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> authService.refresh(request))
                    .isInstanceOf(AuthService.InvalidCredentialsException.class)
                    .hasMessage("Account is deactivated");
        }
    }

    @Nested
    @DisplayName("Logout")
    class LogoutTests {

        @Test
        @DisplayName("Should blacklist token and delete refresh token on logout")
        void logout_success() {
            String token = "valid-jwt-token";
            String authHeader = "Bearer " + token;

            when(jwtUtil.getJtiFromToken(token)).thenReturn("jti-123");
            when(jwtUtil.getRemainingExpirationMs(token)).thenReturn(1800000L);
            when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId.toString());
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);

            authService.logout(authHeader);

            verify(valueOperations).set(eq("auth:blacklist:jti-123"), eq("blacklisted"), eq(1800000L), eq(java.util.concurrent.TimeUnit.MILLISECONDS));
            verify(redisTemplate).delete("auth:refresh:" + userId);
        }

        @Test
        @DisplayName("Should handle null auth header gracefully")
        void logout_nullHeader() {
            authService.logout(null);

            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("Should handle non-Bearer auth header gracefully")
        void logout_nonBearerHeader() {
            authService.logout("Basic credentials");

            verifyNoInteractions(jwtUtil);
        }

        @Test
        @DisplayName("Should not blacklist already expired token")
        void logout_expiredToken() {
            String token = "expired-jwt";
            String authHeader = "Bearer " + token;

            when(jwtUtil.getJtiFromToken(token)).thenReturn("jti-expired");
            when(jwtUtil.getRemainingExpirationMs(token)).thenReturn(-1000L);
            when(jwtUtil.getUserIdFromToken(token)).thenReturn(userId.toString());

            authService.logout(authHeader);

            verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
            verify(redisTemplate).delete("auth:refresh:" + userId);
        }

        @Test
        @DisplayName("Should handle exception during logout gracefully")
        void logout_exceptionHandled() {
            String authHeader = "Bearer invalid-token";

            when(jwtUtil.getJtiFromToken("invalid-token")).thenThrow(new RuntimeException("Token parse failed"));

            // Should not throw
            authService.logout(authHeader);
        }
    }
}
