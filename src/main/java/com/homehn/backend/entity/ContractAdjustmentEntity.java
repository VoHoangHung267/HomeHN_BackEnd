package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "contract_adjustments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdjustmentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private RentalBookingEntity booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seeker_id", nullable = false)
    private UserEntity seeker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private UserEntity landlord;

    @Enumerated(EnumType.STRING)
    @Column(name = "proposer_role", nullable = false, length = 20)
    private ProposerRole proposerRole;

    @Column(name = "extension_months")
    private Integer extensionMonths;

    @Column(name = "proposed_monthly_rent", precision = 12, scale = 2)
    private BigDecimal proposedMonthlyRent;

    @Column(name = "proposed_deposit_amount", precision = 12, scale = 2)
    private BigDecimal proposedDepositAmount;

    @Column(name = "proposed_electric_price", precision = 10, scale = 2)
    private BigDecimal proposedElectricPrice;

    @Column(name = "proposed_water_price", precision = 10, scale = 2)
    private BigDecimal proposedWaterPrice;

    @Column(name = "proposed_other_fees", precision = 10, scale = 2)
    private BigDecimal proposedOtherFees;

    @Column(name = "proposed_contract_terms", columnDefinition = "TEXT")
    private String proposedContractTerms;

    @Column(name = "proposed_move_in_rules", columnDefinition = "TEXT")
    private String proposedMoveInRules;

    @Column(name = "proposed_service_notes", columnDefinition = "TEXT")
    private String proposedServiceNotes;

    @Column(name = "proposed_additional_terms", columnDefinition = "TEXT")
    private String proposedAdditionalTerms;

    @Column(name = "proposal_note", columnDefinition = "TEXT")
    private String proposalNote;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "responder_role", length = 20)
    private ProposerRole responderRole;

    @Column(name = "response_note", columnDefinition = "TEXT")
    private String responseNote;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

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

    public enum ProposerRole {
        SEEKER,
        LANDLORD
    }

    public enum Status {
        PENDING,
        APPROVED,
        REJECTED
    }
}
