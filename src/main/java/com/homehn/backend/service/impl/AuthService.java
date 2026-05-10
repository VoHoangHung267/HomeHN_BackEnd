package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.LoginRequest;
import com.homehn.backend.dto.request.RegisterRequest;
import com.homehn.backend.dto.request.ResetPasswordRequest;
import com.homehn.backend.entity.PasswordResetTokenEntity;
import com.homehn.backend.dto.response.AuthResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.RefreshTokenEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.PasswordResetTokenRepository;
import com.homehn.backend.repository.RefreshTokenRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.*;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository         userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final PasswordEncoder        passwordEncoder;
    private final AuthenticationManager  authManager;
    private final JwtUtil                jwtUtil;
    private final EmailService           emailService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshExp;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest req) {
        if (userRepo.existsByEmail(req.getEmail()))
            throw new AppException("Email �‘ã �‘ược sử dụng");

        var user = UserEntity.builder()
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName())
                .phone(req.getPhone())
                .role(req.getRole())
                .isActive(true)
                .build();

        userRepo.save(user);
        return buildAuthResponse(user);
    }

    public AuthResponse login(LoginRequest req) {
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(req.getEmail(), req.getPassword()));
        } catch (AuthenticationException e) {
            throw new AppException("Email hoặc mật khẩu không �‘úng");
        }

        var user = userRepo.findByEmail(req.getEmail())
                .orElseThrow(() -> new AppException("Tài khoản không t�“n tại"));

        if (!Boolean.TRUE.equals(user.getIsActive()))
            throw new AppException("Tài khoản �‘ã b�‹ khoá", 403);

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String token) {
        var rt = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new AppException("Refresh token không hợp l�‡", 401));

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepo.delete(rt);
            throw new AppException("Refresh token �‘ã hết hạn, vui lòng �‘�ƒng nhập lại", 401);
        }

        return buildAuthResponse(rt.getUser());
    }

    public void logout(Long userId) {
        refreshTokenRepo.deleteByUserId(userId);
    }

    public void forgotPassword(String email) {
        userRepo.findByEmail(email).ifPresent(user -> {
            passwordResetTokenRepo.deleteByUserId(user.getId());
            String token = UUID.randomUUID().toString();
            passwordResetTokenRepo.save(PasswordResetTokenEntity.builder()
                    .user(user)
                    .token(token)
                    .expiresAt(LocalDateTime.now().plusMinutes(30))
                    .build());
            String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
            emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
        });
    }

    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepo.findByToken(req.getToken())
                .orElseThrow(() -> new AppException("Liên kết �‘ặt lại mật khẩu không hợp l�‡", 400));
        if (resetToken.isUsed()) {
            throw new AppException("Liên kết �‘ặt lại mật khẩu �‘ã �‘ược sử dụng", 400);
        }
        if (resetToken.isExpired()) {
            throw new AppException("Liên kết �‘ặt lại mật khẩu �‘ã hết hạn", 400);
        }

        UserEntity user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepo.save(resetToken);
        refreshTokenRepo.deleteByUserId(user.getId());
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        var principal    = new UserPrincipal(user);
        var accessToken  = jwtUtil.generateAccessToken(principal);
        var refreshToken = UUID.randomUUID().toString();

        refreshTokenRepo.deleteByUserId(user.getId());
        refreshTokenRepo.save(RefreshTokenEntity.builder()
                .user(user)
                .token(refreshToken)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExp / 1000))
                .build());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .user(UserResponse.from(user))
                .build();
    }
}
