package com.homehn.backend.dto.response;

import com.homehn.backend.entity.ContractTemplateEntity;
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
public class ContractTemplateResponse {
    private Long id;
    private String name;
    private String content;
    private BigDecimal defaultMonthlyRent;
    private BigDecimal defaultDepositAmount;
    private BigDecimal defaultElectricPrice;
    private BigDecimal defaultWaterPrice;
    private BigDecimal defaultOtherFees;
    private String moveInRules;
    private String serviceNotes;
    private String additionalTerms;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ContractTemplateResponse from(ContractTemplateEntity entity) {
        return ContractTemplateResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .content(entity.getContent())
                .defaultMonthlyRent(entity.getDefaultMonthlyRent())
                .defaultDepositAmount(entity.getDefaultDepositAmount())
                .defaultElectricPrice(entity.getDefaultElectricPrice())
                .defaultWaterPrice(entity.getDefaultWaterPrice())
                .defaultOtherFees(entity.getDefaultOtherFees())
                .moveInRules(entity.getMoveInRules())
                .serviceNotes(entity.getServiceNotes())
                .additionalTerms(entity.getAdditionalTerms())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
