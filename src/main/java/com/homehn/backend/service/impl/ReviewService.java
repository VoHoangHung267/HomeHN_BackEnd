package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.ReviewRequest;
import com.homehn.backend.dto.response.ReviewMediaResponse;
import com.homehn.backend.dto.response.ReviewResponse;
import com.homehn.backend.entity.ReviewEntity;
import com.homehn.backend.entity.ReviewMediaEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.ChatRoomRepository;
import com.homehn.backend.repository.ReviewRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewService {

    private static final int MAX_MEDIA_FILES = 6;

    private final ReviewRepository reviewRepo;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final ChatRoomRepository chatRoomRepo;
    private final CloudinaryService cloudinaryService;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<ReviewResponse> getRoomReviews(Long roomId) {
        return reviewRepo.findByRoom_IdOrderByCreatedAtDesc(roomId).stream()
                .map(this::toResponse)
                .toList();
    }

    public ReviewResponse createReview(Long roomId, ReviewRequest req, List<MultipartFile> files, Long reviewerId) {
        UserEntity reviewer = userRepo.findById(reviewerId).orElseThrow();
        RoomEntity room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không tồn tại", 404));

        if (room.getLandlord().getId().equals(reviewer.getId())) {
            throw new AppException("Bạn không thể đánh giá phòng của chính mình");
        }
        if (!chatRoomRepo.existsByRoom_IdAndSeeker_Id(roomId, reviewer.getId())) {
            throw new AppException("Bạn cần liên hệ với chủ nhà trước khi đánh giá");
        }
        if (reviewRepo.existsByReviewer_IdAndRoom_Id(reviewer.getId(), roomId)) {
            throw new AppException("Bạn đã đánh giá phòng này rồi. Hãy chỉnh sửa đánh giá cũ.");
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

        notificationService.notifyUser(
                room.getLandlord(),
                "REVIEW_RECEIVED",
                "Phòng của bạn có đánh giá mới",
                reviewer.getFullName() + " đánh giá " + req.getRating() + "⭐ cho phòng \"" + room.getTitle() + "\"",
                roomId
        );

        return toResponse(review);
    }

    public ReviewResponse updateReview(Long reviewId, ReviewRequest req, List<MultipartFile> files, Long reviewerId) {
        UserEntity reviewer = userRepo.findById(reviewerId).orElseThrow();
        ReviewEntity review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new AppException("Không tìm thấy đánh giá", 404));

        if (!review.getReviewer().getId().equals(reviewer.getId())) {
            throw new AppException("Bạn không có quyền sửa đánh giá này", 403);
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
        return toResponse(review);
    }

    public void deleteReview(Long reviewId, Long userId, UserEntity.Role role) {
        ReviewEntity review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new AppException("Không tìm thấy đánh giá", 404));

        boolean isOwner = review.getReviewer().getId().equals(userId);
        boolean isAdmin = role == UserEntity.Role.ADMIN;
        if (!isOwner && !isAdmin) {
            throw new AppException("Bạn không có quyền xoá đánh giá này", 403);
        }

        deleteUploadedMedia(review.getMediaFiles());
        RoomEntity room = review.getRoom();
        reviewRepo.delete(review);
        updateRoomRating(room);
    }

    @Transactional(readOnly = true)
    public ReviewResponse getMyReview(Long roomId, Long reviewerId) {
        return reviewRepo.findByReviewer_IdAndRoom_Id(reviewerId, roomId)
                .map(this::toResponse)
                .orElse(null);
    }

    private void validateMediaFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return;
        }
        if (files.size() > MAX_MEDIA_FILES) {
            throw new AppException("Chỉ được tải tối đa " + MAX_MEDIA_FILES + " ảnh hoặc video");
        }
        for (MultipartFile file : files) {
            String contentType = file.getContentType();
            if (contentType == null || !(contentType.startsWith("image/") || contentType.startsWith("video/"))) {
                throw new AppException("Chỉ hỗ trợ file ảnh hoặc video");
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
                cloudinaryService.delete(
                        media.getPublicId(),
                        media.getMediaType() == ReviewMediaEntity.MediaType.VIDEO ? "video" : "image"
                );
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
