package com.homehn.backend.controller;

import com.homehn.backend.dto.request.CreateRentalBookingRequest;
import com.homehn.backend.dto.request.UpdateRentalBookingStatusRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.RentalBookingResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.RentalBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class RentalBookingController {

    private final RentalBookingService rentalBookingService;

    @PostMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> create(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateRentalBookingRequest req,
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RentalBookingResponse booking = rentalBookingService.create(roomId, req, request, principal.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo yêu cầu thuê phòng", booking));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<List<RentalBookingResponse>>> myBookings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rentalBookingService.getMyBookings(principal.getId())));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<List<RentalBookingResponse>>> landlordBookings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                rentalBookingService.getLandlordBookings(principal.getId(), principal.getRole())
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(rentalBookingService.getDetail(id, principal)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã huỷ đơn thuê phòng",
                rentalBookingService.cancel(id, principal.getId())
        ));
    }

    @PostMapping("/{id}/vnpay/refresh")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> refreshPaymentLink(
            @PathVariable Long id,
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã tạo lại link thanh toán VNPAY",
                rentalBookingService.refreshPaymentLink(id, request, principal.getId())
        ));
    }

    @PatchMapping("/{id}/landlord-status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> landlordDecision(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRentalBookingStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã cập nhật đơn thuê phòng",
                rentalBookingService.landlordDecision(id, req, principal.getId(), principal.getRole())
        ));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> payload) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(rentalBookingService.buildVnpayReturnRedirectUrl(payload)))
                .build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> payload) {
        return ResponseEntity.ok(rentalBookingService.handleVnpayIpn(payload));
    }
}
