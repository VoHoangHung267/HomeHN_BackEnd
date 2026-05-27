package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.CreateViewingAppointmentRequest;
import com.homehn.backend.dto.request.UpdateViewingAppointmentRequest;
import com.homehn.backend.dto.response.ViewingAppointmentResponse;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.entity.ViewingAppointmentEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.repository.ViewingAppointmentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ViewingAppointmentService {

    private final ViewingAppointmentRepository appointmentRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    public ViewingAppointmentResponse create(Long roomId, CreateViewingAppointmentRequest req, Long seekerId) {
        UserEntity seeker = userRepo.findById(seekerId).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE
                && room.getStatus() != RoomEntity.RoomStatus.AVAILABLE_SOON) {
            throw new AppException("Chỉ có thể đặt lịch xem với phòng đang hiển thị hoặc sắp trống");
        }
        if (room.getLandlord().getId().equals(seeker.getId())) {
            throw new AppException("Bạn không thể đặt lịch xem phòng của chính mình");
        }

        boolean existingActive = appointmentRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(
                roomId,
                seeker.getId(),
                List.of(
                        ViewingAppointmentEntity.Status.PENDING,
                        ViewingAppointmentEntity.Status.ACCEPTED,
                        ViewingAppointmentEntity.Status.RESCHEDULED
                )
        );
        if (existingActive) {
            throw new AppException("Bạn đã có yêu cầu xem phòng đang chờ xử lý");
        }

        ViewingAppointmentEntity appointment = appointmentRepo.save(ViewingAppointmentEntity.builder()
                .room(room)
                .seeker(seeker)
                .landlord(room.getLandlord())
                .requestedAt(req.getRequestedAt())
                .message(req.getMessage())
                .build());

        notificationService.notifyUser(
                room.getLandlord(),
                "APPOINTMENT_REQUESTED",
                "Có yêu cầu xem phòng mới",
                seeker.getFullName() + " muốn xem phòng \"" + room.getTitle() + "\"",
                appointment.getId()
        );

        return ViewingAppointmentResponse.from(appointment);
    }

    @Transactional(readOnly = true)
    public List<ViewingAppointmentResponse> getMyAppointments(Long userId, UserEntity.Role role) {
        List<ViewingAppointmentEntity> appointments = role == UserEntity.Role.LANDLORD
                ? appointmentRepo.findByLandlord_IdOrderByRequestedAtDesc(userId)
                : appointmentRepo.findBySeeker_IdOrderByRequestedAtDesc(userId);
        return appointments.stream()
                .map(ViewingAppointmentResponse::from)
                .toList();
    }

    public ViewingAppointmentResponse updateStatus(
            Long id,
            UpdateViewingAppointmentRequest req,
            Long actorId,
            UserEntity.Role actorRole
    ) {
        ViewingAppointmentEntity appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy lịch xem phòng", 404));

        boolean isOwner = appointment.getLandlord().getId().equals(actorId);
        boolean isAdmin = actorRole == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xử lý lịch này", 403);
        }
        if (req.getStatus() == ViewingAppointmentEntity.Status.CANCELLED) {
            throw new AppException("Chủ nhà không thể huỷ thay người thuê");
        }
        if (req.getStatus() == ViewingAppointmentEntity.Status.RESCHEDULED) {
            if (req.getRequestedAt() == null) {
                throw new AppException("Vui lòng nhập thời gian đề xuất mới");
            }
            appointment.setRequestedAt(req.getRequestedAt());
        }

        appointment.setStatus(req.getStatus());
        appointment.setLandlordNote(req.getNote());
        appointmentRepo.save(appointment);

        notificationService.notifyUser(
                appointment.getSeeker(),
                "APPOINTMENT_UPDATED",
                "Lịch xem phòng đã được cập nhật",
                "Lịch xem phòng \"" + appointment.getRoom().getTitle() + "\" đã chuyển sang "
                        + appointmentStatusLabel(appointment.getStatus()),
                appointment.getId()
        );

        return ViewingAppointmentResponse.from(appointment);
    }

    public ViewingAppointmentResponse cancel(Long id, Long seekerId) {
        ViewingAppointmentEntity appointment = appointmentRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy lịch xem phòng", 404));
        if (!appointment.getSeeker().getId().equals(seekerId)) {
            throw new AppException("Bạn không có quyền huỷ lịch này", 403);
        }
        appointment.setStatus(ViewingAppointmentEntity.Status.CANCELLED);
        appointmentRepo.save(appointment);

        notificationService.notifyUser(
                appointment.getLandlord(),
                "APPOINTMENT_CANCELLED",
                "Người thuê đã huỷ lịch xem phòng",
                appointment.getSeeker().getFullName() + " đã huỷ lịch xem phòng \"" + appointment.getRoom().getTitle() + "\"",
                appointment.getId()
        );

        return ViewingAppointmentResponse.from(appointment);
    }

    private String appointmentStatusLabel(ViewingAppointmentEntity.Status status) {
        return switch (status) {
            case ACCEPTED -> "đã chấp nhận";
            case RESCHEDULED -> "đề xuất giờ khác";
            case REJECTED -> "đã từ chối";
            case CANCELLED -> "đã huỷ";
            case COMPLETED -> "đã hoàn tất";
            default -> "đang chờ xử lý";
        };
    }
}
