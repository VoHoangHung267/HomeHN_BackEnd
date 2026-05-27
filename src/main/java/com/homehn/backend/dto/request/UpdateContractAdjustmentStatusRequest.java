package com.homehn.backend.dto.request;

import com.homehn.backend.entity.ContractAdjustmentEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateContractAdjustmentStatusRequest {

    @NotNull(message = "Vui lòng chọn trạng thái phản hồi")
    private ContractAdjustmentEntity.Status status;

    private String responseNote;
}
