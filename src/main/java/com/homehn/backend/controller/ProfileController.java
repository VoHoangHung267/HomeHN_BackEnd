package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ChangePasswordRequest;
import com.homehn.backend.dto.request.UpdateProfileRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getProfile(user.getId())));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdateProfileRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật thành công",
                profileService.updateProfile(user.getId(), req)
        ));
    }

    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ChangePasswordRequest req
    ) {
        profileService.changePassword(user.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Upload avatar thành công",
                profileService.uploadAvatar(user.getId(), file)
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                profileService.getStats(user.getId(), user.getRole())
        ));
    }
}
