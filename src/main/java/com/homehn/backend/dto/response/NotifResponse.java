package com.homehn.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotifResponse {
    private Long id;
    private String type, title, message;
    private boolean isRead;
    private Long relatedId;
    private String relatedType;
    private String actionUrl;
    private LocalDateTime createdAt;
}
