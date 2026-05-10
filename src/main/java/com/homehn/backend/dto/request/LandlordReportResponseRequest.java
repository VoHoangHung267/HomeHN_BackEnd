package com.homehn.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LandlordReportResponseRequest {
    @NotNull
    private ResponseType responseType;

    @NotBlank
    @Size(max = 1000)
    private String responseNote;

    public enum ResponseType {
        WILL_FIX,
        CONTEST
    }
}
