package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "contract_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplateEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "landlord_id", nullable = false)
    private UserEntity landlord;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "default_monthly_rent", precision = 12, scale = 2)
    private java.math.BigDecimal defaultMonthlyRent;

    @Column(name = "default_deposit_amount", precision = 12, scale = 2)
    private java.math.BigDecimal defaultDepositAmount;

    @Column(name = "default_electric_price", precision = 10, scale = 2)
    private java.math.BigDecimal defaultElectricPrice;

    @Column(name = "default_water_price", precision = 10, scale = 2)
    private java.math.BigDecimal defaultWaterPrice;

    @Column(name = "default_other_fees", precision = 10, scale = 2)
    private java.math.BigDecimal defaultOtherFees;

    @Column(name = "move_in_rules", columnDefinition = "TEXT")
    private String moveInRules;

    @Column(name = "service_notes", columnDefinition = "TEXT")
    private String serviceNotes;

    @Column(name = "additional_terms", columnDefinition = "TEXT")
    private String additionalTerms;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
