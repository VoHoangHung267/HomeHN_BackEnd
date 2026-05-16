package com.homehn.backend.controller;

import com.homehn.backend.dto.request.LandlordReportResponseRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ReportResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReportResponse>> getReportDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                reportService.getReportDetail(id, principal.getId(), principal.getRole())
        ));
    }

    @PatchMapping("/{id}/landlord-response")
    @PreAuthorize("hasRole('LANDLORD')")
    public ResponseEntity<ApiResponse<ReportResponse>> respondAsLandlord(
            @PathVariable Long id,
            @RequestBody LandlordReportResponseRequest body,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã gửi phản hồi cho admin",
                reportService.respondAsLandlord(id, body, principal.getId())
        ));
    }
}
