package com.homehn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ConfirmCashDepositRequest {
    @NotBlank(message = "Vui lòng nhập ghi chú biên nhận cọc")
    private String receiptNote;
}
