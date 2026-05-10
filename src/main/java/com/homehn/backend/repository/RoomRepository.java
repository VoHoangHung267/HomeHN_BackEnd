package com.homehn.backend.repository;

import com.homehn.backend.entity.RoomEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface RoomRepository extends JpaRepository<RoomEntity, Long>, JpaSpecificationExecutor<RoomEntity> {
    List<RoomEntity> findByLandlordIdOrderByCreatedAtDesc(Long landlordId);
    long countByLandlordId(Long landlordId);
    List<RoomEntity> findByStatusOrderByCreatedAtDesc(RoomEntity.RoomStatus status);
    long countByStatus(RoomEntity.RoomStatus status);

    // RoomRepository.java

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.images LEFT JOIN FETCH r.landlord WHERE r.id = :id")
    Optional<RoomEntity> findByIdWithImages(@Param("id") Long id);

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.amenities WHERE r.id = :id")
    Optional<RoomEntity> findByIdWithAmenities(@Param("id") Long id);

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.images LEFT JOIN FETCH r.landlord WHERE r.id IN :ids")
    List<RoomEntity> findAllByIdWithImages(@Param("ids") List<Long> ids);

    @Query("SELECT r FROM RoomEntity r LEFT JOIN FETCH r.amenities WHERE r.id IN :ids")
    List<RoomEntity> findAllByIdWithAmenities(@Param("ids") List<Long> ids);

    @Transactional
    @Modifying
    @Query("UPDATE RoomEntity r SET r.viewCount = r.viewCount + 1 WHERE r.id = :id")
    void incrementViewCount(@Param("id") Long id);
}
