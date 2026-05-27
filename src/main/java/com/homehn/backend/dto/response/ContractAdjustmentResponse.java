package com.homehn.backend.dto.response;

import com.homehn.backend.entity.ContractAdjustmentEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractAdjustmentResponse {
    private Long id;
    private Long bookingId;
    private ContractAdjustmentEntity.ProposerRole proposerRole;
    private Integer extensionMonths;
    private BigDecimal proposedMonthlyRent;
    private BigDecimal proposedDepositAmount;
    private BigDecimal proposedElectricPrice;
    private BigDecimal proposedWaterPrice;
    private BigDecimal proposedOtherFees;
    private String proposedContractTerms;
    private String proposedMoveInRules;
    private String proposedServiceNotes;
    private String proposedAdditionalTerms;
    private String proposalNote;
    private ContractAdjustmentEntity.Status status;
    private ContractAdjustmentEntity.ProposerRole responderRole;
    private String responseNote;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContractAdjustmentResponse from(ContractAdjustmentEntity entity) {
        return ContractAdjustmentResponse.builder()
                .id(entity.getId())
                .bookingId(entity.getBooking().getId())
                .proposerRole(entity.getProposerRole())
                .extensionMonths(entity.getExtensionMonths())
                .proposedMonthlyRent(entity.getProposedMonthlyRent())
                .proposedDepositAmount(entity.getProposedDepositAmount())
                .proposedElectricPrice(entity.getProposedElectricPrice())
                .proposedWaterPrice(entity.getProposedWaterPrice())
                .proposedOtherFees(entity.getProposedOtherFees())
                .proposedContractTerms(entity.getProposedContractTerms())
                .proposedMoveInRules(entity.getProposedMoveInRules())
                .proposedServiceNotes(entity.getProposedServiceNotes())
                .proposedAdditionalTerms(entity.getProposedAdditionalTerms())
                .proposalNote(entity.getProposalNote())
                .status(entity.getStatus())
                .responderRole(entity.getResponderRole())
                .responseNote(entity.getResponseNote())
                .respondedAt(entity.getRespondedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
