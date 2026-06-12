package com.homehn.backend.controller;

import com.homehn.backend.dto.request.RejectRoomRequest;
import com.homehn.backend.dto.request.ResolveReportRequest;
import com.homehn.backend.dto.response.*;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.service.impl.AdminService;
import com.homehn.backend.service.impl.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
class AdminController {

    private final AdminService adminService;
    private final ReviewService reviewService;

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<StatsResponse>> stats() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getStats()));
    }

    @GetMapping("/rooms/pending")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> pendingRooms() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getPendingRooms()));
    }

    @PatchMapping("/rooms/{id}/approve")
    public ResponseEntity<ApiResponse<Void>> approveRoom(@PathVariable Long id) {
        adminService.approveRoom(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã duyệt phòng", null));
    }

    @PatchMapping("/rooms/{id}/reject")
    public ResponseEntity<ApiResponse<Void>> rejectRoom(
            @PathVariable Long id,
            @RequestBody RejectRoomRequest body
    ) {
        adminService.rejectRoom(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Đã từ chối phòng", null));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<UserResponse>>> allUsers() {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllUsers()));
    }

    @PatchMapping("/users/{id}/toggle-active")
    public ResponseEntity<ApiResponse<Void>> toggleUser(@PathVariable Long id) {
        boolean active = adminService.toggleUser(id);
        return ResponseEntity.ok(ApiResponse.ok(active ? "Đã mở khoá" : "Đã khoá tài khoản", null));
    }

    @GetMapping("/rooms/all")
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> allRooms(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String ward,
            @RequestParam(required = false) RoomEntity.RoomStatus status,
            @RequestParam(required = false) Long landlordId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                adminService.getAllRooms(keyword, ward, status, landlordId, page, size)
        ));
    }

    @PatchMapping("/rooms/{id}/toggle-hidden")
    public ResponseEntity<ApiResponse<Void>> toggleHidden(@PathVariable Long id) {
        boolean hidden = adminService.toggleHidden(id);
        return ResponseEntity.ok(ApiResponse.ok(hidden ? "Đã ẩn phòng" : "Đã hiện phòng", null));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(@PathVariable Long id) {
        adminService.deleteRoom(id);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá phòng", null));
    }

    @GetMapping("/reports")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> allReports(
            @RequestParam(defaultValue = "PENDING") ReportEntity.Status status
    ) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getAllReports(status)));
    }

    @GetMapping("/reports/{id}")
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(adminService.getReport(id)));
    }

    @PatchMapping("/reports/{id}/resolve")
    public ResponseEntity<ApiResponse<Void>> resolveReport(
            @PathVariable Long id,
            @RequestBody ResolveReportRequest body
    ) {
        adminService.resolveReport(id, body);
        return ResponseEntity.ok(ApiResponse.ok("Đã xử lý báo cáo", null));
    }

    @GetMapping("/reviews")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> allReviews() {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getAllReviews()));
    }

    @DeleteMapping("/reviews/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Long id) {
        reviewService.deleteReview(id, -1L, UserEntity.Role.ADMIN);
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá đánh giá", null));
    }
}
