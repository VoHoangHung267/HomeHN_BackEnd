package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CreateContractAdjustmentRequest {

    @Min(value = 1, message = "Gia hạn thêm tối thiểu 1 tháng")
    @Max(value = 36, message = "Gia hạn thêm tối đa 36 tháng")
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
}
