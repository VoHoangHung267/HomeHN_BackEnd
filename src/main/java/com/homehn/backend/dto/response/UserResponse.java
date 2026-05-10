package com.homehn.backend.dto.response;

import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.security.UserPrincipal;
import lombok.*;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserResponse {
    private Long            id;
    private String          email;
    private String          fullName;
    private String          phone;
    private String          avatarUrl;
    private UserEntity.Role role;
    private Boolean         isActive;
    private LocalDateTime   createdAt;

    public static UserResponse from(UserEntity u) {
        return UserResponse.builder()
                .id(u.getId())
                .email(u.getEmail())
                .fullName(u.getFullName())
                .phone(u.getPhone())
                .avatarUrl(u.getAvatarUrl())
                .role(u.getRole())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build();
    }

    public static UserResponse fromPrincipal(UserPrincipal p) {
        return UserResponse.builder()
                .id(p.getId())
                .email(p.getUsername())
                .fullName(p.getFullName())
                .phone(p.getPhone())
                .avatarUrl(p.getAvatarUrl())
                .role(p.getRole())
                .build();
    }
}