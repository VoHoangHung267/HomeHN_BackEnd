package com.homehn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class UpdateBookingContractDraftRequest {

    private BigDecimal monthlyRent;
    private BigDecimal depositAmount;
    private BigDecimal electricPrice;
    private BigDecimal waterPrice;
    private BigDecimal otherFees;

    @NotBlank(message = "Giờ giấc không được để trống")
    private String moveInRules;

    @NotBlank(message = "Thông tin dịch vụ không được để trống")
    private String serviceNotes;

    private String additionalTerms;
}
