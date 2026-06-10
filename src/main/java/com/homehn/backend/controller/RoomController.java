package com.homehn.backend.controller;

import com.homehn.backend.dto.request.AiSearchRequest;
import com.homehn.backend.dto.request.ExtractRoomFormRequest;
import com.homehn.backend.dto.request.GenerateRoomDescriptionRequest;
import com.homehn.backend.dto.request.ReportRequest;
import com.homehn.backend.dto.request.RoomRequest;
import com.homehn.backend.dto.response.AiSearchResponse;
import com.homehn.backend.dto.response.ApiResponse;
import com.homehn.backend.dto.response.ExtractRoomFormResponse;
import com.homehn.backend.dto.response.GenerateRoomDescriptionResponse;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.security.UserPrincipal;
import com.homehn.backend.service.impl.GeminiRoomCopyService;
import com.homehn.backend.service.impl.RoomService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/rooms")
@RequiredArgsConstructor
class RoomController {

    private final RoomService roomService;
    private final GeminiRoomCopyService geminiRoomCopyService;

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
                roomService.search(
                        keyword,
                        district,
                        minPrice,
                        maxPrice,
                        minArea,
                        maxArea,
                        roomType,
                        isFurnished,
                        genderRequirement,
                        sortBy,
                        page,
                        size,
                        uid
                )));
    }

    @PostMapping("/ai/parse-search")
    public ResponseEntity<ApiResponse<AiSearchResponse>> parseSearch(@RequestBody AiSearchRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã phân tích nhu cầu tìm phòng",
                geminiRoomCopyService.parseSearch(req)
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RoomResponse>> getById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long uid = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(roomService.getById(id, uid)));
    }

    @GetMapping("/{id}/recommendations")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> recommendations(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        Long uid = user != null ? user.getId() : null;
        return ResponseEntity.ok(ApiResponse.ok(roomService.getRecommendations(id, uid)));
    }

    @PostMapping("/ai/generate-description")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<GenerateRoomDescriptionResponse>> generateDescription(
            @RequestBody GenerateRoomDescriptionRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã tạo gợi ý nội dung bằng AI",
                geminiRoomCopyService.generateDescription(req)
        ));
    }

    @PostMapping("/ai/extract-room-form")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<ExtractRoomFormResponse>> extractRoomForm(
            @RequestBody ExtractRoomFormRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đã phân tích mô tả và điền gợi ý vào form",
                geminiRoomCopyService.extractRoomForm(req)
        ));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> create(
            @Valid @RequestBody RoomRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Đăng phòng thành công, chờ duyệt",
                roomService.create(req, user.getId())
        ));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RoomRequest req,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật thành công",
                roomService.update(id, req, user.getId())
        ));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<RoomResponse>> updateStatus(
            @PathVariable Long id,
            @RequestParam RoomEntity.RoomStatus status,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(
                "Cập nhật trạng thái phòng thành công",
                roomService.updateStatus(id, status, user.getId())
        ));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        roomService.delete(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Xoá thành công", null));
    }

    @PostMapping("/{id}/images")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<Void>> uploadImages(
            @PathVariable Long id,
            @RequestParam(value = "files", required = false) List<MultipartFile> files,
            @RequestParam(value = "retainedImageUrls", required = false) List<String> retainedImageUrls,
            @RequestParam(value = "syncExistingImages", defaultValue = "false") boolean syncExistingImages,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        roomService.uploadImages(id, files, retainedImageUrls, syncExistingImages, user.getId());
        return ResponseEntity.ok(ApiResponse.ok("Upload ảnh thành công", null));
    }

    @PostMapping("/{id}/favorite")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> toggleFavorite(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        boolean fav = roomService.toggleFavorite(id, user.getId());
        return ResponseEntity.ok(ApiResponse.ok(Map.of("favorited", fav)));
    }

    @GetMapping("/my-rooms")
    @PreAuthorize("hasAnyRole('LANDLORD','ADMIN')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> myRooms(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getMyRooms(user.getId())));
    }

    @GetMapping("/favorites")
    @PreAuthorize("hasRole('SEEKER')")
    public ResponseEntity<ApiResponse<List<RoomResponse>>> favorites(
            @AuthenticationPrincipal UserPrincipal user
    ) {
        return ResponseEntity.ok(ApiResponse.ok(roomService.getFavorites(user.getId())));
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> reportRoom(
            @PathVariable Long id,
            @Valid @RequestBody ReportRequest req,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        roomService.reportRoom(id, req.getReason(), principal.getId());
        return ResponseEntity.ok(ApiResponse.ok("Đã gửi báo cáo", null));
    }
}
