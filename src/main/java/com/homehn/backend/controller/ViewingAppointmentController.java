package com.homehn.backend.controller;

import com.homehn.backend.dto.request.CreateViewingAppointmentRequest;
import com.homehn.backend.dto.request.UpdateViewingAppointmentRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ViewingAppointmentResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.ViewingAppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class ViewingAppointmentController {

    private final ViewingAppointmentService viewingAppointmentService;

    @PostMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> create(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateViewingAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã gửi yêu cầu xem phòng",
                viewingAppointmentService.create(roomId, req, principal.getId())
        ));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ViewingAppointmentResponse>>> myAppointments(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                viewingAppointmentService.getMyAppointments(principal.getId(), principal.getRole())
        ));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateViewingAppointmentRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật lịch xem phòng",
                viewingAppointmentService.updateStatus(id, req, principal.getId(), principal.getRole())
        ));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ViewingAppointmentResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã huỷ lịch xem phòng",
                viewingAppointmentService.cancel(id, principal.getId())
        ));
    }
}
