package com.homehn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class SaveContractTemplateRequest {

    @NotBlank(message = "Tên mẫu hợp đồng không được để trống")
    @Size(max = 255, message = "Tên mẫu hợp đồng tối đa 255 ký tự")
    private String name;

    private BigDecimal defaultMonthlyRent;
    private BigDecimal defaultDepositAmount;
    private BigDecimal defaultElectricPrice;
    private BigDecimal defaultWaterPrice;
    private BigDecimal defaultOtherFees;

    @NotBlank(message = "Giờ giấc không được để trống")
    private String moveInRules;

    @NotBlank(message = "Thông tin dịch vụ không được để trống")
    private String serviceNotes;

    private String additionalTerms;
}
