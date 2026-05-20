package com.homehn.backend.dto.request;

import com.homehn.backend.entity.UserEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @Email(message = "Email không hợp lệ")
    @NotBlank(message = "Email không được để trống")
    private String email;

    @Size(min = 8, message = "Mật khẩu ít nhất 8 ký tự")
    @NotBlank(message = "Mật khẩu không được để trống")
    private String password;

    @NotBlank(message = "Họ tên không được để trống")
    private String fullName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @NotBlank(message = "Mã xác thực không được để trống")
    @Pattern(regexp = "^\\d{6}$", message = "Mã xác thực không hợp lệ")
    private String verificationCode;

    private UserEntity.Role role = UserEntity.Role.SEEKER;

    public UserEntity.Role getRole() {
        return role == null ? UserEntity.Role.SEEKER : role;
    }
}
