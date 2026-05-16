package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.ChangePasswordRequest;
import com.homehn.backend.dto.request.UpdateProfileRequest;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.FavoriteRepository;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class ProfileService {

    private final UserRepository userRepo;
    private final FavoriteRepository favoriteRepo;
    private final ViewingAppointmentRepository appointmentRepo;
    private final RentalBookingRepository bookingRepo;
    private final RoomRepository roomRepo;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        return UserResponse.from(userRepo.findById(userId).orElseThrow());
    }

    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        user.setFullName(req.getFullName());
        user.setPhone(req.getPhone());
        return UserResponse.from(userRepo.save(user));
    }

    public void changePassword(Long userId, ChangePasswordRequest req) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new AppException("Mật khẩu hiện tại không đúng");
        }
        if (req.getCurrentPassword().equals(req.getNewPassword())) {
            throw new AppException("Mật khẩu mới phải khác mật khẩu hiện tại");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        userRepo.save(user);
    }

    public String uploadAvatar(Long userId, MultipartFile file) {
        UserEntity user = userRepo.findById(userId).orElseThrow();
        var uploaded = cloudinaryService.upload(file, "phongtro/avatars");
        String url = (String) uploaded.get("url");
        user.setAvatarUrl(url);
        userRepo.save(user);
        return url;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(Long userId, UserEntity.Role role) {
        long favorites = favoriteRepo.countByUser_Id(userId);
        long totalAppointments = role == UserEntity.Role.LANDLORD
                ? appointmentRepo.countByLandlord_Id(userId)
                : appointmentRepo.countBySeeker_Id(userId);
        long totalBookings = role == UserEntity.Role.LANDLORD
                ? bookingRepo.countByLandlord_Id(userId)
                : bookingRepo.countBySeeker_Id(userId);
        long totalRooms = role == UserEntity.Role.LANDLORD
                ? roomRepo.countByLandlordId(userId)
                : 0L;
        long totalViews = role == UserEntity.Role.LANDLORD
                ? roomRepo.findByLandlordIdOrderByCreatedAtDesc(userId).stream()
                .mapToLong(RoomEntity::getViewCount)
                .sum()
                : 0L;

        return Map.of(
                "totalFavorites", favorites,
                "totalAppointments", totalAppointments,
                "totalBookings", totalBookings,
                "totalRooms", totalRooms,
                "totalViews", totalViews
        );
    }
}
