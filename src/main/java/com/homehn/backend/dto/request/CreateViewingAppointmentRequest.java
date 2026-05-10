package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateViewingAppointmentRequest {
    @NotNull
    @Future
    private LocalDateTime requestedAt;

    private String message;
}
