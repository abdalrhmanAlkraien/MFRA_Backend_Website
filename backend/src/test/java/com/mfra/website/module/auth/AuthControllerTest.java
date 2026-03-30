package com.mfra.website.module.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mfra.website.common.exception.RateLimitExceededException;
import com.mfra.website.common.security.JwtAuthFilter;
import com.mfra.website.common.security.JwtUtil;
import com.mfra.website.module.auth.controller.AuthController;
import com.mfra.website.module.auth.dto.LoginRequest;
import com.mfra.website.module.auth.dto.LoginResponse;
import com.mfra.website.module.auth.dto.RefreshRequest;
import com.mfra.website.module.auth.dto.TokenResponse;
import com.mfra.website.module.auth.dto.UserInfoResponse;
import com.mfra.website.module.auth.service.AuthService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private AuthService authService;
    @MockitoBean private JwtAuthFilter jwtAuthFilter;
    @MockitoBean private JwtUtil jwtUtil;
    @MockitoBean private StringRedisTemplate redisTemplate;

    @Nested
    @DisplayName("POST /api/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should return 200 with tokens on successful login")
        void login_success() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            UUID userId = UUID.randomUUID();
            LoginResponse response = LoginResponse.builder()
                    .accessToken("access-token")
                    .refreshToken("refresh-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .user(UserInfoResponse.builder()
                            .id(userId)
                            .fullName("Test Admin")
                            .email("admin@mfra.com")
                            .role("ADMIN")
                            .build())
                    .build();

            when(authService.login(any(LoginRequest.class), anyString())).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("access-token"))
                    .andExpect(jsonPath("$.data.refreshToken").value("refresh-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600))
                    .andExpect(jsonPath("$.data.user.email").value("admin@mfra.com"))
                    .andExpect(jsonPath("$.data.user.role").value("ADMIN"));
        }

        @Test
        @DisplayName("Should return 400 when email is blank")
        void login_blankEmail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("");
            request.setPassword("admin123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 when password is blank")
        void login_blankPassword() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 400 when email is invalid format")
        void login_invalidEmail() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("not-an-email");
            request.setPassword("admin123");

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 401 when credentials are invalid")
        void login_invalidCredentials() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("wrongpassword");

            when(authService.login(any(LoginRequest.class), anyString()))
                    .thenThrow(new AuthService.InvalidCredentialsException("Invalid email or password"));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        }

        @Test
        @DisplayName("Should return 429 when rate limited")
        void login_rateLimited() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            when(authService.login(any(LoginRequest.class), anyString()))
                    .thenThrow(new RateLimitExceededException("Access temporarily restricted. Try again later."));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("RATE_LIMIT_EXCEEDED"));
        }

        @Test
        @DisplayName("Should extract IP from X-Forwarded-For header")
        void login_xForwardedForIp() throws Exception {
            LoginRequest request = new LoginRequest();
            request.setEmail("admin@mfra.com");
            request.setPassword("admin123");

            LoginResponse response = LoginResponse.builder()
                    .accessToken("token")
                    .refreshToken("refresh")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .user(UserInfoResponse.builder()
                            .id(UUID.randomUUID())
                            .fullName("Admin")
                            .email("admin@mfra.com")
                            .role("ADMIN")
                            .build())
                    .build();

            when(authService.login(any(LoginRequest.class), eq("10.0.0.1"))).thenReturn(response);

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("X-Forwarded-For", "10.0.0.1, 10.0.0.2")
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());

            verify(authService).login(any(LoginRequest.class), eq("10.0.0.1"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/refresh")
    class RefreshEndpointTests {

        @Test
        @DisplayName("Should return 200 with new access token on valid refresh")
        void refresh_success() throws Exception {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("valid-refresh-token");

            TokenResponse response = TokenResponse.builder()
                    .accessToken("new-access-token")
                    .tokenType("Bearer")
                    .expiresIn(3600)
                    .build();

            when(authService.refresh(any(RefreshRequest.class))).thenReturn(response);

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                    .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                    .andExpect(jsonPath("$.data.expiresIn").value(3600));
        }

        @Test
        @DisplayName("Should return 400 when refresh token is blank")
        void refresh_blankToken() throws Exception {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("");

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
        }

        @Test
        @DisplayName("Should return 401 when refresh token is invalid")
        void refresh_invalidToken() throws Exception {
            RefreshRequest request = new RefreshRequest();
            request.setRefreshToken("invalid-token");

            when(authService.refresh(any(RefreshRequest.class)))
                    .thenThrow(new AuthService.InvalidCredentialsException("Invalid or expired refresh token"));

            mockMvc.perform(post("/api/auth/refresh")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"));
        }
    }

    @Nested
    @DisplayName("POST /api/auth/logout")
    class LogoutEndpointTests {

        @Test
        @DisplayName("Should return 200 on successful logout")
        void logout_success() throws Exception {
            mockMvc.perform(post("/api/auth/logout")
                            .header("Authorization", "Bearer some-jwt-token"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).logout("Bearer some-jwt-token");
        }

        @Test
        @DisplayName("Should return 200 even without Authorization header")
        void logout_noHeader() throws Exception {
            mockMvc.perform(post("/api/auth/logout"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));

            verify(authService).logout(null);
        }
    }
}
