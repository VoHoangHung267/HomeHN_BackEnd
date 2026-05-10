package com.homehn.backend.controller;

import com.homehn.backend.dto.request.RejectRoomRequest;
import com.homehn.backend.dto.request.ResolveReportRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ReportResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.dto.response.StatsResponse;
import com.homehn.backend.dto.response.UserResponse;
import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.ReportRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class AdminController {

    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final ReportRepository reportRepo;
    private final NotificationRepository notifRepo;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(StatsResponse.builder()
                .totalUsers(userRepo.count())
                .totalRooms(roomRepo.count())
                .pendingRooms(roomRepo.countByStatus(RoomEntity.RoomStatus.PENDING))
                .totalReports(reportRepo.count())
                .build()));
    }

    @GetMapping("/rooms/pending")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> pendingRooms() {
        var rooms = roomRepo.findByStatusOrderByCreatedAtDesc(RoomEntity.RoomStatus.PENDING)
                .stream().map(RoomResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(rooms));
    }

    @PatchMapping("/rooms/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRoom(@PathVariable Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(RoomEntity.RoomStatus.ACTIVE);
        roomRepo.save(room);

        notifyUser(room.getLandlord(), "ROOM_APPROVED", "Phòng �‘ã �‘ược duy�‡t",
                "Phòng \"" + room.getTitle() + "\" �‘ã �‘ược duy�‡t và hi�ƒn th�‹.", room.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã duy�‡t phòng", null));
    }

    @PatchMapping("/rooms/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRoom(
            @PathVariable Long id, @RequestBody RejectRoomRequest body
    ) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(RoomEntity.RoomStatus.REJECTED);
        roomRepo.save(room);

        String reason = body != null && body.getReason() != null && !body.getReason().isBlank()
                ? " Lý do: " + body.getReason()
                : "";
        notifyUser(room.getLandlord(), "ROOM_REJECTED", "Phòng b�‹ từ ch�‘i",
                "Phòng \"" + room.getTitle() + "\" �‘ã b�‹ từ ch�‘i." + reason, room.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã từ ch�‘i phòng", null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> allUsers() {
        var users = userRepo.findAllByOrderByCreatedAtDesc().stream().map(UserResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        UserEntity user = userRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy user", 404));
        user.setIsActive(!Boolean.TRUE.equals(user.getIsActive()));
        userRepo.save(user);
        return ResponseEntity.ok(ApiResponse.ok(
                Boolean.TRUE.equals(user.getIsActive()) ? "Đã m�Ÿ khoá" : "Đã khoá tài khoản", null));
    }

    @GetMapping("/rooms/all")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> allRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) RoomEntity.RoomStatus status,
            @RequestParam(required = false) Long landlordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Specification<RoomEntity> spec = (root, query, cb) -> {
            var predicates = new ArrayList<Predicate>();
            if (keyword != null && !keyword.isBlank()) {
                String kw = "%" + keyword.toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), kw),
                        cb.like(cb.lower(root.get("address")), kw),
                        cb.like(cb.lower(root.get("district")), kw)
                ));
            }
            if (district != null) {
                predicates.add(cb.like(cb.lower(root.get("district")), "%" + district.toLowerCase() + "%"));
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
        var pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(
                roomRepo.findAll(spec, pageable).map(RoomResponse::from)));
    }

    @PatchMapping("/rooms/{id}/toggle-hidden")
    public ResponseEntity<ApiResponse<Void>> toggleHidden(@PathVariable Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        room.setStatus(room.getStatus() == RoomEntity.RoomStatus.HIDDEN
                ? RoomEntity.RoomStatus.ACTIVE
                : RoomEntity.RoomStatus.HIDDEN);
        roomRepo.save(room);
        String msg = room.getStatus() == RoomEntity.RoomStatus.HIDDEN ? "Đã ẩn phòng" : "Đã hi�‡n phòng";
        return ResponseEntity.ok(ApiResponse.ok(msg, null));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));
        roomRepo.delete(room);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá phòng", null));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> allReports(
            @RequestParam(defaultValue = "PENDING") ReportEntity.Status status) {
        var list = reportRepo.findByStatusOrderByCreatedAtDesc(status)
                .stream().map(this::toReportResponse).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long id) {
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));
        return ResponseEntity.ok(ApiResponse.ok(toReportResponse(report)));
    }

    @PatchMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable Long id,
            @RequestBody ResolveReportRequest body) {
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
        notifyUser(report.getReporter(), "REPORT_RESOLVED", "Báo cáo của bạn �‘ã �‘ược xử lý",
                "Báo cáo phòng \"" + report.getRoom().getTitle() + "\" �‘ã �‘ược cập nhật: " + statusText,
                report.getId());

        notifyUser(report.getRoom().getLandlord(), "REPORT_RESOLVED",
                "Báo cáo liên quan �‘ến phòng của bạn �‘ã �‘ược xử lý",
                "Phòng \"" + report.getRoom().getTitle() + "\" �‘ã �‘ược cập nhật trạng thái báo cáo: " + statusText,
                report.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã xử lý báo cáo", null));
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
