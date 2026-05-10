package com.homehn.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageResponse {
    private Long          id;
    private Long          chatRoomId;
    private Long          senderId;
    private String        senderName;
    private String        senderAvatar;
    private String        content;
    private boolean       isRead;
    private LocalDateTime sentAt;
}
