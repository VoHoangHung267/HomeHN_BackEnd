package com.homehn.backend.dto.request;

import com.homehn.backend.entity.ViewingAppointmentEntity;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class UpdateViewingAppointmentRequest {
    @NotNull
    private ViewingAppointmentEntity.Status status;

    private LocalDateTime requestedAt;
    private String note;
}
