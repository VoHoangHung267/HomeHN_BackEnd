package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateRentalBookingRequest {

    @NotBlank(message = "Vui lòng nhập họ tên người thuê")
    private String tenantFullName;

    @NotBlank(message = "Vui lòng nhập số điện thoại")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String tenantPhone;

    @Email(message = "Email không hợp lệ")
    private String tenantEmail;

    @Pattern(regexp = "^$|^[0-9]{9}$|^[0-9]{12}$", message = "CCCD/CMND phải gồm 9 hoặc 12 chữ số")
    private String tenantIdentityNumber;

    @NotNull(message = "Vui lòng chọn ngày dự kiến vào ở")
    @FutureOrPresent(message = "Ngày vào ở phải từ hôm nay trở đi")
    private LocalDate moveInDate;

    @NotNull(message = "Vui lòng nhập thời hạn thuê")
    @Min(value = 1, message = "Thời hạn thuê tối thiểu là 1 tháng")
    @Max(value = 36, message = "Thời hạn thuê tối đa là 36 tháng")
    private Integer leaseMonths;

    @NotNull(message = "Vui lòng nhập số người ở")
    @Min(value = 1, message = "Số người ở tối thiểu là 1")
    @Max(value = 20, message = "Số người ở không hợp lệ")
    private Integer occupantCount;

    private String note;
}
