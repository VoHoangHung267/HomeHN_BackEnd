package com.homehn.backend.controller;

import com.homehn.backend.dto.response.*;
import com.homehn.backend.entity.*;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import lombok.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationRepository notifRepo;
    private final UserRepository         userRepo;

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<NotifResponse>>> getAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        List<NotifResponse> list = notifRepo.findByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream().map(n -> NotifResponse.builder()
                        .id(n.getId())
                        .type(n.getType())
                        .title(n.getTitle())
                        .message(n.getMessage())
                        .isRead(n.getIsRead())
                        .relatedId(n.getRelatedId())
                        .relatedType(resolveRelatedType(n.getType()))
                        .actionUrl(resolveActionUrl(n.getType(), n.getRelatedId()))
                        .createdAt(n.getCreatedAt())
                        .build())
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Long>> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(
                notifRepo.countByUser_IdAndIsRead(user.getId(), false)));
    }

    @PatchMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> readAll(
            @AuthenticationPrincipal UserPrincipal principal) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        notifRepo.markAllRead(user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã �‘ọc tất cả", null));
    }

    @PatchMapping("/{id}/read")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> readOne(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        NotificationEntity n = notifRepo.findById(id).orElseThrow();
        if (!n.getUser().getId().equals(user.getId()))
            throw new com.homehn.backend.exception.AppException("Không có quyền", 403);
        n.setIsRead(true);
        notifRepo.save(n);
        return ResponseEntity.ok(ApiResponse.ok("OK", null));
    }

    private String resolveRelatedType(String type) {
        return switch (type) {
            case "REPORT_RECEIVED", "REPORT_RESOLVED", "ADMIN_REPORT_RECEIVED", "REPORT_LANDLORD_RESPONDED" -> "REPORT";
            case "ROOM_APPROVED", "ROOM_REJECTED", "REVIEW_RECEIVED" -> "ROOM";
            case "APPOINTMENT_REQUESTED", "APPOINTMENT_UPDATED", "APPOINTMENT_CANCELLED" -> "APPOINTMENT";
            case "BOOKING_CREATED", "BOOKING_UPDATED", "BOOKING_DEPOSIT_PAID" -> "BOOKING";
            default -> null;
        };
    }

    private String resolveActionUrl(String type, Long relatedId) {
        if (relatedId == null) return null;
        return switch (type) {
            case "REPORT_RECEIVED", "REPORT_RESOLVED", "ADMIN_REPORT_RECEIVED", "REPORT_LANDLORD_RESPONDED" -> "/reports/" + relatedId;
            case "ROOM_APPROVED", "ROOM_REJECTED", "REVIEW_RECEIVED" -> "/rooms/" + relatedId;
            case "APPOINTMENT_REQUESTED", "APPOINTMENT_CANCELLED" -> "/landlord";
            case "APPOINTMENT_UPDATED" -> "/rooms";
            case "BOOKING_CREATED", "BOOKING_UPDATED", "BOOKING_DEPOSIT_PAID" -> "/bookings/" + relatedId;
            default -> null;
        };
    }

}
