package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RequestRenewalRequest {

    @NotNull(message = "Vui lòng nhập thời hạn muốn gia hạn")
    @Min(value = 1, message = "Gia hạn tối thiểu 1 tháng")
    @Max(value = 36, message = "Gia hạn tối đa 36 tháng")
    private Integer leaseMonths;

    private String note;
}
