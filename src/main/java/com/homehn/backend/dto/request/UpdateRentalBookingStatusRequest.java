package com.homehn.backend.dto.request;

import com.homehn.backend.entity.RentalBookingEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateRentalBookingStatusRequest {

    @NotNull(message = "Vui lòng chọn trạng thái")
    private RentalBookingEntity.Status status;

    private String note;
}
