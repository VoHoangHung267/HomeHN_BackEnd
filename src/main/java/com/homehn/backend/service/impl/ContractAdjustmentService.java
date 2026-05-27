package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.CreateContractAdjustmentRequest;
import com.homehn.backend.dto.request.UpdateContractAdjustmentStatusRequest;
import com.homehn.backend.dto.response.ContractAdjustmentResponse;
import com.homehn.backend.entity.ContractAdjustmentEntity;
import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ContractAdjustmentRepository;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractAdjustmentService {

    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");

    private final ContractAdjustmentRepository adjustmentRepo;
    private final RentalBookingRepository bookingRepo;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ContractAdjustmentResponse> getByBooking(Long bookingId, UserPrincipal principal) {
        RentalBookingEntity booking = findBooking(bookingId);
        ensureParticipant(booking, principal);
        return adjustmentRepo.findByBooking_IdOrderByCreatedAtDesc(bookingId).stream()
                .map(ContractAdjustmentResponse::from)
                .toList();
    }

    public ContractAdjustmentResponse create(Long bookingId, CreateContractAdjustmentRequest req, UserPrincipal principal) {
        RentalBookingEntity booking = findBooking(bookingId);
        ensureParticipant(booking, principal);
        ensureAdjustmentStage(booking);
        if (adjustmentRepo.findFirstByBooking_IdAndStatusOrderByCreatedAtDesc(bookingId, ContractAdjustmentEntity.Status.PENDING).isPresent()) {
            throw new AppException("Đang có một đề xuất điều chỉnh hợp đồng chờ phản hồi");
        }
        validateProposal(req);

        ContractAdjustmentEntity.ProposerRole proposerRole = resolveRole(booking, principal);
        ContractAdjustmentEntity adjustment = adjustmentRepo.save(ContractAdjustmentEntity.builder()
                .booking(booking)
                .room(booking.getRoom())
                .seeker(booking.getSeeker())
                .landlord(booking.getLandlord())
                .proposerRole(proposerRole)
                .extensionMonths(req.getExtensionMonths())
                .proposedMonthlyRent(req.getProposedMonthlyRent())
                .proposedDepositAmount(req.getProposedDepositAmount())
                .proposedElectricPrice(req.getProposedElectricPrice())
                .proposedWaterPrice(req.getProposedWaterPrice())
                .proposedOtherFees(req.getProposedOtherFees())
                .proposedContractTerms(blankToNull(req.getProposedContractTerms()))
                .proposalNote(blankToNull(req.getProposalNote()))
                .status(ContractAdjustmentEntity.Status.PENDING)
                .build());

        booking.setStatus(RentalBookingEntity.Status.RENEWAL_PENDING);
        booking.setPaymentMessage("Đang có đề xuất điều chỉnh hợp đồng chờ bên còn lại phản hồi.");
        bookingRepo.save(booking);

        notifyOtherSide(booking, proposerRole, "CONTRACT_ADJUSTMENT_CREATED", "Có đề xuất điều chỉnh hợp đồng mới");
        return ContractAdjustmentResponse.from(adjustment);
    }

    public ContractAdjustmentResponse updateStatus(
            Long adjustmentId,
            UpdateContractAdjustmentStatusRequest req,
            UserPrincipal principal
    ) {
        ContractAdjustmentEntity adjustment = adjustmentRepo.findById(adjustmentId)
                .orElseThrow(() -> new AppException("Không tìm thấy đề xuất điều chỉnh hợp đồng", 404));
        RentalBookingEntity booking = adjustment.getBooking();
        ensureParticipant(booking, principal);

        ContractAdjustmentEntity.ProposerRole actorRole = resolveRole(booking, principal);
        if (actorRole == adjustment.getProposerRole()) {
            throw new AppException("Bạn không thể tự phản hồi đề xuất do chính mình tạo");
        }
        if (adjustment.getStatus() != ContractAdjustmentEntity.Status.PENDING) {
            throw new AppException("Đề xuất này đã được xử lý");
        }
        if (req.getStatus() == ContractAdjustmentEntity.Status.PENDING) {
            throw new AppException("Trạng thái phản hồi không hợp lệ");
        }

        adjustment.setStatus(req.getStatus());
        adjustment.setResponderRole(actorRole);
        adjustment.setResponseNote(blankToNull(req.getResponseNote()));
        adjustment.setRespondedAt(LocalDateTime.now(APP_ZONE));

        if (req.getStatus() == ContractAdjustmentEntity.Status.APPROVED) {
            applyApprovedProposal(booking, adjustment);
            notifyOtherSide(booking, actorRole, "CONTRACT_ADJUSTMENT_APPROVED", "Đề xuất điều chỉnh hợp đồng đã được chấp thuận");
        } else {
            booking.setStatus(RentalBookingEntity.Status.EXPIRING_SOON);
            booking.setPaymentMessage("Một đề xuất điều chỉnh hợp đồng đã bị từ chối. Hai bên có thể gửi đề xuất mới hoặc chuyển sang cho thuê khách mới.");
            bookingRepo.save(booking);
            notifyOtherSide(booking, actorRole, "CONTRACT_ADJUSTMENT_REJECTED", "Đề xuất điều chỉnh hợp đồng đã bị từ chối");
        }

        return ContractAdjustmentResponse.from(adjustmentRepo.save(adjustment));
    }

    private void applyApprovedProposal(RentalBookingEntity booking, ContractAdjustmentEntity adjustment) {
        RoomEntity room = booking.getRoom();

        if (adjustment.getExtensionMonths() != null) {
            booking.setLeaseMonths(booking.getLeaseMonths() + adjustment.getExtensionMonths());
        }
        if (adjustment.getProposedMonthlyRent() != null) {
            booking.setMonthlyRent(adjustment.getProposedMonthlyRent());
            room.setPrice(adjustment.getProposedMonthlyRent());
        }
        if (adjustment.getProposedDepositAmount() != null) {
            booking.setDepositAmount(adjustment.getProposedDepositAmount());
        }
        if (adjustment.getProposedElectricPrice() != null) {
            room.setElectricPrice(adjustment.getProposedElectricPrice());
        }
        if (adjustment.getProposedWaterPrice() != null) {
            room.setWaterPrice(adjustment.getProposedWaterPrice());
        }
        if (adjustment.getProposedOtherFees() != null) {
            room.setOtherFees(adjustment.getProposedOtherFees());
        }
        if (adjustment.getProposedContractTerms() != null) {
            booking.setContractTerms(adjustment.getProposedContractTerms());
        }

        booking.setStatus(RentalBookingEntity.Status.ACTIVE);
        booking.setPaymentMessage("Đề xuất điều chỉnh hợp đồng đã được chấp thuận. Hợp đồng tiếp tục có hiệu lực với điều khoản mới.");
        room.setStatus(RoomEntity.RoomStatus.RENTED);
        bookingRepo.save(booking);
    }

    private void notifyOtherSide(
            RentalBookingEntity booking,
            ContractAdjustmentEntity.ProposerRole actorRole,
            String type,
            String title
    ) {
        if (actorRole == ContractAdjustmentEntity.ProposerRole.SEEKER) {
            notificationService.notifyUser(
                    booking.getLandlord(),
                    type,
                    title,
                    "Có cập nhật điều chỉnh hợp đồng cho phòng \"" + booking.getRoom().getTitle() + "\".",
                    booking.getId()
            );
        } else {
            notificationService.notifyUser(
                    booking.getSeeker(),
                    type,
                    title,
                    "Có cập nhật điều chỉnh hợp đồng cho phòng \"" + booking.getRoom().getTitle() + "\".",
                    booking.getId()
            );
        }
    }

    private RentalBookingEntity findBooking(Long bookingId) {
        return bookingRepo.findById(bookingId)
                .orElseThrow(() -> new AppException("Không tìm thấy hợp đồng thuê", 404));
    }

    private void ensureParticipant(RentalBookingEntity booking, UserPrincipal principal) {
        boolean allowed = principal.getRole() == UserEntity.Role.ADMIN
                || booking.getSeeker().getId().equals(principal.getId())
                || booking.getLandlord().getId().equals(principal.getId());
        if (!allowed) {
            throw new AppException("Bạn không có quyền xem hoặc chỉnh sửa hợp đồng này", 403);
        }
    }

    private void ensureAdjustmentStage(RentalBookingEntity booking) {
        if (booking.getStatus() != RentalBookingEntity.Status.EXPIRING_SOON
                && booking.getStatus() != RentalBookingEntity.Status.RENEWAL_PENDING) {
            throw new AppException("Chỉ có thể điều chỉnh hợp đồng khi đang trong giai đoạn sắp hết hạn hoặc gia hạn");
        }
    }

    private ContractAdjustmentEntity.ProposerRole resolveRole(RentalBookingEntity booking, UserPrincipal principal) {
        if (booking.getSeeker().getId().equals(principal.getId())) {
            return ContractAdjustmentEntity.ProposerRole.SEEKER;
        }
        return ContractAdjustmentEntity.ProposerRole.LANDLORD;
    }

    private void validateProposal(CreateContractAdjustmentRequest req) {
        boolean hasContent = req.getExtensionMonths() != null
                || req.getProposedMonthlyRent() != null
                || req.getProposedDepositAmount() != null
                || req.getProposedElectricPrice() != null
                || req.getProposedWaterPrice() != null
                || req.getProposedOtherFees() != null
                || blankToNull(req.getProposedContractTerms()) != null
                || blankToNull(req.getProposalNote()) != null;
        if (!hasContent) {
            throw new AppException("Vui lòng nhập ít nhất một điều khoản muốn điều chỉnh");
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
