package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.RejectRoomRequest;
import com.homehn.backend.dto.request.ResolveReportRequest;
import com.homehn.backend.dto.response.ReportResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.dto.response.StatsResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ReportRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminService {

    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final ReportRepository reportRepo;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public StatsResponse getStats() {
        return StatsResponse.builder()
                .totalUsers(userRepo.count())
                .totalRooms(roomRepo.count())
                .pendingRooms(roomRepo.countByStatus(RoomEntity.RoomStatus.PENDING))
                .totalReports(reportRepo.count())
                .build();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getPendingRooms() {
        return roomRepo.findByStatusOrderByCreatedAtDesc(RoomEntity.RoomStatus.PENDING)
                .stream()
                .map(RoomResponse::from)
                .toList();
    }

    public void approveRoom(Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(RoomEntity.RoomStatus.ACTIVE);
        roomRepo.save(room);

        notificationService.notifyUser(
                room.getLandlord(),
                "ROOM_APPROVED",
                "Phòng đã được duyệt",
                "Phòng \"" + room.getTitle() + "\" đã được duyệt và hiển thị.",
                room.getId()
        );
    }

    public void rejectRoom(Long id, RejectRoomRequest body) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(RoomEntity.RoomStatus.REJECTED);
        roomRepo.save(room);

        String reason = body != null && body.getReason() != null && !body.getReason().isBlank()
                ? " Lý do: " + body.getReason()
                : "";
        notificationService.notifyUser(
                room.getLandlord(),
                "ROOM_REJECTED",
                "Phòng bị từ chối",
                "Phòng \"" + room.getTitle() + "\" đã bị từ chối." + reason,
                room.getId()
        );
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(UserResponse::from)
                .toList();
    }

    public boolean toggleUser(Long id) {
        UserEntity user = userRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy user", 404));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepo.save(user);
        return Boolean.TRUE.equals(user.getIsActive());
    }

    @Transactional(readOnly = true)
    public Page<RoomResponse> getAllRooms(
            String keyword,
            String ward,
            RoomEntity.RoomStatus status,
            Long landlordId,
            int page,
            int size
    ) {
        Specification<RoomEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), kw),
                        cb.like(cb.lower(root.get("address")), kw),
                        cb.like(cb.lower(root.get("ward")), kw)
                ));
            }
            if (ward != null && !ward.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("ward")), "%" + ward.toLowerCase() + "%"));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (landlordId != null) {
                predicates.add(cb.equal(root.get("landlord").get("id"), landlordId));
            }
            query.orderBy(cb.desc(root.get("createdAt")));
            return cb.and(predicates.toArray(Predicate[]::new));
        };

        return roomRepo.findAll(spec, PageRequest.of(page, size)).map(RoomResponse::from);
    }

    public boolean toggleHidden(Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(room.getStatus() == RoomEntity.RoomStatus.HIDDEN
                ? RoomEntity.RoomStatus.ACTIVE
                : RoomEntity.RoomStatus.HIDDEN);
        roomRepo.save(room);
        return room.getStatus() == RoomEntity.RoomStatus.HIDDEN;
    }

    public void deleteRoom(Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        roomRepo.delete(room);
    }

    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports(ReportEntity.Status status) {
        return reportRepo.findByStatusOrderByCreatedAtDesc(status).stream()
                .map(this::toReportResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ReportResponse getReport(Long id) {
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));
        return toReportResponse(report);
    }

    public void resolveReport(Long id, ResolveReportRequest body) {
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));
        report.setStatus(body.getStatus());
        report.setAdminNote(body.getNote());
        reportRepo.save(report);

        if (body.getStatus() == ReportEntity.Status.RESOLVED) {
            report.getRoom().setStatus(RoomEntity.RoomStatus.HIDDEN);
            roomRepo.save(report.getRoom());
        }

        String statusText = reportStatusLabel(body.getStatus());
        notificationService.notifyUser(
                report.getReporter(),
                "REPORT_RESOLVED",
                "Báo cáo của bạn đã được xử lý",
                "Báo cáo phòng \"" + report.getRoom().getTitle() + "\" đã được cập nhật: " + statusText,
                report.getId()
        );
        notificationService.notifyUser(
                report.getRoom().getLandlord(),
                "REPORT_RESOLVED",
                "Báo cáo liên quan đến phòng của bạn đã được xử lý",
                "Phòng \"" + report.getRoom().getTitle() + "\" đã được cập nhật trạng thái báo cáo: " + statusText,
                report.getId()
        );
    }

    private ReportResponse toReportResponse(ReportEntity report) {
        return ReportResponse.builder()
                .id(report.getId())
                .roomId(report.getRoom().getId())
                .roomTitle(report.getRoom().getTitle())
                .reporterName(report.getReporter().getFullName())
                .reporterEmail(report.getReporter().getEmail())
                .reason(report.getReason())
                .status(report.getStatus())
                .adminNote(report.getAdminNote())
                .landlordResponseType(report.getLandlordResponseType())
                .landlordResponseNote(report.getLandlordResponseNote())
                .landlordRespondedAt(report.getLandlordRespondedAt())
                .createdAt(report.getCreatedAt())
                .build();
    }

    private String reportStatusLabel(ReportEntity.Status status) {
        return switch (status) {
            case REVIEWED -> "Đang xem xét";
            case RESOLVED -> "Đã xử lý";
            case DISMISSED -> "Bỏ qua";
            default -> "Chờ xử lý";
        };
    }
}
