package com.homehn.backend.dto.response;

import com.homehn.backend.entity.RentalBookingEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalBookingResponse {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Pattern TXN_REF_TIMESTAMP_PATTERN = Pattern.compile("(\\d{13})$");

    private Long id;
    private Long roomId;
    private String roomTitle;
    private String roomPrimaryImage;
    private com.homehn.backend.entity.RoomEntity.RoomStatus roomStatus;
    private Long seekerId;
    private String seekerName;
    private Long landlordId;
    private String landlordName;
    private String tenantFullName;
    private String tenantPhone;
    private String tenantEmail;
    private String tenantIdentityNumber;
    private LocalDate moveInDate;
    private LocalDate contractEndDate;
    private Integer leaseMonths;
    private Integer occupantCount;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private BigDecimal electricPrice;
    private BigDecimal waterPrice;
    private BigDecimal otherFees;
    private String contractCode;
    private String contractTerms;
    private String contractMoveInRules;
    private String contractServiceNotes;
    private String contractAdditionalTerms;
    private String note;
    private String landlordNote;
    private RentalBookingEntity.Status status;
    private RentalBookingEntity.PaymentStatus paymentStatus;
    private String paymentProvider;
    private String paymentMethod;
    private String paymentOrderId;
    private String paymentPayUrl;
    private String paymentDeeplink;
    private String paymentQrCodeUrl;
    private Long paymentTransId;
    private Integer paymentResultCode;
    private String paymentMessage;
    private LocalDateTime depositPaidAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static RentalBookingResponse from(RentalBookingEntity booking) {
        String primaryImage = booking.getRoom().getImages().stream()
                .filter(img -> img.isPrimary())
                .map(img -> img.getImageUrl())
                .findFirst()
                .orElse(null);

        return RentalBookingResponse.builder()
                .id(booking.getId())
                .roomId(booking.getRoom().getId())
                .roomTitle(booking.getRoom().getTitle())
                .roomPrimaryImage(primaryImage)
                .roomStatus(booking.getRoom().getStatus())
                .seekerId(booking.getSeeker().getId())
                .seekerName(booking.getSeeker().getFullName())
                .landlordId(booking.getLandlord().getId())
                .landlordName(booking.getLandlord().getFullName())
                .tenantFullName(booking.getTenantFullName())
                .tenantPhone(booking.getTenantPhone())
                .tenantEmail(booking.getTenantEmail())
                .tenantIdentityNumber(booking.getTenantIdentityNumber())
                .moveInDate(booking.getMoveInDate())
                .contractEndDate(resolveContractEndDate(booking))
                .leaseMonths(booking.getLeaseMonths())
                .occupantCount(booking.getOccupantCount())
                .monthlyRent(booking.getMonthlyRent())
                .depositAmount(booking.getDepositAmount())
                .electricPrice(booking.getRoom().getElectricPrice())
                .waterPrice(booking.getRoom().getWaterPrice())
                .otherFees(booking.getRoom().getOtherFees())
                .contractCode(booking.getContractCode())
                .contractTerms(booking.getContractTerms())
                .contractMoveInRules(booking.getContractMoveInRules())
                .contractServiceNotes(booking.getContractServiceNotes())
                .contractAdditionalTerms(booking.getContractAdditionalTerms())
                .note(booking.getNote())
                .landlordNote(booking.getLandlordNote())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .paymentProvider(booking.getPaymentProvider())
                .paymentMethod(booking.getPaymentProvider())
                .paymentOrderId(booking.getPaymentOrderId())
                .paymentPayUrl(booking.getPaymentPayUrl())
                .paymentDeeplink(booking.getPaymentDeeplink())
                .paymentQrCodeUrl(booking.getPaymentQrCodeUrl())
                .paymentTransId(booking.getPaymentTransId())
                .paymentResultCode(booking.getPaymentResultCode())
                .paymentMessage(booking.getPaymentMessage())
                .depositPaidAt(booking.getDepositPaidAt())
                .createdAt(normalizeCreatedAt(booking))
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private static LocalDate resolveContractEndDate(RentalBookingEntity booking) {
        if (booking.getMoveInDate() == null || booking.getLeaseMonths() == null) {
            return null;
        }
        return booking.getMoveInDate().plusMonths(booking.getLeaseMonths()).minusDays(1);
    }

    private static LocalDateTime normalizeCreatedAt(RentalBookingEntity booking) {
        LocalDateTime createdAt = booking.getCreatedAt();
        if (createdAt == null || booking.getPaymentOrderId() == null) {
            return createdAt;
        }

        Matcher matcher = TXN_REF_TIMESTAMP_PATTERN.matcher(booking.getPaymentOrderId());
        if (!matcher.find()) {
            return createdAt;
        }

        try {
            long createdMillis = Long.parseLong(matcher.group(1));
            LocalDateTime derivedCreatedAt = LocalDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(createdMillis),
                    APP_ZONE
            );

            long diffHours = Math.abs(Duration.between(createdAt, derivedCreatedAt).toHours());
            return diffHours == 7 ? derivedCreatedAt : createdAt;
        } catch (NumberFormatException ex) {
            return createdAt;
        }
    }
}
