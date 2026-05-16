package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ReviewRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ReviewResponse;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRoomReviews(@PathVariable Long roomId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getRoomReviews(roomId)));
    }

    @PostMapping(value = "/room/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long roomId,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đánh giá thành công",
                reviewService.createReview(roomId, req, files, principal.getId())
        ));
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật đánh giá thành công",
                reviewService.updateReview(reviewId, req, files, principal.getId())
        ));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reviewService.deleteReview(reviewId, principal.getId(), principal.getRole());
        return ResponseEntity.ok(ApiResponse.ok("Đã xoá đánh giá", null));
    }

    @GetMapping("/room/{roomId}/my-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getMyReview(roomId, principal.getId())));
    }
}
