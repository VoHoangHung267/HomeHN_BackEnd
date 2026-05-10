package com.homehn.backend.controller;

import com.homehn.backend.dto.request.CreateRentalBookingRequest;
import com.homehn.backend.dto.request.UpdateRentalBookingStatusRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.RentalBookingResponse;
import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.VnpayPaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class RentalBookingController {

    private final RentalBookingRepository bookingRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;
    private final VnpayPaymentService vnpayPaymentService;

    @PostMapping("/rooms/{roomId}")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> create(
            @PathVariable Long roomId,
            @Valid @RequestBody CreateRentalBookingRequest req,
            HttpServletRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity seeker = userRepo.findById(principal.getId()).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            throw new AppException("Ch�‰ có th�ƒ gửi yêu cầu thuê v�›i phòng �‘ang hi�ƒn th�‹");
        }
        if (room.getLandlord().getId().equals(seeker.getId())) {
            throw new AppException("Bạn không th�ƒ thuê phòng của chính mình");
        }
        if (req.getOccupantCount() > room.getMaxPeople()) {
            throw new AppException("S�‘ người �Ÿ vượt quá gi�›i hạn của phòng");
        }

        List<RentalBookingEntity.Status> blockingStatuses = List.of(
                RentalBookingEntity.Status.PENDING_PAYMENT,
                RentalBookingEntity.Status.DEPOSIT_PAID,
                RentalBookingEntity.Status.CONFIRMED
        );
        if (bookingRepo.existsByRoom_IdAndStatusIn(roomId, blockingStatuses)) {
            throw new AppException("Phòng này �‘ang có �‘ơn thuê �‘ang xử lý hoặc �‘ã �‘ược ch�‘t");
        }
        if (bookingRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(roomId, seeker.getId(), blockingStatuses)) {
            throw new AppException("Bạn �‘ã có m�™t �‘ơn thuê �‘ang xử lý cho phòng này");
        }

        RentalBookingEntity booking = bookingRepo.save(RentalBookingEntity.builder()
                .room(room)
                .seeker(seeker)
                .landlord(room.getLandlord())
                .tenantFullName(req.getTenantFullName().trim())
                .tenantPhone(req.getTenantPhone().trim())
                .tenantEmail(blankToNull(req.getTenantEmail()))
                .tenantIdentityNumber(blankToNull(req.getTenantIdentityNumber()))
                .moveInDate(req.getMoveInDate())
                .leaseMonths(req.getLeaseMonths())
                .occupantCount(req.getOccupantCount())
                .monthlyRent(room.getPrice())
                .depositAmount(defaultDepositAmount(room))
                .contractCode(generateContractCode())
                .contractTerms(buildContractTerms(room, req))
                .note(blankToNull(req.getNote()))
                .status(RentalBookingEntity.Status.PENDING_PAYMENT)
                .paymentStatus(RentalBookingEntity.PaymentStatus.PENDING)
                .paymentProvider("VNPAY")
                .build());

        VnpayPaymentService.PaymentCreationResult payment;
        try {
            payment = vnpayPaymentService.createDepositPayment(booking, request);
        } catch (AppException ex) {
            bookingRepo.delete(booking);
            throw ex;
        }

        booking.setPaymentOrderId(payment.getOrderId());
        booking.setPaymentRequestId(payment.getRequestId());
        booking.setPaymentPayUrl(payment.getPayUrl());
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
        booking.setPaymentMessage(payment.getMessage());
        booking.setPaymentResultCode(payment.getResultCode());
        bookingRepo.save(booking);

        notifyUser(room.getLandlord(), "BOOKING_CREATED", "Có yêu cầu thuê phòng m�›i",
                seeker.getFullName() + " vừa gửi yêu cầu thuê phòng \"" + room.getTitle() + "\" và chờ thanh toán cọc.",
                booking.getId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đã tạo yêu cầu thuê phòng", RentalBookingResponse.from(booking)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<List<RentalBookingResponse>>> myBookings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var data = bookingRepo.findBySeeker_IdOrderByCreatedAtDesc(principal.getId()).stream()
                .map(RentalBookingResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/landlord")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<List<RentalBookingResponse>>> landlordBookings(
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        var source = principal.getRole() == UserEntity.Role.ADMIN
                ? bookingRepo.findAllByOrderByCreatedAtDesc()
                : bookingRepo.findByLandlord_IdOrderByCreatedAtDesc(principal.getId());
        var data = source.stream()
                .map(RentalBookingResponse::from)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> detail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy �‘ơn thuê phòng", 404));
        ensureParticipant(booking, principal);
        return ResponseEntity.ok(ApiResponse.ok(RentalBookingResponse.from(booking)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> cancel(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy �‘ơn thuê phòng", 404));
        if (!booking.getSeeker().getId().equals(principal.getId())) {
            throw new AppException("Bạn không có quyền huỷ �‘ơn này", 403);
        }
        if (booking.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
            throw new AppException("Đơn thuê �‘ã �‘ược xác nhận, không th�ƒ huỷ");
        }

        booking.setStatus(RentalBookingEntity.Status.CANCELLED);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.CANCELLED);
        booking.setPaymentMessage("Người thuê �‘ã huỷ yêu cầu thuê phòng");
        bookingRepo.save(booking);

        notifyUser(booking.getLandlord(), "BOOKING_UPDATED", "Người thuê �‘ã huỷ �‘ơn thuê",
                booking.getSeeker().getFullName() + " �‘ã huỷ yêu cầu thuê phòng \"" + booking.getRoom().getTitle() + "\"",
                booking.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã huỷ �‘ơn thuê phòng", RentalBookingResponse.from(booking)));
    }

    @PatchMapping("/{id}/landlord-status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RentalBookingResponse>> landlordDecision(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRentalBookingStatusRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy �‘ơn thuê phòng", 404));

        boolean isOwner = booking.getLandlord().getId().equals(principal.getId());
        boolean isAdmin = principal.getRole() == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xử lý �‘ơn này", 403);
        }

        if (req.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
            if (booking.getStatus() != RentalBookingEntity.Status.DEPOSIT_PAID) {
                throw new AppException("Ch�‰ có th�ƒ xác nhận khi người thuê �‘ã thanh toán cọc");
            }
            booking.setStatus(RentalBookingEntity.Status.CONFIRMED);
            booking.setConfirmedAt(LocalDateTime.now());
            booking.getRoom().setStatus(RoomEntity.RoomStatus.RENTED);
            roomRepo.save(booking.getRoom());
        } else if (req.getStatus() == RentalBookingEntity.Status.REJECTED) {
            if (booking.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
                throw new AppException("Đơn thuê �‘ã �‘ược xác nhận, không th�ƒ từ ch�‘i");
            }
            booking.setStatus(RentalBookingEntity.Status.REJECTED);
        } else {
            throw new AppException("Ch�‰ h�— trợ xác nhận hoặc từ ch�‘i �‘ơn thuê");
        }

        booking.setLandlordNote(blankToNull(req.getNote()));
        bookingRepo.save(booking);

        String action = booking.getStatus() == RentalBookingEntity.Status.CONFIRMED ? "�‘ã �‘ược xác nhận" : "�‘ã b�‹ từ ch�‘i";
        notifyUser(booking.getSeeker(), "BOOKING_UPDATED", "Đơn thuê phòng �‘ã �‘ược cập nhật",
                "Đơn thuê phòng \"" + booking.getRoom().getTitle() + "\" của bạn " + action + ".",
                booking.getId());

        return ResponseEntity.ok(ApiResponse.ok("Đã cập nhật �‘ơn thuê phòng", RentalBookingResponse.from(booking)));
    }

    @GetMapping("/vnpay/return")
    public ResponseEntity<Void> vnpayReturn(@RequestParam Map<String, String> payload) {
        if (!vnpayPaymentService.isEnabled()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(vnpayPaymentService.buildFrontendReturnUrlForList(false)))
                    .build();
        }

        VnpayPaymentService.CallbackVerificationResult verification = vnpayPaymentService.verifyCallback(payload);
        if (!verification.isValid()) {
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(vnpayPaymentService.buildFrontendReturnUrlForList(false)))
                    .build();
        }

        RentalBookingEntity booking = bookingRepo.findByPaymentOrderId(verification.getTxnRef()).orElse(null);
        if (booking != null) {
            applyPaymentResult(booking, verification, false);
        }

        String redirectUrl = booking != null
                ? vnpayPaymentService.buildFrontendReturnUrl(booking.getId(), verification.isSuccess())
                : vnpayPaymentService.buildFrontendReturnUrlForList(false);

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(redirectUrl))
                .build();
    }

    @GetMapping("/vnpay/ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(@RequestParam Map<String, String> payload) {
        if (!vnpayPaymentService.isEnabled()) {
            return ResponseEntity.ok(ipnResponse("99", "VNPAY disabled"));
        }

        VnpayPaymentService.CallbackVerificationResult verification = vnpayPaymentService.verifyCallback(payload);
        if (!verification.isValid()) {
            return ResponseEntity.ok(ipnResponse("97", "Invalid signature"));
        }

        RentalBookingEntity booking = bookingRepo.findByPaymentOrderId(verification.getTxnRef()).orElse(null);
        if (booking == null) {
            return ResponseEntity.ok(ipnResponse("01", "Order not found"));
        }

        long expectedAmount = defaultDepositAmount(booking.getRoom()).stripTrailingZeros().longValueExact() * 100L;
        if (verification.getAmount() != expectedAmount) {
            return ResponseEntity.ok(ipnResponse("04", "Invalid amount"));
        }

        if ((booking.getPaymentStatus() == RentalBookingEntity.PaymentStatus.PAID
                || booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID
                || booking.getStatus() == RentalBookingEntity.Status.CONFIRMED)
                && verification.isSuccess()) {
            return ResponseEntity.ok(ipnResponse("02", "Order already confirmed"));
        }

        applyPaymentResult(booking, verification, true);
        return ResponseEntity.ok(ipnResponse("00", "Confirm Success"));
    }

    private void ensureParticipant(RentalBookingEntity booking, UserPrincipal principal) {
        boolean allowed = principal.getRole() == UserEntity.Role.ADMIN
                || booking.getSeeker().getId().equals(principal.getId())
                || booking.getLandlord().getId().equals(principal.getId());
        if (!allowed) {
            throw new AppException("Bạn không có quyền xem �‘ơn thuê này", 403);
        }
    }

    private BigDecimal defaultDepositAmount(RoomEntity room) {
        return room.getPrice();
    }

    private String generateContractCode() {
        return "HD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String buildContractTerms(RoomEntity room, CreateRentalBookingRequest req) {
        return "Hợp �‘�“ng dự kiến cho phòng \"" + room.getTitle() + "\". "
                + "Tiền phòng hằng tháng: " + room.getPrice().stripTrailingZeros().toPlainString() + " VND. "
                + "Tiền cọc dự kiến: " + defaultDepositAmount(room).stripTrailingZeros().toPlainString() + " VND. "
                + "Thời hạn thuê: " + req.getLeaseMonths() + " tháng. "
                + "S�‘ người �Ÿ: " + req.getOccupantCount() + ". "
                + "Ngày vào �Ÿ dự kiến: " + req.getMoveInDate() + ".";
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

    private void applyPaymentResult(
            RentalBookingEntity booking,
            VnpayPaymentService.CallbackVerificationResult verification,
            boolean notify
    ) {
        booking.setPaymentProvider("VNPAY");
        booking.setPaymentResultCode(toInt(verification.getResponseCode(), -1));
        booking.setPaymentMessage(verification.getMessage());
        booking.setPaymentTransId(verification.getTransactionNo() > 0 ? verification.getTransactionNo() : null);

        if (verification.isSuccess()) {
            booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.PAID);
            booking.setStatus(RentalBookingEntity.Status.DEPOSIT_PAID);
            if (booking.getDepositPaidAt() == null) {
                booking.setDepositPaidAt(LocalDateTime.now());
            }

            if (notify) {
                notifyUser(booking.getLandlord(), "BOOKING_DEPOSIT_PAID", "Người thuê �‘ã thanh toán cọc",
                        booking.getSeeker().getFullName() + " �‘ã thanh toán cọc cho phòng \"" + booking.getRoom().getTitle() + "\" qua VNPAY.",
                        booking.getId());

                notifyUser(booking.getSeeker(), "BOOKING_UPDATED", "Đã ghi nhận thanh toán cọc",
                        "H�‡ th�‘ng �‘ã ghi nhận tiền cọc cho phòng \"" + booking.getRoom().getTitle() + "\". Chủ nhà sẽ xác nhận tiếp.",
                        booking.getId());
            }
        } else {
            booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.FAILED);
            if (booking.getStatus() != RentalBookingEntity.Status.CONFIRMED) {
                booking.setStatus(RentalBookingEntity.Status.PAYMENT_FAILED);
            }
        }

        bookingRepo.save(booking);
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private int toInt(Object value, int fallback) {
        if (value == null) return fallback;
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private Map<String, String> ipnResponse(String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", code);
        response.put("Message", message);
        return response;
    }
}
