package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.RoomRequest;
import com.homehn.backend.dto.response.RoomResponse;
import com.homehn.backend.entity.*;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.FavoriteRepository;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.RoomSpecification;
import com.homehn.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RoomService {

    private final RoomRepository roomRepo;
    private final FavoriteRepository favoriteRepo;
    private final UserRepository userRepo;
    private final CloudinaryService   cloudinaryService;

    // â”€â”€ Search â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Transactional(readOnly = true)
    public Page<RoomResponse> search(
            String keyword, String district,
            BigDecimal minPrice, BigDecimal maxPrice,
            BigDecimal minArea, BigDecimal maxArea,
            RoomEntity.RoomType roomType, Boolean isFurnished,
            RoomEntity.GenderRequirement gender,
            String sortBy,
            int page, int size, Long currentUserId
    ) {
        var spec     = RoomSpecification.filter(keyword, district, minPrice, maxPrice,
                minArea, maxArea, roomType, isFurnished, gender);
        var pageable = PageRequest.of(page, size, resolveSort(sortBy));

        // Lấy danh sách id trư�›c
        Page<RoomEntity> roomPage = roomRepo.findAll(spec, pageable);
        List<Long> ids = roomPage.getContent().stream().map(RoomEntity::getId).toList();
        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < ids.size(); i++) {
            orderMap.put(ids.get(i), i);
        }

        // Fetch images + landlord cho tất cả rooms
        List<RoomEntity> roomsWithImages = roomRepo.findAllByIdWithImages(ids);
        roomsWithImages.sort(Comparator.comparingInt(room -> orderMap.getOrDefault(room.getId(), Integer.MAX_VALUE)));
        // Fetch amenities cho tất cả rooms
        List<RoomEntity> roomsWithAmenities = roomRepo.findAllByIdWithAmenities(ids);

        // G�™p amenities vào
        Map<Long, RoomEntity> amenityMap = roomsWithAmenities.stream()
                .collect(Collectors.toMap(RoomEntity::getId, r -> r));

        List<RoomResponse> responses = roomsWithImages.stream().map(r -> {
            var resp = RoomResponse.from(r);

            // Set amenities từ amenityMap thẳng vào response
            RoomEntity withAmenities = amenityMap.get(r.getId());
            if (withAmenities != null) {
                resp.setAmenities(withAmenities.getAmenities().stream()
                        .map(RoomAmenityEntity::getAmenityName)
                        .toList());
            }

            if (currentUserId != null)
                resp.setFavorited(favoriteRepo.existsByUser_IdAndRoom_Id(currentUserId, r.getId()));
            return resp;
        }).toList();



        return new PageImpl<>(responses, pageable, roomPage.getTotalElements());
    }

    // â”€â”€ Get detail â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Transactional
    public RoomResponse getById(Long id, Long currentUserId) {
        var room = roomRepo.findByIdWithImages(id)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));

        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            if (currentUserId == null) {
                throw new AppException("Phòng không t�“n tại", 404);
            }
            UserEntity currentUser = userRepo.findById(currentUserId).orElseThrow();
            boolean canView = currentUser.getRole() == UserEntity.Role.ADMIN
                    || room.getLandlord().getId().equals(currentUserId);
            if (!canView) {
                throw new AppException("Phòng không t�“n tại", 404);
            }
        }

        // Fetch amenities riêng, KH�”NG addAll vào entity
        List<String> amenityNames = roomRepo.findByIdWithAmenities(id)
                .map(r -> r.getAmenities().stream()
                        .map(RoomAmenityEntity::getAmenityName)
                        .toList())
                .orElse(List.of());

        roomRepo.incrementViewCount(id);

        var resp = RoomResponse.from(room);
        resp.setAmenities(amenityNames); // set thẳng vào response

        if (currentUserId != null)
            resp.setFavorited(favoriteRepo.existsByUser_IdAndRoom_Id(currentUserId, id));
        return resp;
    }

    // â”€â”€ Create â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public RoomResponse create(RoomRequest req, Long landlordId) {
        UserEntity landlord = userRepo.findById(landlordId).orElseThrow();
        var room = RoomEntity.builder()
                .landlord(landlord)
                .title(req.getTitle()).description(req.getDescription())
                .price(req.getPrice()).area(req.getArea())
                .electricPrice(req.getElectricPrice()).waterPrice(req.getWaterPrice()).otherFees(req.getOtherFees())
                .address(req.getAddress()).ward(req.getWard()).district(req.getDistrict()).city(req.getCity())
                .latitude(req.getLatitude()).longitude(req.getLongitude())
                .roomType(req.getRoomType()).isFurnished(req.isFurnished())
                .maxPeople(req.getMaxPeople()).genderRequirement(req.getGenderRequirement())
                .status(RoomEntity.RoomStatus.PENDING)
                .build();

        if (req.getAmenities() != null) {
            req.getAmenities().forEach(a ->
                    room.getAmenities().add(RoomAmenityEntity.builder().room(room).amenityName(a).build()));
        }
        return RoomResponse.from(roomRepo.save(room));
    }

    public RoomResponse updateStatus(Long id, RoomEntity.RoomStatus status, Long actorId) {
        UserEntity actor = userRepo.findById(actorId).orElseThrow();
        var room = findOwnedRoom(id, actor);

        if (actor.getRole() == UserEntity.Role.ADMIN) {
            room.setStatus(status);
            return RoomResponse.from(roomRepo.save(room));
        }

        RoomEntity.RoomStatus currentStatus = room.getStatus();
        boolean allowed =
                (currentStatus == RoomEntity.RoomStatus.ACTIVE && status == RoomEntity.RoomStatus.HIDDEN)
                        || (currentStatus == RoomEntity.RoomStatus.HIDDEN && status == RoomEntity.RoomStatus.ACTIVE)
                        || ((currentStatus == RoomEntity.RoomStatus.ACTIVE || currentStatus == RoomEntity.RoomStatus.HIDDEN)
                        && status == RoomEntity.RoomStatus.RENTED)
                        || (currentStatus == RoomEntity.RoomStatus.RENTED && status == RoomEntity.RoomStatus.ACTIVE);

        if (!allowed) {
            throw new AppException("Kh�ƒ´ng thá»�’ chuyá»�’n tráº¡ng th�ƒ¡i ph�ƒ²ng tá»« " + currentStatus + " sang " + status);
        }

        room.setStatus(status);
        return RoomResponse.from(roomRepo.save(room));
    }

    // â”€â”€ Update â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public RoomResponse update(Long id, RoomRequest req, Long actorId) {
        UserEntity actor = userRepo.findById(actorId).orElseThrow();
        var room = findOwnedRoom(id, actor);
        room.setTitle(req.getTitle());          room.setDescription(req.getDescription());
        room.setPrice(req.getPrice());          room.setArea(req.getArea());
        room.setElectricPrice(req.getElectricPrice()); room.setWaterPrice(req.getWaterPrice());
        room.setOtherFees(req.getOtherFees());
        room.setAddress(req.getAddress());      room.setWard(req.getWard());
        room.setDistrict(req.getDistrict());    room.setCity(req.getCity());
        room.setLatitude(req.getLatitude());    room.setLongitude(req.getLongitude());
        room.setRoomType(req.getRoomType());    room.setFurnished(req.isFurnished());
        room.setMaxPeople(req.getMaxPeople());  room.setGenderRequirement(req.getGenderRequirement());

        room.getAmenities().clear();
        if (req.getAmenities() != null)
            req.getAmenities().forEach(a ->
                    room.getAmenities().add(RoomAmenityEntity.builder().room(room).amenityName(a).build()));

        return RoomResponse.from(roomRepo.save(room));
    }

    // â”€â”€ Delete â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void delete(Long id, Long actorId) {
        UserEntity actor = userRepo.findById(actorId).orElseThrow();
        roomRepo.delete(findOwnedRoom(id, actor));
    }

    // â”€â”€ Upload images â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public void uploadImages(Long roomId, List<MultipartFile> files, Long actorId) {
        UserEntity actor = userRepo.findById(actorId).orElseThrow();
        var room = findOwnedRoom(roomId, actor);
        boolean hasImages = !room.getImages().isEmpty();

        for (int i = 0; i < files.size(); i++) {
            var uploaded = cloudinaryService.upload(files.get(i), "phongtro/rooms");
            var img = RoomImageEntity.builder()
                    .room(room)
                    .imageUrl((String) uploaded.get("url"))
                    .publicId((String) uploaded.get("public_id"))
                    .isPrimary(!hasImages && i == 0)
                    .sortOrder(room.getImages().size() + i)
                    .build();
            room.getImages().add(img);
        }
        roomRepo.save(room);
    }

    // â”€â”€ My rooms â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    @Transactional(readOnly = true)
    public List<RoomResponse> getMyRooms(Long landlordId) {
        return roomRepo.findByLandlordIdOrderByCreatedAtDesc(landlordId)
                .stream().map(RoomResponse::from).toList();
    }



    // â”€â”€ Toggle favorite â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    public boolean toggleFavorite(Long roomId, Long userId) {
        if (favoriteRepo.existsByUser_IdAndRoom_Id(userId, roomId)) {
            favoriteRepo.deleteByUserIdAndRoomId(userId, roomId);
            return false;
        }
        var room = roomRepo.findById(roomId)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));
        if (room.getStatus() != RoomEntity.RoomStatus.ACTIVE) {
            throw new AppException("Ch�‰ có th�ƒ yêu thích phòng �‘ang hi�ƒn th�‹");
        }
        var user = userRepo.findById(userId).orElseThrow();
        favoriteRepo.save(FavoriteEntity.builder().user(user).room(room).build());
        return true;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> getFavorites(Long userId) {
        List<FavoriteEntity> favorites = favoriteRepo.findByUserIdWithImages(userId);

        // Map amenities vào room
        Map<Long, List<RoomAmenityEntity>> amenityMap = favoriteRepo.findByUserIdWithAmenities(userId)
                .stream()
                .collect(Collectors.toMap(
                        f -> f.getRoom().getId(),
                        f -> f.getRoom().getAmenities()
                ));

        return favorites.stream().map(f -> {
            var room = f.getRoom();
            var resp = RoomResponse.from(room);

            // Set amenities thẳng vào response, không addAll vào entity
            var amenities = amenityMap.get(room.getId());
            if (amenities != null) {
                resp.setAmenities(amenities.stream()
                        .map(RoomAmenityEntity::getAmenityName)
                        .toList());
            }

            resp.setFavorited(true);
            return resp;
        }).toList();
    }

    // â”€â”€ Helper â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
    private Sort resolveSort(String sortBy) {
        if (sortBy == null || sortBy.isBlank() || "createdAt".equals(sortBy)) {
            return Sort.by(Sort.Direction.DESC, "createdAt");
        }
        return switch (sortBy) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "viewCount" -> Sort.by(Sort.Direction.DESC, "viewCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private RoomEntity findOwnedRoom(Long id, UserEntity actor) {
        var room = roomRepo.findById(id)
                .orElseThrow(() -> new AppException("Phòng không t�“n tại", 404));
        if (!room.getLandlord().getId().equals(actor.getId())
                && actor.getRole() != UserEntity.Role.ADMIN)
            throw new AccessDeniedException("Bạn không có quyền thao tác trên phòng này");
        return room;
    }
}
