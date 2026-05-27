package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.CreateRentalBookingRequest;
import com.homehn.backend.dto.request.UpdateRentalBookingStatusRequest;
import com.homehn.backend.dto.response.RentalBookingResponse;
import com.homehn.backend.config.VnpayProperties;
import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalBookingService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern TXN_REF_TIMESTAMP_PATTERN = Pattern.compile("(\\d{13})$");

    private final RentalBookingRepository bookingRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayProperties vnpayProperties;

    public RentalBookingResponse create(
            Long roomId,
            CreateRentalBookingRequest req,
            HttpServletRequest request,
            Long seekerId
    ) {
        UserEntity seeker = userRepo.findById(seekerId).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            throw new AppException("Chỉ có thể gửi yêu cầu thuê với phòng đang hiển thị");
        }
        if (room.getLandlord().getId().equals(seeker.getId())) {
            throw new AppException("Bạn không thể thuê phòng của chính mình");
        }
        if (req.getOccupantCount() > room.getMaxPeople()) {
            throw new AppException("Số người ở vượt quá giới hạn của phòng");
        }

        List<RentalBookingEntity.Status> blockingStatuses = List.of(
                RentalBookingEntity.Status.PENDING_PAYMENT,
                RentalBookingEntity.Status.DEPOSIT_PAID,
                RentalBookingEntity.Status.CONFIRMED
        );
        expirePendingBookingsForRoom(roomId, blockingStatuses);
        if (bookingRepo.existsByRoom_IdAndStatusIn(roomId, blockingStatuses)) {
            throw new AppException("Phòng này đang có đơn thuê đang xử lý hoặc đã được chốt");
        }
        if (bookingRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(roomId, seeker.getId(), blockingStatuses)) {
            throw new AppException("Bạn đã có một đơn thuê đang xử lý cho phòng này");
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

        notificationService.notifyUser(
                room.getLandlord(),
                "BOOKING_CREATED",
                "Có yêu cầu thuê phòng mới",
                seeker.getFullName() + " vừa gửi yêu cầu thuê phòng \"" + room.getTitle() + "\" và chờ thanh toán cọc.",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    @Transactional(readOnly = true)
    public List<RentalBookingResponse> getMyBookings(Long seekerId) {
        return bookingRepo.findBySeeker_IdOrderByCreatedAtDesc(seekerId).stream()
                .map(this::expirePendingPaymentIfNeeded)
                .map(RentalBookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RentalBookingResponse> getLandlordBookings(Long userId, UserEntity.Role role) {
        List<RentalBookingEntity> source = role == UserEntity.Role.ADMIN
                ? bookingRepo.findAllByOrderByCreatedAtDesc()
                : bookingRepo.findByLandlord_IdOrderByCreatedAtDesc(userId);
        return source.stream()
                .map(this::expirePendingPaymentIfNeeded)
                .map(RentalBookingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public RentalBookingResponse getDetail(Long id, UserPrincipal principal) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));
        ensureParticipant(booking, principal);
        booking = expirePendingPaymentIfNeeded(booking);
        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse cancel(Long id, Long seekerId) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));
        if (!booking.getSeeker().getId().equals(seekerId)) {
            throw new AppException("Bạn không có quyền huỷ đơn này", 403);
        }
        if (booking.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
            throw new AppException("Đơn thuê đã được xác nhận, không thể huỷ");
        }

        booking.setStatus(RentalBookingEntity.Status.CANCELLED);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.CANCELLED);
        booking.setPaymentMessage("Người thuê đã huỷ yêu cầu thuê phòng");
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getLandlord(),
                "BOOKING_UPDATED",
                "Người thuê đã huỷ đơn thuê",
                booking.getSeeker().getFullName() + " đã huỷ yêu cầu thuê phòng \"" + booking.getRoom().getTitle() + "\"",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse refreshPaymentLink(Long id, HttpServletRequest request, Long seekerId) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));

        if (!booking.getSeeker().getId().equals(seekerId)) {
            throw new AppException("Bạn không có quyền thanh toán đơn này", 403);
        }
        booking = expirePendingPaymentIfNeeded(booking);
        if (booking.getStatus() == RentalBookingEntity.Status.CANCELLED
                && booking.getPaymentStatus() == RentalBookingEntity.PaymentStatus.CANCELLED
                && booking.getPaymentResultCode() != null
                && booking.getPaymentResultCode() == 15) {
            throw new AppException("Link thanh toán VNPAY đã hết hạn. Đơn thuê đã bị huỷ, vui lòng tạo lại đơn thuê mới.");
        }
        if (booking.getStatus() != RentalBookingEntity.Status.PENDING_PAYMENT
                && booking.getStatus() != RentalBookingEntity.Status.PAYMENT_FAILED) {
            throw new AppException("Đơn thuê này không thể tạo lại link thanh toán");
        }

        VnpayPaymentService.PaymentCreationResult payment = vnpayPaymentService.createDepositPayment(booking, request);
        booking.setStatus(RentalBookingEntity.Status.PENDING_PAYMENT);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.PENDING);
        booking.setPaymentOrderId(payment.getOrderId());
        booking.setPaymentRequestId(payment.getRequestId());
        booking.setPaymentPayUrl(payment.getPayUrl());
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
        booking.setPaymentTransId(null);
        booking.setPaymentMessage(payment.getMessage());
        booking.setPaymentResultCode(payment.getResultCode());
        bookingRepo.save(booking);

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse landlordDecision(
            Long id,
            UpdateRentalBookingStatusRequest req,
            Long actorId,
            UserEntity.Role actorRole
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));

        boolean isOwner = booking.getLandlord().getId().equals(actorId);
        boolean isAdmin = actorRole == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xử lý đơn này", 403);
        }

        if (req.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
            if (booking.getStatus() != RentalBookingEntity.Status.DEPOSIT_PAID) {
                throw new AppException("Chỉ có thể xác nhận khi người thuê đã thanh toán cọc");
            }
            booking.setStatus(RentalBookingEntity.Status.CONFIRMED);
            booking.setConfirmedAt(now());
            booking.getRoom().setStatus(RoomEntity.RoomStatus.RENTED);
            roomRepo.save(booking.getRoom());
        } else if (req.getStatus() == RentalBookingEntity.Status.REJECTED) {
            if (booking.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
                throw new AppException("Đơn thuê đã được xác nhận, không thể từ chối");
            }
            booking.setStatus(RentalBookingEntity.Status.REJECTED);
        } else {
            throw new AppException("Chỉ hỗ trợ xác nhận hoặc từ chối đơn thuê");
        }

        booking.setLandlordNote(blankToNull(req.getNote()));
        bookingRepo.save(booking);

        String action = booking.getStatus() == RentalBookingEntity.Status.CONFIRMED
                ? "đã được xác nhận"
                : "đã bị từ chối";
        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Đơn thuê phòng đã được cập nhật",
                "Đơn thuê phòng \"" + booking.getRoom().getTitle() + "\" của bạn " + action + ".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public String buildVnpayReturnRedirectUrl(Map<String, String> payload) {
        if (!vnpayPaymentService.isEnabled()) {
            return vnpayPaymentService.buildFrontendReturnUrlForList(false);
        }

        VnpayPaymentService.CallbackVerificationResult verification = vnpayPaymentService.verifyCallback(payload);
        if (!verification.isValid()) {
            return vnpayPaymentService.buildFrontendReturnUrlForList(false);
        }

        RentalBookingEntity booking = bookingRepo.findByPaymentOrderId(verification.getTxnRef()).orElse(null);
        if (booking != null) {
            applyPaymentResult(booking, verification, false);
            return vnpayPaymentService.buildFrontendReturnUrl(booking.getId(), verification.isSuccess());
        }

        return vnpayPaymentService.buildFrontendReturnUrlForList(false);
    }

    public Map<String, String> handleVnpayIpn(Map<String, String> payload) {
        if (!vnpayPaymentService.isEnabled()) {
            return ipnResponse("99", "VNPAY disabled");
        }

        VnpayPaymentService.CallbackVerificationResult verification = vnpayPaymentService.verifyCallback(payload);
        if (!verification.isValid()) {
            return ipnResponse("97", "Invalid signature");
        }

        RentalBookingEntity booking = bookingRepo.findByPaymentOrderId(verification.getTxnRef()).orElse(null);
        if (booking == null) {
            return ipnResponse("01", "Order not found");
        }

        long expectedAmount = defaultDepositAmount(booking.getRoom()).stripTrailingZeros().longValueExact() * 100L;
        if (verification.getAmount() != expectedAmount) {
            return ipnResponse("04", "Invalid amount");
        }

        if ((booking.getPaymentStatus() == RentalBookingEntity.PaymentStatus.PAID
                || booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID
                || booking.getStatus() == RentalBookingEntity.Status.CONFIRMED)
                && verification.isSuccess()) {
            return ipnResponse("02", "Order already confirmed");
        }

        applyPaymentResult(booking, verification, true);
        return ipnResponse("00", "Confirm Success");
    }

    private void ensureParticipant(RentalBookingEntity booking, UserPrincipal principal) {
        boolean allowed = principal.getRole() == UserEntity.Role.ADMIN
                || booking.getSeeker().getId().equals(principal.getId())
                || booking.getLandlord().getId().equals(principal.getId());
        if (!allowed) {
            throw new AppException("Bạn không có quyền xem đơn thuê này", 403);
        }
    }

    private BigDecimal defaultDepositAmount(RoomEntity room) {
        return room.getPrice();
    }

    private String generateContractCode() {
        return "HD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String buildContractTerms(RoomEntity room, CreateRentalBookingRequest req) {
        return "Hợp đồng dự kiến cho phòng \"" + room.getTitle() + "\". "
                + "Tiền phòng hằng tháng: " + room.getPrice().stripTrailingZeros().toPlainString() + " VND. "
                + "Tiền cọc dự kiến: " + defaultDepositAmount(room).stripTrailingZeros().toPlainString() + " VND. "
                + "Thời hạn thuê: " + req.getLeaseMonths() + " tháng. "
                + "Số người ở: " + req.getOccupantCount() + ". "
                + "Ngày vào ở dự kiến: " + req.getMoveInDate() + ".";
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
                booking.setDepositPaidAt(now());
            }

            if (notify) {
                notificationService.notifyUser(
                        booking.getLandlord(),
                        "BOOKING_DEPOSIT_PAID",
                        "Người thuê đã thanh toán cọc",
                        booking.getSeeker().getFullName() + " đã thanh toán cọc cho phòng \"" + booking.getRoom().getTitle() + "\" qua VNPAY.",
                        booking.getId()
                );

                notificationService.notifyUser(
                        booking.getSeeker(),
                        "BOOKING_UPDATED",
                        "Đã ghi nhận thanh toán cọc",
                        "Hệ thống đã ghi nhận tiền cọc cho phòng \"" + booking.getRoom().getTitle() + "\". Chủ nhà sẽ xác nhận tiếp.",
                        booking.getId()
                );
            }
        } else {
            if (isExpiredOrCancelledPayment(verification)) {
                markBookingPaymentExpired(booking);
            } else {
                booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.FAILED);
                if (booking.getStatus() != RentalBookingEntity.Status.CONFIRMED) {
                    booking.setStatus(RentalBookingEntity.Status.PAYMENT_FAILED);
                }
            }
        }

        bookingRepo.save(booking);
    }

    private RentalBookingEntity expirePendingPaymentIfNeeded(RentalBookingEntity booking) {
        if (!isAwaitingPayment(booking) || !hasPaymentLinkExpired(booking)) {
            return booking;
        }

        markBookingPaymentExpired(booking);
        return bookingRepo.save(booking);
    }

    private void expirePendingBookingsForRoom(Long roomId, List<RentalBookingEntity.Status> statuses) {
        bookingRepo.findByRoom_IdAndStatusIn(roomId, statuses).forEach(this::expirePendingPaymentIfNeeded);
    }

    private boolean isAwaitingPayment(RentalBookingEntity booking) {
        return booking.getStatus() == RentalBookingEntity.Status.PENDING_PAYMENT
                && booking.getPaymentStatus() == RentalBookingEntity.PaymentStatus.PENDING;
    }

    private boolean hasPaymentLinkExpired(RentalBookingEntity booking) {
        LocalDateTime expiresAt = resolvePaymentExpiresAt(booking);
        return expiresAt != null && !expiresAt.isAfter(now());
    }

    private LocalDateTime resolvePaymentExpiresAt(RentalBookingEntity booking) {
        Long createdMillis = extractPaymentCreatedAtMillis(booking.getPaymentOrderId());
        if (createdMillis != null) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(createdMillis), APP_ZONE)
                    .plusMinutes(vnpayProperties.getExpireMinutes());
        }

        LocalDateTime fallbackBase = booking.getUpdatedAt() != null ? booking.getUpdatedAt() : booking.getCreatedAt();
        if (fallbackBase == null) {
            return null;
        }
        return fallbackBase.plusMinutes(vnpayProperties.getExpireMinutes());
    }

    private Long extractPaymentCreatedAtMillis(String paymentOrderId) {
        if (paymentOrderId == null || paymentOrderId.isBlank()) {
            return null;
        }

        Matcher matcher = TXN_REF_TIMESTAMP_PATTERN.matcher(paymentOrderId);
        if (!matcher.find()) {
            return null;
        }

        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isExpiredOrCancelledPayment(VnpayPaymentService.CallbackVerificationResult verification) {
        return "15".equals(verification.getResponseCode())
                || "15".equals(verification.getTransactionStatus())
                || "24".equals(verification.getResponseCode())
                || "24".equals(verification.getTransactionStatus());
    }

    private void markBookingPaymentExpired(RentalBookingEntity booking) {
        if (booking.getStatus() == RentalBookingEntity.Status.CONFIRMED) {
            return;
        }

        booking.setStatus(RentalBookingEntity.Status.CANCELLED);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.CANCELLED);
        booking.setPaymentPayUrl(null);
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
        booking.setPaymentMessage("Link thanh toán VNPAY đã hết hạn. Đơn thuê đã bị huỷ, vui lòng tạo lại đơn thuê mới.");
        booking.setPaymentResultCode(15);
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

    private LocalDateTime now() {
        return LocalDateTime.now(APP_ZONE);
    }

    private Map<String, String> ipnResponse(String code, String message) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("RspCode", code);
        response.put("Message", message);
        return response;
    }
}
