package com.homehn.backend.controller;

import com.homehn.backend.dto.request.CreateViewingAppointmentRequest;
import com.homehn.backend.dto.request.UpdateViewingAppointmentRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ViewingAppointmentResponse;
import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.entity.ViewingAppointmentEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.repository.ViewingAppointmentRepository;
import com.homehn.backend.security.UserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class ViewingAppointmentController {

    private final ViewingAppointmentRepository appointmentRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;

    @PostMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> create(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateViewingAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity seeker = userRepo.findById(principal.getId()).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            throw new AppException("Ch�‰ có th�ƒ �‘ặt l�‹ch xem phòng �‘ang hi�ƒn th�‹");
        }
        if (room.getLandlord().getId().equals(seeker.getId())) {
            throw new AppException("Bạn không th�ƒ �‘ặt l�‹ch xem phòng của chính mình");
        }

        boolean existingActive = appointmentRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(
                roomId,
                seeker.getId(),
                List.of(ViewingAppointmentEntity.Status.PENDING,
                        ViewingAppointmentEntity.Status.ACCEPTED,
                        ViewingAppointmentEntity.Status.RESCHEDULED)
        );
        if (existingActive) {
            throw new AppException("Bạn �‘ã có yêu cầu xem phòng �‘ang chờ xử lý");
        }

        ViewingAppointmentEntity appointment = appointmentRepo.save(ViewingAppointmentEntity.builder()
                .room(room)
                .seeker(seeker)
                .landlord(room.getLandlord())
                .requestedAt(req.getRequestedAt())
                .message(req.getMessage())
                .build());

        notifyUser(room.getLandlord(), "APPOINTMENT_REQUESTED", "Có yêu cầu xem phòng m�›i",
                seeker.getFullName() + " mu�‘n xem phòng \"" + room.getTitle() + "\"",
                appointment.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã gửi yêu cầu xem phòng", ViewingAppointmentResponse.from(appointment)));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ViewingAppointmentResponse>>> myAppointments(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        List<ViewingAppointmentEntity> appointments = principal.getRole() == UserEntity.Role.LANDLORD
                ? appointmentRepo.findByLandlord_IdOrderByRequestedAtDesc(principal.getId())
                : appointmentRepo.findBySeeker_IdOrderByRequestedAtDesc(principal.getId());
        return ResponseEntity.ok(ApiResponse.ok(appointments.stream()
                .map(ViewingAppointmentResponse::from)
                .toList()));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateViewingAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ViewingAppointmentEntity appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy l�‹ch xem phòng", 404));

        boolean isOwner = appointment.getLandlord().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xử lý l�‹ch này", 403);
        }
        if (req.getStatus() == ViewingAppointmentEntity.Status.CANCELLED) {
            throw new AppException("Chủ nhà không th�ƒ huỷ thay người thuê");
        }
        if (req.getStatus() == ViewingAppointmentEntity.Status.RESCHEDULED) {
            if (req.getRequestedAt() == null) {
                throw new AppException("Vui lòng nhập thời gian �‘ề xuất m�›i");
            }
            appointment.setRequestedAt(req.getRequestedAt());
        }

        appointment.setStatus(req.getStatus());
        appointment.setLandlordNote(req.getNote());
        appointmentRepo.save(appointment);

        if (req.getStatus() == ViewingAppointmentEntity.Status.COMPLETED) {
            appointment.getRoom().setStatus(RoomEntity.RoomStatus.RENTED);
            roomRepo.save(appointment.getRoom());
        }

        notifyUser(appointment.getSeeker(), "APPOINTMENT_UPDATED", "L�‹ch xem phòng �‘ã �‘ược cập nhật",
                "L�‹ch xem phòng \"" + appointment.getRoom().getTitle() + "\" �‘ã chuy�ƒn sang "
                        + appointmentStatusLabel(appointment.getStatus()),
                appointment.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật l�‹ch xem phòng", ViewingAppointmentResponse.from(appointment)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ViewingAppointmentEntity appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy l�‹ch xem phòng", 404));
        if (!appointment.getSeeker().getId().equals(principal.getId())) {
            throw new AppException("Bạn không có quyền huỷ l�‹ch này", 403);
        }
        appointment.setStatus(ViewingAppointmentEntity.Status.CANCELLED);
        appointmentRepo.save(appointment);

        notifyUser(appointment.getLandlord(), "APPOINTMENT_CANCELLED", "Người thuê �‘ã huỷ l�‹ch xem phòng",
                appointment.getSeeker().getFullName() + " �‘ã huỷ l�‹ch xem phòng \""
                        + appointment.getRoom().getTitle() + "\"",
                appointment.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã huỷ l�‹ch xem phòng", ViewingAppointmentResponse.from(appointment)));
    }

    private void notifyUser(UserEntity user, String type, String title, String message, Long relatedId) {
        notifRepo.save(NotificationEntity.builder()
                .user(user)
                .type(type)
                .title(title)
                .message(message)
                .relatedId(relatedId)
                .build());
    }

    private String appointmentStatusLabel(ViewingAppointmentEntity.Status status) {
        return switch (status) {
            case ACCEPTED -> "�‘ã chấp nhận";
            case RESCHEDULED -> "�‘ề xuất giờ khác";
            case REJECTED -> "�‘ã từ ch�‘i";
            case CANCELLED -> "�‘ã huỷ";
            case COMPLETED -> "�‘ã hoàn tất";
            default -> "�‘ang chờ xử lý";
        };
    }
}
