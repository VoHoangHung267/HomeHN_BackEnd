package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ForgotPasswordRequest;
import com.homehn.backend.dto.request.LoginRequest;
import com.homehn.backend.dto.request.RefreshTokenRequest;
import com.homehn.backend.dto.request.RegisterRequest;
import com.homehn.backend.dto.request.ResetPasswordRequest;
import com.homehn.backend.dto.request.SendVerificationCodeRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.AuthResponse;
import com.homehn.backend.dto.response.EmailAvailabilityResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Đăng ký thành công", authService.register(req)));
    }

    @GetMapping("/check-email")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmail(@RequestParam String email) {
        return ResponseEntity.ok(ApiResponse.ok(authService.checkEmailAvailability(email)));
    }

    @PostMapping("/send-verification-code")
    public ResponseEntity<ApiResponse<Void>> sendVerificationCode(@Valid @RequestBody SendVerificationCodeRequest req) {
        authService.sendRegistrationVerificationCode(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi mã xác thực đến email của bạn", null));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Đăng nhập thành công", authService.login(req)));
    }

    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.refreshToken(req.getRefreshToken())));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã gửi liên kết đặt lại mật khẩu tới email của bạn", null));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponse.ok("Đặt lại mật khẩu thành công", null));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(@AuthenticationPrincipal UserPrincipal user) {
        authService.logout(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đăng xuất thành công", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.fromPrincipal(user)));
    }
}
