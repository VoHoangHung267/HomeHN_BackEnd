package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.LandlordReportResponseRequest;
import com.homehn.backend.dto.response.ReportResponse;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ReportRepository;
import com.homehn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public ReportResponse getReportDetail(Long id, Long userId, UserEntity.Role role) {
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));

        boolean isAdmin = role == UserEntity.Role.ADMIN;
        boolean isReporter = report.getReporter().getId().equals(userId);
        boolean isLandlord = report.getRoom().getLandlord().getId().equals(userId);
        if (!isAdmin && !isReporter && !isLandlord) {
            throw new AppException("Bạn không có quyền xem báo cáo này", 403);
        }

        return toResponse(report);
    }

    public ReportResponse respondAsLandlord(Long id, LandlordReportResponseRequest body, Long landlordId) {
        ReportEntity report = reportRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy báo cáo", 404));

        if (!report.getRoom().getLandlord().getId().equals(landlordId)) {
            throw new AppException("Bạn không có quyền phản hồi báo cáo này", 403);
        }
        if (report.getStatus() != ReportEntity.Status.REVIEWED) {
            throw new AppException("Chỉ có thể phản hồi khi báo cáo đang ở trạng thái xem xét");
        }

        report.setLandlordResponseType(ReportEntity.LandlordResponseType.valueOf(body.getResponseType().name()));
        report.setLandlordResponseNote(body.getResponseNote().trim());
        report.setLandlordRespondedAt(LocalDateTime.now());
        reportRepo.save(report);

        userRepo.findByRole(UserEntity.Role.ADMIN).forEach(admin ->
                notificationService.notifyUser(
                        admin,
                        "REPORT_LANDLORD_RESPONDED",
                        "Chủ phòng đã phản hồi báo cáo",
                        "Chủ phòng của \"" + report.getRoom().getTitle() + "\" đã gửi phản hồi cho báo cáo.",
                        report.getId()
                )
        );

        return toResponse(report);
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
