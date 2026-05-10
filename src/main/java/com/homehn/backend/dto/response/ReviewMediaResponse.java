package com.homehn.backend.dto.response;

import com.homehn.backend.entity.ReviewMediaEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewMediaResponse {
    private Long id;
    private String type;
    private String url;

    public static ReviewMediaResponse from(ReviewMediaEntity media) {
        return ReviewMediaResponse.builder()
                .id(media.getId())
                .type(media.getMediaType().name())
                .url(media.getMediaUrl())
                .build();
    }
}
