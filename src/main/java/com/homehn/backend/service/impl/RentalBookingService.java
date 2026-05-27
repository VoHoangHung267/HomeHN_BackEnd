package com.homehn.backend.service.impl;

import com.homehn.backend.config.VnpayProperties;
import com.homehn.backend.dto.request.ApproveRenewalRequest;
import com.homehn.backend.dto.request.ConfirmCashDepositRequest;
import com.homehn.backend.dto.request.CreateRentalBookingRequest;
import com.homehn.backend.dto.request.RejectRenewalRequest;
import com.homehn.backend.dto.request.RequestRenewalRequest;
import com.homehn.backend.dto.request.TerminateContractEarlyRequest;
import com.homehn.backend.dto.request.UpdateBookingContractDraftRequest;
import com.homehn.backend.dto.request.UpdateRentalBookingStatusRequest;
import com.homehn.backend.dto.response.RentalBookingResponse;
import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ContractAdjustmentRepository;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.util.ContractTermsFormatter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class RentalBookingService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern TXN_REF_TIMESTAMP_PATTERN = Pattern.compile("(\\d{13})$");
    private static final Set<RentalBookingEntity.Status> ACTIVE_CONTRACT_STATUSES = Set.of(
            RentalBookingEntity.Status.ACTIVE,
            RentalBookingEntity.Status.EXPIRING_SOON,
            RentalBookingEntity.Status.RENEWAL_PENDING,
            RentalBookingEntity.Status.EARLY_TERMINATION_PENDING
    );

    private final RentalBookingRepository bookingRepo;
    private final ContractAdjustmentRepository adjustmentRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationService notificationService;
    private final VnpayPaymentService vnpayPaymentService;
    private final VnpayProperties vnpayProperties;
    private final ContractLifecycleService contractLifecycleService;

    public RentalBookingResponse create(
            Long roomId,
            CreateRentalBookingRequest req,
            HttpServletRequest request,
            Long seekerId
    ) {
        contractLifecycleService.syncNow();

        UserEntity seeker = userRepo.findById(seekerId).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));

        if (room.getLandlord().getId().equals(seeker.getId())) {
            throw new AppException("Bạn không thể thuê phòng của chính mình");
        }
        if (req.getOccupantCount() > room.getMaxPeople()) {
            throw new AppException("Số người ở vượt quá giới hạn của phòng");
        }

        LocalDate moveInDate = req.getMoveInDate();
        if (room.getStatus() == RoomEntity.RoomStatus.ACTIVE) {
            ensureNoActiveContract(roomId);
        } else if (room.getStatus() == RoomEntity.RoomStatus.AVAILABLE_SOON) {
            LocalDate availableFrom = resolveAvailableFrom(roomId);
            if (availableFrom == null) {
                throw new AppException("Phòng chưa sẵn sàng nhận yêu cầu thuê mới");
            }
            if (!moveInDate.isAfter(availableFrom.minusDays(1))) {
                throw new AppException("Ngày vào ở phải từ " + availableFrom + " trở đi");
            }
        } else {
            throw new AppException("Chỉ có thể gửi yêu cầu thuê với phòng đang hiển thị hoặc sắp trống");
        }

        List<RentalBookingEntity.Status> blockingStatuses = List.of(
                RentalBookingEntity.Status.REQUESTED,
                RentalBookingEntity.Status.PENDING_PAYMENT,
                RentalBookingEntity.Status.DEPOSIT_PAID
        );
        expirePendingBookingsForRoom(roomId, blockingStatuses);
        if (bookingRepo.existsByRoom_IdAndStatusIn(roomId, blockingStatuses)) {
            throw new AppException("Phòng này đang có yêu cầu thuê khác đang xử lý hoặc đã đặt cọc");
        }
        if (bookingRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(roomId, seeker.getId(), blockingStatuses)) {
            throw new AppException("Bạn đã có một yêu cầu thuê đang xử lý cho phòng này");
        }

        RentalBookingEntity booking = bookingRepo.save(RentalBookingEntity.builder()
                .room(room)
                .seeker(seeker)
                .landlord(room.getLandlord())
                .tenantFullName(req.getTenantFullName().trim())
                .tenantPhone(req.getTenantPhone().trim())
                .tenantEmail(blankToNull(req.getTenantEmail()))
                .tenantIdentityNumber(blankToNull(req.getTenantIdentityNumber()))
                .moveInDate(moveInDate)
                .leaseMonths(req.getLeaseMonths())
                .occupantCount(req.getOccupantCount())
                .monthlyRent(room.getPrice())
                .depositAmount(defaultDepositAmount(room))
                .contractCode(generateContractCode())
                .contractMoveInRules("Trao đổi trực tiếp với chủ trọ để chốt giờ giấc ra vào.")
                .contractServiceNotes("Điện, nước và dịch vụ áp dụng theo thông tin phòng tại thời điểm gửi yêu cầu.")
                .contractAdditionalTerms("Người thuê xem trước hợp đồng, có thể yêu cầu chỉnh sửa trước khi chuyển sang đặt cọc.")
                .contractTerms(buildContractTerms(
                        "Trao đổi trực tiếp với chủ trọ để chốt giờ giấc ra vào.",
                        room.getPrice(),
                        defaultDepositAmount(room),
                        room.getElectricPrice(),
                        room.getWaterPrice(),
                        room.getOtherFees(),
                        "Điện, nước và dịch vụ áp dụng theo thông tin phòng tại thời điểm gửi yêu cầu.",
                        "Người thuê xem trước hợp đồng, có thể yêu cầu chỉnh sửa trước khi chuyển sang đặt cọc."
                ))
                .note(blankToNull(req.getNote()))
                .status(RentalBookingEntity.Status.REQUESTED)
                .paymentStatus(RentalBookingEntity.PaymentStatus.PENDING)
                .paymentProvider(req.getPaymentMethod())
                .paymentMessage("Đã tạo yêu cầu thuê phòng. Đang chờ chủ trọ xem xét hồ sơ.")
                .build());

        notificationService.notifyUser(
                room.getLandlord(),
                "BOOKING_CREATED",
                "Có yêu cầu thuê phòng mới",
                seeker.getFullName() + " vừa gửi yêu cầu thuê phòng \"" + room.getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public List<RentalBookingResponse> getMyBookings(Long seekerId) {
        contractLifecycleService.syncNow();
        return bookingRepo.findBySeeker_IdOrderByCreatedAtDesc(seekerId).stream()
                .map(this::expirePendingPaymentIfNeeded)
                .map(RentalBookingResponse::from)
                .toList();
    }

    public List<RentalBookingResponse> getLandlordBookings(Long userId, UserEntity.Role role) {
        contractLifecycleService.syncNow();
        List<RentalBookingEntity> source = role == UserEntity.Role.ADMIN
                ? bookingRepo.findAllByOrderByCreatedAtDesc()
                : bookingRepo.findByLandlord_IdOrderByCreatedAtDesc(userId);
        return source.stream()
                .map(this::expirePendingPaymentIfNeeded)
                .map(RentalBookingResponse::from)
                .toList();
    }

    public RentalBookingResponse getDetail(Long id, UserPrincipal principal) {
        contractLifecycleService.syncNow();
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
        if (booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID
                || ACTIVE_CONTRACT_STATUSES.contains(booking.getStatus())) {
            throw new AppException("Đơn thuê đã bước sang giai đoạn hiệu lực hoặc đã đặt cọc, không thể huỷ");
        }

        booking.setStatus(RentalBookingEntity.Status.CANCELLED);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.CANCELLED);
        booking.setPaymentPayUrl(null);
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
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
        if (!"VNPAY".equalsIgnoreCase(booking.getPaymentProvider())) {
            throw new AppException("Đơn thuê này không sử dụng VNPAY");
        }
        if (booking.getStatus() != RentalBookingEntity.Status.PENDING_PAYMENT
                && booking.getStatus() != RentalBookingEntity.Status.PAYMENT_FAILED) {
            throw new AppException("Đơn thuê này không thể tạo lại link thanh toán");
        }

        applyFreshPaymentLink(booking, request);
        bookingRepo.save(booking);
        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse landlordDecision(
            Long id,
            UpdateRentalBookingStatusRequest req,
            Long actorId,
            UserEntity.Role actorRole,
            HttpServletRequest request
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));

        ensureLandlordOrAdmin(booking, actorId, actorRole);

        if (req.getAction() == UpdateRentalBookingStatusRequest.Action.APPROVE) {
            if (booking.getStatus() != RentalBookingEntity.Status.REQUESTED) {
                throw new AppException("Chỉ có thể chấp thuận yêu cầu thuê mới");
            }
            if (adjustmentRepo.findFirstByBooking_IdAndStatusOrderByCreatedAtDesc(
                    booking.getId(), com.homehn.backend.entity.ContractAdjustmentEntity.Status.PENDING
            ).isPresent()) {
                throw new AppException("Vui lòng xử lý xong các điều chỉnh hợp đồng trước khi yêu cầu đặt cọc");
            }
            if ("VNPAY".equalsIgnoreCase(booking.getPaymentProvider())) {
                applyFreshPaymentLink(booking, request);
                booking.setPaymentMessage("Chủ trọ đã chấp thuận yêu cầu thuê. Vui lòng đọc điều khoản và thanh toán cọc qua VNPAY để giữ phòng.");
            } else {
                booking.setStatus(RentalBookingEntity.Status.PENDING_PAYMENT);
                booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.PENDING);
                booking.setPaymentPayUrl(null);
                booking.setPaymentMessage("Chủ trọ đã chấp thuận yêu cầu thuê. Người thuê sẽ đặt cọc tiền mặt/chuyển khoản trực tiếp và chủ trọ xác nhận trên hệ thống.");
            }
            booking.setLandlordNote(blankToNull(req.getNote()));
            bookingRepo.save(booking);

            notificationService.notifyUser(
                    booking.getSeeker(),
                    "BOOKING_UPDATED",
                    "Yêu cầu thuê đã được chấp thuận",
                    "Chủ trọ đã chấp thuận yêu cầu thuê phòng \"" + booking.getRoom().getTitle() + "\". Vui lòng xem điều khoản và thực hiện đặt cọc theo phương thức đã chọn.",
                    booking.getId()
            );
            return RentalBookingResponse.from(booking);
        }

        if (req.getAction() != UpdateRentalBookingStatusRequest.Action.REJECT) {
            throw new AppException("Chỉ hỗ trợ chấp thuận hoặc từ chối yêu cầu thuê");
        }
        if (booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID || ACTIVE_CONTRACT_STATUSES.contains(booking.getStatus())) {
            throw new AppException("Đơn thuê đã được đặt cọc hoặc đã có hiệu lực, không thể từ chối");
        }

        booking.setStatus(RentalBookingEntity.Status.REJECTED);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.CANCELLED);
        booking.setPaymentPayUrl(null);
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
        booking.setLandlordNote(blankToNull(req.getNote()));
        booking.setPaymentMessage("Chủ trọ đã từ chối yêu cầu thuê.");
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Yêu cầu thuê đã bị từ chối",
                "Chủ trọ đã từ chối yêu cầu thuê phòng \"" + booking.getRoom().getTitle() + "\" của bạn.",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse requestRenewal(Long id, RequestRenewalRequest req, Long seekerId) {
        contractLifecycleService.syncNow();
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
        if (!booking.getSeeker().getId().equals(seekerId)) {
            throw new AppException("Bạn không có quyền yêu cầu gia hạn hợp đồng này", 403);
        }
        if (booking.getStatus() != RentalBookingEntity.Status.EXPIRING_SOON) {
            throw new AppException("Hợp đồng này hiện chưa ở giai đoạn yêu cầu gia hạn");
        }

        booking.setStatus(RentalBookingEntity.Status.RENEWAL_PENDING);
        booking.setLandlordNote(null);
        booking.setNote(blankToNull(req.getNote()));
        booking.setPaymentMessage("Người thuê đã gửi yêu cầu gia hạn hợp đồng thêm " + req.getLeaseMonths() + " tháng. Đang chờ chủ trọ chốt điều khoản.");
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getLandlord(),
                "CONTRACT_RENEWAL_REQUESTED",
                "Có yêu cầu gia hạn hợp đồng",
                booking.getSeeker().getFullName() + " muốn gia hạn hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\" thêm " + req.getLeaseMonths() + " tháng.",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse approveRenewal(Long id, ApproveRenewalRequest req, Long actorId, UserEntity.Role actorRole) {
        contractLifecycleService.syncNow();
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
        ensureLandlordOrAdmin(booking, actorId, actorRole);
        if (booking.getStatus() != RentalBookingEntity.Status.RENEWAL_PENDING) {
            throw new AppException("Hợp đồng này hiện chưa ở giai đoạn chốt gia hạn");
        }

        booking.setLeaseMonths(booking.getLeaseMonths() + req.getLeaseMonths());
        booking.setContractTerms(blankToNull(req.getContractTerms()) != null ? req.getContractTerms().trim() : booking.getContractTerms());
        booking.setLandlordNote(blankToNull(req.getNote()));
        booking.setStatus(RentalBookingEntity.Status.ACTIVE);
        booking.setPaymentMessage("Hai bên đã thống nhất gia hạn hợp đồng. Phòng tiếp tục ở trạng thái đã cho thuê.");
        booking.getRoom().setStatus(RoomEntity.RoomStatus.RENTED);
        roomRepo.save(booking.getRoom());
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "CONTRACT_RENEWED",
                "Hợp đồng đã được gia hạn",
                "Chủ trọ đã chấp thuận gia hạn hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse rejectRenewal(Long id, RejectRenewalRequest req, Long actorId, UserEntity.Role actorRole) {
        contractLifecycleService.syncNow();
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
        ensureLandlordOrAdmin(booking, actorId, actorRole);
        if (booking.getStatus() != RentalBookingEntity.Status.RENEWAL_PENDING
                && booking.getStatus() != RentalBookingEntity.Status.EXPIRING_SOON) {
            throw new AppException("Hợp đồng này hiện chưa ở giai đoạn xử lý gia hạn");
        }

        booking.setStatus(RentalBookingEntity.Status.EXPIRING_SOON);
        booking.setLandlordNote(blankToNull(req.getNote()));
        booking.setPaymentMessage("Chủ trọ xác nhận không gia hạn. Phòng đang mở cho khách mới xem trước và nhận yêu cầu thuê sau ngày hết hạn.");
        booking.getRoom().setStatus(RoomEntity.RoomStatus.AVAILABLE_SOON);
        roomRepo.save(booking.getRoom());
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "CONTRACT_RENEWAL_REJECTED",
                "Không gia hạn hợp đồng",
                "Chủ trọ đã xác nhận không gia hạn hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse terminateContractEarly(
            Long roomId,
            TerminateContractEarlyRequest req,
            Long actorId,
            UserEntity.Role actorRole
    ) {
        contractLifecycleService.syncNow();

        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Không tìm thấy phòng", 404));

        boolean isOwner = room.getLandlord().getId().equals(actorId);
        if (!isOwner) {
            throw new AppException("Bạn không có quyền kết thúc hợp đồng của phòng này", 403);
        }

        RentalBookingEntity booking = bookingRepo.findByRoom_IdAndStatusIn(roomId, ACTIVE_CONTRACT_STATUSES).stream()
                .findFirst()
                .orElseThrow(() -> new AppException("Phòng này hiện không có hợp đồng thuê đang hiệu lực", 400));
        if (booking.getStatus() != RentalBookingEntity.Status.ACTIVE) {
            throw new AppException("Chỉ có thể yêu cầu kết thúc sớm khi hợp đồng đang ở trạng thái hiệu lực");
        }

        String reason = blankToNull(req != null ? req.getNote() : null);
        if (reason == null) {
            throw new AppException("Vui lòng nhập lý do kết thúc hợp đồng sớm");
        }
        if (booking.getStatus() == RentalBookingEntity.Status.EARLY_TERMINATION_PENDING) {
            throw new AppException("Yêu cầu kết thúc hợp đồng sớm đang chờ admin duyệt");
        }

        booking.setStatus(RentalBookingEntity.Status.EARLY_TERMINATION_PENDING);
        booking.setLandlordNote(reason);
        booking.setPaymentMessage("Chủ trọ đã gửi yêu cầu kết thúc hợp đồng sớm. Đang chờ admin duyệt.");

        bookingRepo.save(booking);

        userRepo.findByRole(UserEntity.Role.ADMIN).forEach(admin -> notificationService.notifyUser(
                admin,
                "BOOKING_UPDATED",
                "Có yêu cầu kết thúc hợp đồng sớm",
                "Chủ trọ đã gửi yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + room.getTitle() + "\".",
                booking.getId()
        ));

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Có yêu cầu kết thúc hợp đồng sớm",
                "Chủ trọ đã gửi yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + room.getTitle() + "\". Lý do: " + reason,
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse approveEarlyTermination(Long bookingId, String note, Long actorId, UserEntity.Role actorRole) {
        RentalBookingEntity booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
        if (actorRole != UserEntity.Role.ADMIN) {
            throw new AppException("Chỉ admin mới có quyền duyệt kết thúc hợp đồng sớm", 403);
        }
        if (booking.getStatus() != RentalBookingEntity.Status.EARLY_TERMINATION_PENDING) {
            throw new AppException("Hợp đồng này hiện không ở trạng thái chờ duyệt kết thúc sớm");
        }

        booking.setStatus(RentalBookingEntity.Status.COMPLETED);
        booking.setPaymentMessage("Admin đã duyệt kết thúc hợp đồng sớm. Phòng đang tạm ẩn, chủ trọ có thể hiện lại để nhận khách mới.");
        if (blankToNull(note) != null) {
            booking.setLandlordNote((booking.getLandlordNote() != null ? booking.getLandlordNote() + "\n\n" : "") + "Ghi chú admin: " + note.trim());
        }

        RoomEntity room = booking.getRoom();
        room.setStatus(RoomEntity.RoomStatus.HIDDEN_REVIEW);
        roomRepo.save(room);
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Yêu cầu kết thúc hợp đồng sớm đã được duyệt",
                "Admin đã duyệt yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + room.getTitle() + "\".",
                booking.getId()
        );
        notificationService.notifyUser(
                booking.getLandlord(),
                "BOOKING_UPDATED",
                "Yêu cầu kết thúc hợp đồng sớm đã được duyệt",
                "Admin đã duyệt yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + room.getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse rejectEarlyTermination(Long bookingId, String note, Long actorId, UserEntity.Role actorRole) {
        RentalBookingEntity booking = bookingRepo.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
        if (actorRole != UserEntity.Role.ADMIN) {
            throw new AppException("Chỉ admin mới có quyền từ chối kết thúc hợp đồng sớm", 403);
        }
        if (booking.getStatus() != RentalBookingEntity.Status.EARLY_TERMINATION_PENDING) {
            throw new AppException("Hợp đồng này hiện không ở trạng thái chờ duyệt kết thúc sớm");
        }

        booking.setStatus(RentalBookingEntity.Status.ACTIVE);
        booking.setPaymentMessage("Admin đã từ chối yêu cầu kết thúc hợp đồng sớm. Hợp đồng tiếp tục có hiệu lực.");
        if (blankToNull(note) != null) {
            booking.setLandlordNote((booking.getLandlordNote() != null ? booking.getLandlordNote() + "\n\n" : "") + "Ghi chú admin: " + note.trim());
        }

        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Yêu cầu kết thúc hợp đồng sớm đã bị từ chối",
                "Admin đã từ chối yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\".",
                booking.getId()
        );
        notificationService.notifyUser(
                booking.getLandlord(),
                "BOOKING_UPDATED",
                "Yêu cầu kết thúc hợp đồng sớm đã bị từ chối",
                "Admin đã từ chối yêu cầu kết thúc sớm hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse updateContractDraft(
            Long id,
            UpdateBookingContractDraftRequest req,
            Long actorId,
            UserEntity.Role actorRole
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));
        ensureLandlordOrAdmin(booking, actorId, actorRole);

        if (booking.getStatus() != RentalBookingEntity.Status.REQUESTED) {
            throw new AppException("Chỉ có thể cập nhật hợp đồng ở bước xem trước khi yêu cầu đặt cọc");
        }

        if (req.getMonthlyRent() != null) {
            booking.setMonthlyRent(req.getMonthlyRent());
        }
        if (req.getDepositAmount() != null) {
            booking.setDepositAmount(req.getDepositAmount());
        }
        if (req.getElectricPrice() != null) {
            booking.getRoom().setElectricPrice(req.getElectricPrice());
        }
        if (req.getWaterPrice() != null) {
            booking.getRoom().setWaterPrice(req.getWaterPrice());
        }
        if (req.getOtherFees() != null) {
            booking.getRoom().setOtherFees(req.getOtherFees());
        }
        booking.setContractMoveInRules(req.getMoveInRules().trim());
        booking.setContractServiceNotes(req.getServiceNotes().trim());
        booking.setContractAdditionalTerms(blankToNull(req.getAdditionalTerms()));
        booking.setContractTerms(buildContractTerms(
                booking.getContractMoveInRules(),
                booking.getMonthlyRent(),
                booking.getDepositAmount(),
                booking.getRoom().getElectricPrice(),
                booking.getRoom().getWaterPrice(),
                booking.getRoom().getOtherFees(),
                booking.getContractServiceNotes(),
                booking.getContractAdditionalTerms()
        ));
        booking.setPaymentMessage("Chủ trọ đã cập nhật hợp đồng để người thuê xem và phản hồi trước khi chuyển sang bước đặt cọc.");
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Chủ trọ đã cập nhật hợp đồng",
                "Chủ trọ vừa cập nhật nội dung hợp đồng thuê phòng \"" + booking.getRoom().getTitle() + "\".",
                booking.getId()
        );

        return RentalBookingResponse.from(booking);
    }

    public RentalBookingResponse confirmCashDeposit(
            Long id,
            ConfirmCashDepositRequest req,
            Long actorId,
            UserEntity.Role actorRole
    ) {
        RentalBookingEntity booking = bookingRepo.findById(id)
                .orElseThrow(() -> new AppException("Không tìm thấy đơn thuê phòng", 404));
        ensureLandlordOrAdmin(booking, actorId, actorRole);
        if (!"CASH".equalsIgnoreCase(booking.getPaymentProvider())) {
            throw new AppException("Đơn thuê này không dùng phương thức đặt cọc tiền mặt");
        }
        if (booking.getStatus() != RentalBookingEntity.Status.PENDING_PAYMENT) {
            throw new AppException("Đơn thuê này chưa ở trạng thái chờ xác nhận nhận cọc");
        }

        booking.setStatus(RentalBookingEntity.Status.DEPOSIT_PAID);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.PAID);
        booking.setDepositPaidAt(now());
        booking.setLandlordNote(blankToNull(req.getReceiptNote()));
        booking.setPaymentMessage("Chủ trọ đã xác nhận nhận cọc tiền mặt/chuyển khoản trực tiếp.");
        bookingRepo.save(booking);

        notificationService.notifyUser(
                booking.getSeeker(),
                "BOOKING_UPDATED",
                "Chủ trọ đã xác nhận nhận cọc",
                "Chủ trọ đã xác nhận nhận cọc cho phòng \"" + booking.getRoom().getTitle() + "\".",
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
                || booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID)
                && verification.isSuccess()) {
            return ipnResponse("02", "Order already confirmed");
        }

        applyPaymentResult(booking, verification, true);
        return ipnResponse("00", "Confirm Success");
    }

    private void applyFreshPaymentLink(RentalBookingEntity booking, HttpServletRequest request) {
        VnpayPaymentService.PaymentCreationResult payment = vnpayPaymentService.createDepositPayment(booking, request);
        booking.setStatus(RentalBookingEntity.Status.PENDING_PAYMENT);
        booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.PENDING);
        booking.setPaymentOrderId(payment.getOrderId());
        booking.setPaymentRequestId(payment.getRequestId());
        booking.setPaymentPayUrl(payment.getPayUrl());
        booking.setPaymentDeeplink(null);
        booking.setPaymentQrCodeUrl(null);
        booking.setPaymentTransId(null);
        booking.setPaymentMessage("Chủ trọ đã chấp thuận yêu cầu thuê. Vui lòng đọc điều khoản và thanh toán cọc để giữ phòng.");
        booking.setPaymentResultCode(payment.getResultCode());
    }

    private void ensureParticipant(RentalBookingEntity booking, UserPrincipal principal) {
        boolean allowed = principal.getRole() == UserEntity.Role.ADMIN
                || booking.getSeeker().getId().equals(principal.getId())
                || booking.getLandlord().getId().equals(principal.getId());
        if (!allowed) {
            throw new AppException("Bạn không có quyền xem đơn thuê này", 403);
        }
    }

    private void ensureLandlordOrAdmin(RentalBookingEntity booking, Long actorId, UserEntity.Role actorRole) {
        boolean isOwner = booking.getLandlord().getId().equals(actorId);
        boolean isAdmin = actorRole == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xử lý đơn này", 403);
        }
    }

    private void ensureNoActiveContract(Long roomId) {
        if (bookingRepo.existsByRoom_IdAndStatusIn(roomId, ACTIVE_CONTRACT_STATUSES)) {
            throw new AppException("Phòng này hiện đang có hợp đồng thuê còn hiệu lực");
        }
    }

    private LocalDate resolveAvailableFrom(Long roomId) {
        return bookingRepo.findByRoom_IdAndStatusIn(roomId, ACTIVE_CONTRACT_STATUSES).stream()
                .map(contractLifecycleService::contractEndDate)
                .max(LocalDate::compareTo)
                .map(date -> date.plusDays(1))
                .orElse(null);
    }

    private BigDecimal defaultDepositAmount(RoomEntity room) {
        return room.getPrice();
    }

    private String generateContractCode() {
        return "HD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    private String buildContractTerms(
            String moveInRules,
            BigDecimal monthlyRent,
            BigDecimal depositAmount,
            BigDecimal electricPrice,
            BigDecimal waterPrice,
            BigDecimal otherFees,
            String serviceNotes,
            String additionalTerms
    ) {
        return ContractTermsFormatter.format(
                moveInRules,
                monthlyRent,
                depositAmount,
                electricPrice,
                waterPrice,
                otherFees,
                serviceNotes,
                additionalTerms
        );
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
                        "Đã ghi nhận tiền cọc",
                        "Hệ thống đã ghi nhận tiền cọc cho phòng \"" + booking.getRoom().getTitle() + "\".",
                        booking.getId()
                );
            }
        } else {
            if (isExpiredOrCancelledPayment(verification)) {
                markBookingPaymentExpired(booking);
            } else {
                booking.setPaymentStatus(RentalBookingEntity.PaymentStatus.FAILED);
                if (booking.getStatus() != RentalBookingEntity.Status.DEPOSIT_PAID) {
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
        if (booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID) {
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
        if (value == null) {
            return fallback;
        }
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
