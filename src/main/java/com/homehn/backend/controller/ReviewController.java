package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ReviewRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ReviewMediaResponse;
import com.homehn.backend.dto.response.ReviewResponse;
import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.ReviewEntity;
import com.homehn.backend.entity.ReviewMediaEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ChatRoomRepository;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.ReviewRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.CloudinaryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private static final int MAX_MEDIA_FILES = 6;

    private final ReviewRepository reviewRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;
    private final ChatRoomRepository chatRoomRepo;
    private final CloudinaryService cloudinaryService;

    @GetMapping("/room/{roomId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getRoomReviews(@PathVariable Long roomId) {
        List<ReviewResponse> list = reviewRepo.findByRoom_IdOrderByCreatedAtDesc(roomId)
                .stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping(value = "/room/{roomId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @PathVariable Long roomId,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity reviewer = userRepo.findById(principal.getId()).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));

        if (room.getLandlord().getId().equals(reviewer.getId())) {
            throw new AppException("Bạn không th�ƒ �‘ánh giá phòng của chính mình");
        }
        if (!chatRoomRepo.existsByRoom_IdAndSeeker_Id(roomId, reviewer.getId())) {
            throw new AppException("Bạn cần liên h�‡ v�›i chủ nhà trư�›c khi �‘ánh giá");
        }
        if (reviewRepo.existsByReviewer_IdAndRoom_Id(reviewer.getId(), roomId)) {
            throw new AppException("Bạn �‘ã �‘ánh giá phòng này r�“i. Hãy ch�‰nh sửa �‘ánh giá cũ.");
        }

        validateMediaFiles(files);

        ReviewEntity review = ReviewEntity.builder()
                .room(room)
                .reviewer(reviewer)
                .rating(req.getRating())
                .ratingLocation(req.getRatingLocation())
                .ratingPrice(req.getRatingPrice())
                .ratingLandlord(req.getRatingLandlord())
                .ratingHygiene(req.getRatingHygiene())
                .comment(req.getComment())
                .build();

        attachMedia(review, files);
        review = reviewRepo.save(review);

        updateRoomRating(room);

        notifRepo.save(NotificationEntity.builder()
                .user(room.getLandlord())
                .type("REVIEW_RECEIVED")
                .title("Phòng của bạn có �‘ánh giá m�›i")
                .message(reviewer.getFullName() + " �‘ánh giá " + req.getRating()
                        + "⭐ cho phòng \"" + room.getTitle() + "\"")
                .relatedId(roomId)
                .build());

        return ResponseEntity.ok(ApiResponse.ok("Đánh giá thành công", toResponse(review)));
    }

    @PutMapping(value = "/{reviewId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
            @PathVariable Long reviewId,
            @Valid @ModelAttribute ReviewRequest req,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity reviewer = userRepo.findById(principal.getId()).orElseThrow();
        ReviewEntity review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new AppException("Không tìm thấy �‘ánh giá", 404));

        if (!review.getReviewer().getId().equals(reviewer.getId())) {
            throw new AppException("Bạn không có quyền sửa �‘ánh giá này", 403);
        }

        validateMediaFiles(files);

        review.setRating(req.getRating());
        review.setRatingLocation(req.getRatingLocation());
        review.setRatingPrice(req.getRatingPrice());
        review.setRatingLandlord(req.getRatingLandlord());
        review.setRatingHygiene(req.getRatingHygiene());
        review.setComment(req.getComment());

        replaceMedia(review, files);
        review = reviewRepo.save(review);

        updateRoomRating(review.getRoom());

        return ResponseEntity.ok(ApiResponse.ok("Cập nhật �‘ánh giá thành công", toResponse(review)));
    }

    @DeleteMapping("/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity user = userRepo.findById(principal.getId()).orElseThrow();
        ReviewEntity review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new AppException("Không tìm thấy �‘ánh giá", 404));

        boolean isOwner = review.getReviewer().getId().equals(user.getId());
        boolean isAdmin = user.getRole() == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xoá �‘ánh giá này", 403);
        }

        deleteUploadedMedia(review.getMediaFiles());

        RoomEntity room = review.getRoom();
        reviewRepo.delete(review);
        updateRoomRating(room);

        return ResponseEntity.ok(ApiResponse.ok("Đã xoá �‘ánh giá", null));
    }

    @GetMapping("/room/{roomId}/my-review")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> getMyReview(
            @PathVariable Long roomId,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity reviewer = userRepo.findById(principal.getId()).orElseThrow();
        return reviewRepo.findByReviewer_IdAndRoom_Id(reviewer.getId(), roomId)
                .map(r -> ResponseEntity.ok(ApiResponse.ok(toResponse(r))))
                .orElse(ResponseEntity.ok(ApiResponse.ok(null)));
    }

    private void validateMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (files.size() > MAX_MEDIA_FILES) {
            throw new AppException("Ch�‰ �‘ược tải t�‘i �‘a " + MAX_MEDIA_FILES + " ảnh hoặc video");
        }

        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
                throw new AppException("Ch�‰ h�— trợ file ảnh hoặc video");
            }
        }
    }

    private void attachMedia(ReviewEntity review, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String resourceType = getResourceType(file);
            var uploaded = cloudinaryService.upload(file, "phongtro/reviews", resourceType);

            review.getMediaFiles().add(ReviewMediaEntity.builder()
                    .review(review)
                    .mediaType("video".equals(resourceType) ? ReviewMediaEntity.MediaType.VIDEO : ReviewMediaEntity.MediaType.IMAGE)
                    .mediaUrl((String) uploaded.get("secure_url"))
                    .publicId((String) uploaded.get("public_id"))
                    .sortOrder(i)
                    .build());
        }
    }

    private void replaceMedia(ReviewEntity review, List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        deleteUploadedMedia(review.getMediaFiles());
        review.getMediaFiles().clear();
        attachMedia(review, files);
    }

    private void deleteUploadedMedia(List<ReviewMediaEntity> mediaFiles) {
        if (mediaFiles == null) {
            return;
        }

        for (ReviewMediaEntity media : new ArrayList<>(mediaFiles)) {
            if (media.getPublicId() != null && !media.getPublicId().isBlank()) {
                cloudinaryService.delete(media.getPublicId(), media.getMediaType() == ReviewMediaEntity.MediaType.VIDEO ? "video" : "image");
            }
        }
    }

    private String getResourceType(MultipartFile file) {
        String contentType = file.getContentType();
        return contentType != null && contentType.startsWith("video/") ? "video" : "image";
    }

    private void updateRoomRating(RoomEntity room) {
        Double avg = reviewRepo.avgRatingByRoom(room.getId());
        long count = reviewRepo.countByRoom(room.getId());
        room.setAvgRating(avg != null ? Math.round(avg * 100.0) / 100.0 : 0.0);
        room.setReviewCount((int) count);
        roomRepo.save(room);
        updateLandlordRating(room.getLandlord());
    }

    private void updateLandlordRating(UserEntity landlord) {
        List<RoomEntity> rooms = roomRepo.findByLandlordIdOrderByCreatedAtDesc(landlord.getId());
        double total = 0;
        int count = 0;
        for (RoomEntity room : rooms) {
            if (room.getReviewCount() > 0) {
                total += room.getAvgRating() * room.getReviewCount();
                count += room.getReviewCount();
            }
        }
        landlord.setAvgRating(count > 0 ? Math.round(total / count * 100.0) / 100.0 : 0.0);
        landlord.setTotalReviews(count);
        userRepo.save(landlord);
    }

    private ReviewResponse toResponse(ReviewEntity review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .reviewerId(review.getReviewer().getId())
                .reviewerName(review.getReviewer().getFullName())
                .reviewerAvatar(review.getReviewer().getAvatarUrl())
                .rating(review.getRating())
                .ratingLocation(review.getRatingLocation())
                .ratingPrice(review.getRatingPrice())
                .ratingLandlord(review.getRatingLandlord())
                .ratingHygiene(review.getRatingHygiene())
                .comment(review.getComment())
                .media(review.getMediaFiles().stream().map(ReviewMediaResponse::from).toList())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
                .build();
    }
}
