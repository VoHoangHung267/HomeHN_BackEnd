package com.homehn.backend.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty("isRead")
    private boolean isRead;
    private Long relatedId;
    private String relatedType;
    private String actionUrl;
    private LocalDateTime createdAt;
}
