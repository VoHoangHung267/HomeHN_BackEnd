package com.homehn.backend.controller;

import com.homehn.backend.dto.request.ReportRequest;
import com.homehn.backend.dto.request.RoomRequest;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.entity.NotificationEntity;
import com.homehn.backend.entity.ReportEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.NotificationRepository;
import com.homehn.backend.repository.ReportRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
class RoomController {

    private final ReportRepository reportRepo;
    private final RoomService roomService;
    private final RoomRepository roomRepo;
    private final UserRepository userRepo;
    private final NotificationRepository notifRepo;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<RoomResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) BigDecimal minArea,
            @RequestParam(required = false) BigDecimal maxArea,
            @RequestParam(required = false) RoomEntity.RoomType roomType,
            @RequestParam(required = false) Boolean isFurnished,
            @RequestParam(required = false) RoomEntity.GenderRequirement genderRequirement,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long uid = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(
                roomService.search(keyword, district, minPrice, maxPrice,
                        minArea, maxArea, roomType, isFurnished, genderRequirement, sortBy, page, size, uid)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user
    ) {
        Long uid = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(roomService.getById(id, uid)));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> create(
            @Valid @RequestBody RoomRequest req, @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Đ�ƒng phòng thành công, chờ duy�‡t",
                roomService.create(req, user.getId())));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> update(
            @PathVariable Long id, @Valid @RequestBody RoomRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thành công",
                roomService.update(id, req, user.getId())));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam RoomEntity.RoomStatus status,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok("Cáº­p nháº­t tráº¡ng th�ƒ¡i ph�ƒ²ng th�ƒ nh c�ƒÂ´ng",
                roomService.updateStatus(id, status, user.getId())));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user
    ) {
        roomService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Xoá thành công", null));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> uploadImages(
            @PathVariable Long id,
            @RequestParam("files") List<MultipartFile> files,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        roomService.uploadImages(id, files, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Upload ảnh thành công", null));
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(
            @PathVariable Long id, @AuthenticationPrincipal UserPrincipal user
    ) {
        boolean fav = roomService.toggleFavorite(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", fav)));
    }

    @GetMapping("/my-rooms")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> myRooms(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getMyRooms(user.getId())));
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> favorites(
            @AuthenticationPrincipal UserPrincipal user) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getFavorites(user.getId())));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> reportRoom(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        UserEntity user = userRepo.findById(principal.getId())
                .orElseThrow(() -> new AppException("Không tìm thấy user", 404));

        RoomEntity room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            throw new AppException("Ch�‰ có th�ƒ báo cáo phòng �‘ang hi�ƒn th�‹");
        }

        if (room.getLandlord().getId().equals(user.getId())) {
            throw new AppException("Bạn không th�ƒ báo cáo phòng của chính mình");
        }

        if (reportRepo.existsByReporter_IdAndRoom_Id(user.getId(), id)) {
            throw new AppException("Bạn �‘ã báo cáo phòng này r�“i");
        }

        ReportEntity report = reportRepo.save(ReportEntity.builder()
                .reporter(user)
                .room(room)
                .reason(req.getReason())
                .status(ReportEntity.Status.PENDING)
                .build());

        notifRepo.save(NotificationEntity.builder()
                .user(room.getLandlord())
                .type("REPORT_RECEIVED")
                .title("Phòng của bạn có báo cáo m�›i")
                .message(user.getFullName() + " �‘ã báo cáo phòng \"" + room.getTitle() + "\"")
                .relatedId(report.getId())
                .build());

        userRepo.findByRole(UserEntity.Role.ADMIN).stream()
                .filter(admin -> !admin.getId().equals(room.getLandlord().getId()))
                .forEach(admin -> notifRepo.save(NotificationEntity.builder()
                                .user(admin)
                                .type("ADMIN_REPORT_RECEIVED")
                                .title("Có báo cáo phòng m�›i")
                                .message(user.getFullName() + " �‘ã báo cáo phòng \"" + room.getTitle() + "\"")
                                .relatedId(report.getId())
                                .build()));

        return ResponseEntity.ok(ApiResponse.ok("Đã gửi báo cáo", null));
    }
}
