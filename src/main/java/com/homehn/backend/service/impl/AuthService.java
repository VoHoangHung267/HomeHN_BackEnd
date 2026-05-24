package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.LoginRequest;
import com.homehn.backend.dto.request.RegisterRequest;
import com.homehn.backend.dto.request.ResetPasswordRequest;
import com.homehn.backend.dto.response.AuthResponse;
import com.homehn.backend.dto.response.EmailAvailabilityResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.PasswordResetTokenEntity;
import com.homehn.backend.entity.RefreshTokenEntity;
import com.homehn.backend.entity.RegistrationVerificationCodeEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.PasswordResetTokenRepository;
import com.homehn.backend.repository.RefreshTokenRepository;
import com.homehn.backend.repository.RegistrationVerificationCodeRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepo;
    private final RefreshTokenRepository refreshTokenRepo;
    private final PasswordResetTokenRepository passwordResetTokenRepo;
    private final RegistrationVerificationCodeRepository registrationCodeRepo;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authManager;
    private final JwtUtil jwtUtil;
    private final EmailService emailService;

    @Value("${app.jwt.refresh-token-expiration}")
    private long refreshExp;

    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    public AuthResponse register(RegisterRequest req) {
        String normalizedEmail = normalizeEmail(req.getEmail());
        if (userRepo.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AppException("Email đã được sử dụng");
        }

        RegistrationVerificationCodeEntity verification = registrationCodeRepo
                .findTopByEmailIgnoreCaseOrderByCreatedAtDesc(normalizedEmail)
                .orElseThrow(() -> new AppException("Vui lòng yêu cầu mã xác thực email trước"));

        if (verification.isUsed()) {
            throw new AppException("Mã xác thực này đã được sử dụng");
        }
        if (verification.isExpired()) {
            throw new AppException("Mã xác thực đã hết hạn. Vui lòng gửi lại mã mới");
        }
        if (!verification.getCode().equals(req.getVerificationCode().trim())) {
            throw new AppException("Mã xác thực không đúng");
        }

        UserEntity user = UserEntity.builder()
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .fullName(req.getFullName().trim())
                .phone(req.getPhone().trim())
                .role(req.getRole())
                .isActive(true)
                .build();

        userRepo.save(user);
        verification.setUsedAt(LocalDateTime.now());
        registrationCodeRepo.save(verification);
        return buildAuthResponse(user);
    }

    public EmailAvailabilityResponse checkEmailAvailability(String email) {
        return new EmailAvailabilityResponse(userRepo.existsByEmailIgnoreCase(normalizeEmail(email)));
    }

    public void sendRegistrationVerificationCode(String email) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepo.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new AppException("Email đã được sử dụng");
        }

        registrationCodeRepo.deleteByEmailIgnoreCase(normalizedEmail);

        String verificationCode = String.format("%06d", ThreadLocalRandom.current().nextInt(0, 1_000_000));
        registrationCodeRepo.save(RegistrationVerificationCodeEntity.builder()
                .email(normalizedEmail)
                .code(verificationCode)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build());

        emailService.sendRegistrationVerificationCode(normalizedEmail, verificationCode);
    }

    public AuthResponse login(LoginRequest req) {
        String normalizedEmail = normalizeEmail(req.getEmail());
        try {
            authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(normalizedEmail, req.getPassword()));
        } catch (AuthenticationException e) {
            throw new AppException("Email hoặc mật khẩu không đúng");
        }

        UserEntity user = userRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException("Tài khoản không tồn tại"));

        if (!Boolean.TRUE.equals(user.getIsActive())) {
            throw new AppException("Tài khoản đã bị khoá", 403);
        }

        return buildAuthResponse(user);
    }

    public AuthResponse refreshToken(String token) {
        RefreshTokenEntity rt = refreshTokenRepo.findByToken(token)
                .orElseThrow(() -> new AppException("Refresh token không hợp lệ", 401));

        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepo.delete(rt);
            throw new AppException("Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại", 401);
        }

        return buildAuthResponse(rt.getUser());
    }

    public void logout(Long userId) {
        refreshTokenRepo.deleteByUserId(userId);
    }

    public void forgotPassword(String email) {
        String normalizedEmail = normalizeEmail(email);
        UserEntity user = userRepo.findByEmailIgnoreCase(normalizedEmail)
                .orElseThrow(() -> new AppException("Email không tồn tại trong hệ thống", 404));

        passwordResetTokenRepo.deleteByUserId(user.getId());
        String token = UUID.randomUUID().toString();
        passwordResetTokenRepo.save(PasswordResetTokenEntity.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .build());
        String resetLink = frontendUrl + "/auth/reset-password?token=" + token;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    public void resetPassword(ResetPasswordRequest req) {
        PasswordResetTokenEntity resetToken = passwordResetTokenRepo.findByToken(req.getToken())
                .orElseThrow(() -> new AppException("Liên kết đặt lại mật khẩu không hợp lệ", 400));
        if (resetToken.isUsed()) {
            throw new AppException("Liên kết đặt lại mật khẩu đã được sử dụng", 400);
        }
        if (resetToken.isExpired()) {
            throw new AppException("Liên kết đặt lại mật khẩu đã hết hạn", 400);
        }

        UserEntity user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);

        resetToken.setUsedAt(LocalDateTime.now());
        passwordResetTokenRepo.save(resetToken);
        refreshTokenRepo.deleteByUserId(user.getId());
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse buildAuthResponse(UserEntity user) {
        UserPrincipal principal = new UserPrincipal(user);
        String accessToken = jwtUtil.generateAccessToken(principal);
        String refreshToken = UUID.randomUUID().toString();

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
