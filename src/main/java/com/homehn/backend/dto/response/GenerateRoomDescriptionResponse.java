package com.homehn.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GenerateRoomDescriptionResponse {
    private String suggestedTitle;
    private String suggestedDescription;
}
