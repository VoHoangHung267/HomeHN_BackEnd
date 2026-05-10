package com.homehn.backend.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewResponse {
    private Long id;
    private Long reviewerId;
    private String reviewerName, reviewerAvatar;
    private int rating;
    private Integer ratingLocation, ratingPrice, ratingLandlord, ratingHygiene;
    private String comment;
    private List<ReviewMediaResponse> media;
    private LocalDateTime createdAt, updatedAt;
}
