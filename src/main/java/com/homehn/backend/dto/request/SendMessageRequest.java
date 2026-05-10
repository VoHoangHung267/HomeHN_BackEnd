package com.homehn.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class SendMessageRequest {

    @NotNull(message = "Chat room ID không được để trống")
    private Long chatRoomId;

    @NotBlank(message = "Nội dung tin nhắn không được để trống")
    private String content;
}