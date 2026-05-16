package com.homehn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatAssistantResponse {
    private String answer;
    private String note;
    private String actionLabel;
    private String actionUrl;
}
