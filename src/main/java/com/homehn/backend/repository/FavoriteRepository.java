package com.homehn.backend.repository;

import com.homehn.backend.entity.FavoriteEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<FavoriteEntity, Long> {
    boolean existsByUser_IdAndRoom_Id(Long userId, Long roomId);
    long countByUser_Id(Long userId);
    Optional<FavoriteEntity> findByUser_IdAndRoom_Id(Long userId, Long roomId);

    @Query("SELECT f FROM FavoriteEntity f LEFT JOIN FETCH f.room r LEFT JOIN FETCH r.images LEFT JOIN FETCH r.landlord WHERE f.user.id = :userId ORDER BY f.createdAt DESC")
    List<FavoriteEntity> findByUserIdWithImages(@Param("userId") Long userId);

    @Query("SELECT f FROM FavoriteEntity f LEFT JOIN FETCH f.room r LEFT JOIN FETCH r.amenities WHERE f.user.id = :userId")
    List<FavoriteEntity> findByUserIdWithAmenities(@Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("DELETE FROM FavoriteEntity f WHERE f.user.id = :userId AND f.room.id = :roomId")
    void deleteByUserIdAndRoomId(@Param("userId") Long userId, @Param("roomId") Long roomId);
}
