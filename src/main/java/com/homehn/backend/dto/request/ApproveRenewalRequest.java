package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ApproveRenewalRequest {

    @NotNull(message = "Vui lòng nhập thời hạn gia hạn đã chốt")
    @Min(value = 1, message = "Gia hạn tối thiểu 1 tháng")
    @Max(value = 36, message = "Gia hạn tối đa 36 tháng")
    private Integer leaseMonths;

    private String contractTerms;
    private String note;
}
