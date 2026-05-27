package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "rental_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RentalBookingEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private UserEntity landlord;

    @Column(name = "tenant_full_name", nullable = false)
    private String tenantFullName;

    @Column(name = "tenant_phone", nullable = false)
    private String tenantPhone;

    @Column(name = "tenant_email")
    private String tenantEmail;

    @Column(name = "tenant_identity_number")
    private String tenantIdentityNumber;

    @Column(name = "move_in_date", nullable = false)
    private LocalDate moveInDate;

    @Column(name = "lease_months", nullable = false)
    private Integer leaseMonths;

    @Column(name = "occupant_count", nullable = false)
    private Integer occupantCount;

    @Column(name = "monthly_rent", nullable = false, precision = 12, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "contract_code", nullable = false, unique = true, length = 64)
    private String contractCode;

    @Column(name = "contract_terms", columnDefinition = "TEXT")
    private String contractTerms;

    @Column(name = "contract_move_in_rules", columnDefinition = "TEXT")
    private String contractMoveInRules;

    @Column(name = "contract_service_notes", columnDefinition = "TEXT")
    private String contractServiceNotes;

    @Column(name = "contract_additional_terms", columnDefinition = "TEXT")
    private String contractAdditionalTerms;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "landlord_note", columnDefinition = "TEXT")
    private String landlordNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Status status = Status.PENDING_PAYMENT;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    @Column(name = "payment_provider", length = 32)
    @Builder.Default
    private String paymentProvider = "VNPAY";

    @Column(name = "payment_order_id", unique = true, length = 100)
    private String paymentOrderId;

    @Column(name = "payment_request_id", unique = true, length = 100)
    private String paymentRequestId;

    @Column(name = "payment_pay_url", columnDefinition = "TEXT")
    private String paymentPayUrl;

    @Column(name = "payment_deeplink", columnDefinition = "TEXT")
    private String paymentDeeplink;

    @Column(name = "payment_qr_code_url", columnDefinition = "TEXT")
    private String paymentQrCodeUrl;

    @Column(name = "payment_trans_id")
    private Long paymentTransId;

    @Column(name = "payment_result_code")
    private Integer paymentResultCode;

    @Column(name = "payment_message", columnDefinition = "TEXT")
    private String paymentMessage;

    @Column(name = "deposit_paid_at")
    private LocalDateTime depositPaidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Status {
        REQUESTED,
        PENDING_PAYMENT,
        DEPOSIT_PAID,
        ACTIVE,
        EXPIRING_SOON,
        RENEWAL_PENDING,
        REJECTED,
        CANCELLED,
        PAYMENT_FAILED,
        COMPLETED
    }

    public enum PaymentStatus {
        PENDING,
        PAID,
        FAILED,
        CANCELLED
    }
}
