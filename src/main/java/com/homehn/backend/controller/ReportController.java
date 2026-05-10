package com.homehn.backend.controller;

import com.homehn.backend.dto.request.LandlordReportResponseRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ReportResponse;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.ReportRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));

        boolean isAdmin = user.getRole() == UserEntity.Role.ADMIN;
        boolean isReporter = report.getReporter().getId().equals(user.getId());
        boolean isLandlord = report.getRoom().getLandlord().getId().equals(user.getId());
        if (!isAdmin && !isReporter && !isLandlord) {
            throw new AppException("Bạn không có quyền xem báo cáo này", 403);
        }

        return ResponseEntity.ok(ApiResponse.ok(toResponse(report)));
    }

    @PatchMapping("/{id}/landlord-response")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ApiResponse<ReportResponse>> respondAsLandlord(
            @PathVariable Long id,
            @RequestBody LandlordReportResponseRequest body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));

        if (!report.getRoom().getLandlord().getId().equals(user.getId())) {
            throw new AppException("Bạn không có quyền phản h�“i báo cáo này", 403);
        }
        if (report.getStatus() != ReportEntity.Status.REVIEWED) {
            throw new AppException("Ch�‰ có th�ƒ phản h�“i khi báo cáo �‘ang �Ÿ trạng thái xem xét");
        }

        report.setLandlordResponseType(ReportEntity.LandlordResponseType.valueOf(body.getResponseType().name()));
        report.setLandlordResponseNote(body.getResponseNote().trim());
        report.setLandlordRespondedAt(java.time.LocalDateTime.now());
        reportRepo.save(report);

        userRepo.findByRole(UserEntity.Role.ADMIN).forEach(admin -> notifRepo.save(com.homehn.backend.entity.NotificationEntity.builder()
                .user(admin)
                .type("REPORT_LANDLORD_RESPONDED")
                .title("Chủ phòng �‘ã phản h�“i báo cáo")
                .message("Chủ phòng của \"" + report.getRoom().getTitle() + "\" �‘ã gửi phản h�“i cho báo cáo.")
                .relatedId(report.getId())
                .build()));

        return ResponseEntity.ok(ApiResponse.ok("Đã gửi phản h�“i cho admin", toResponse(report)));
    }

    private ReportResponse toResponse(ReportEntity report) {
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
}
