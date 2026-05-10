package com.homehn.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewRequest {
    @Min(1) @Max(5) private int rating;
    @Min(1) @Max(5) private Integer ratingLocation; // nullable
    @Min(1) @Max(5) private Integer ratingPrice;
    @Min(1) @Max(5) private Integer ratingLandlord;
    @Min(1) @Max(5) private Integer ratingHygiene;
    @Size(max = 200) private String comment;
}
