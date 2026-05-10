package com.homehn.backend.dto.response;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ChatRoomResponse {
    private Long          id;
    private Long          roomId;
    private String        roomTitle;
    private String        roomPrimaryImage;
    private Long          seekerId;
    private String        seekerName;
    private Long          landlordId;
    private String        landlordName;
    private String        lastMessage;
    private LocalDateTime lastMessageAt;
    private int           unreadCount;
}