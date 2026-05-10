package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ChangePasswordRequest;
import com.homehn.backend.dto.request.UpdateProfileRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.FavoriteRepository;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.repository.ViewingAppointmentRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository     userRepo;
    private final FavoriteRepository favoriteRepo;
    private final ViewingAppointmentRepository appointmentRepo;
    private final RentalBookingRepository bookingRepo;
    private final RoomRepository roomRepo;
    private final PasswordEncoder    passwordEncoder;
    private final CloudinaryService  cloudinaryService;

    // ── GET profile ─────────────────────────────────────────
    @GetMapping
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.ok(UserResponse.from(
                userRepo.findById(user.getId()).orElseThrow())));
    }

    // ── PUT update info ──────────────────────────────────────
    @PutMapping
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody UpdateProfileRequest req) {
        UserEntity u = userRepo.findById(user.getId()).orElseThrow();
        u.setFullName(req.getFullName());
        u.setPhone(req.getPhone());
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công", UserResponse.from(userRepo.save(u))));
    }

    // ── PATCH change password ────────────────────────────────
    @PatchMapping("/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @AuthenticationPrincipal UserPrincipal user,
            @Valid @RequestBody ChangePasswordRequest req) {
        UserEntity u = userRepo.findById(user.getId()).orElseThrow();
        if (!passwordEncoder.matches(req.getCurrentPassword(), u.getPasswordHash()))
            throw new AppException("Mật khẩu hiện tại không đúng");
        if (req.getCurrentPassword().equals(req.getNewPassword()))
            throw new AppException("Mật khẩu mới phải khác mật khẩu hiện tại");
        u.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(u);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công", null));
    }

    // ── POST upload avatar ───────────────────────────────────
    @PostMapping("/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(
            @AuthenticationPrincipal UserPrincipal user,
            @RequestParam("file") MultipartFile file) {
        UserEntity u = userRepo.findById(user.getId()).orElseThrow();
        var uploaded = cloudinaryService.upload(file, "phongtro/avatars");
        String url = (String) uploaded.get("url");
        u.setAvatarUrl(url);
        userRepo.save(u);
        return ResponseEntity.ok(ApiResponse.ok("Upload avatar thành công", url));
    }

    // ── GET stats ────────────────────────────────────────────
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStats(
            @AuthenticationPrincipal UserPrincipal user) {
        long favorites = favoriteRepo.countByUser_Id(user.getId());
        long totalAppointments = user.getRole() == UserEntity.Role.LANDLORD
                ? appointmentRepo.countByLandlord_Id(user.getId())
                : appointmentRepo.countBySeeker_Id(user.getId());
        long totalBookings = user.getRole() == UserEntity.Role.LANDLORD
                ? bookingRepo.countByLandlord_Id(user.getId())
                : bookingRepo.countBySeeker_Id(user.getId());
        long totalRooms = user.getRole() == UserEntity.Role.LANDLORD
                ? roomRepo.countByLandlordId(user.getId())
                : 0L;
        long totalViews = user.getRole() == UserEntity.Role.LANDLORD
                ? roomRepo.findByLandlordIdOrderByCreatedAtDesc(user.getId()).stream()
                    .mapToLong(room -> room.getViewCount())
                    .sum()
                : 0L;

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalFavorites", favorites,
                "totalAppointments", totalAppointments,
                "totalBookings", totalBookings,
                "totalRooms", totalRooms,
                "totalViews", totalViews
        )));
    }
}
