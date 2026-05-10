package com.homehn.backend.dto.response;

import com.homehn.backend.entity.RentalBookingEntity;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalBookingResponse {
    private Long id;
    private Long roomId;
    private String roomTitle;
    private String roomPrimaryImage;
    private Long seekerId;
    private String seekerName;
    private Long landlordId;
    private String landlordName;
    private String tenantFullName;
    private String tenantPhone;
    private String tenantEmail;
    private String tenantIdentityNumber;
    private LocalDate moveInDate;
    private Integer leaseMonths;
    private Integer occupantCount;
    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private String contractCode;
    private String contractTerms;
    private String note;
    private String landlordNote;
    private RentalBookingEntity.Status status;
    private RentalBookingEntity.PaymentStatus paymentStatus;
    private String paymentProvider;
    private String paymentOrderId;
    private String paymentPayUrl;
    private String paymentDeeplink;
    private String paymentQrCodeUrl;
    private Long paymentTransId;
    private Integer paymentResultCode;
    private String paymentMessage;
    private LocalDateTime depositPaidAt;
    private LocalDateTime confirmedAt;
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
                .seekerId(booking.getSeeker().getId())
                .seekerName(booking.getSeeker().getFullName())
                .landlordId(booking.getLandlord().getId())
                .landlordName(booking.getLandlord().getFullName())
                .tenantFullName(booking.getTenantFullName())
                .tenantPhone(booking.getTenantPhone())
                .tenantEmail(booking.getTenantEmail())
                .tenantIdentityNumber(booking.getTenantIdentityNumber())
                .moveInDate(booking.getMoveInDate())
                .leaseMonths(booking.getLeaseMonths())
                .occupantCount(booking.getOccupantCount())
                .monthlyRent(booking.getMonthlyRent())
                .depositAmount(booking.getDepositAmount())
                .contractCode(booking.getContractCode())
                .contractTerms(booking.getContractTerms())
                .note(booking.getNote())
                .landlordNote(booking.getLandlordNote())
                .status(booking.getStatus())
                .paymentStatus(booking.getPaymentStatus())
                .paymentProvider(booking.getPaymentProvider())
                .paymentOrderId(booking.getPaymentOrderId())
                .paymentPayUrl(booking.getPaymentPayUrl())
                .paymentDeeplink(booking.getPaymentDeeplink())
                .paymentQrCodeUrl(booking.getPaymentQrCodeUrl())
                .paymentTransId(booking.getPaymentTransId())
                .paymentResultCode(booking.getPaymentResultCode())
                .paymentMessage(booking.getPaymentMessage())
                .depositPaidAt(booking.getDepositPaidAt())
                .confirmedAt(booking.getConfirmedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
