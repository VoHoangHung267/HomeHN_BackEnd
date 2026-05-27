package com.homehn.backend.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRentalBookingStatusRequest {

    @NotNull(message = "Vui lòng chọn thao tác xử lý")
    private Action action;

    private String note;

    public enum Action {
        APPROVE,
        REJECT
    }
}
