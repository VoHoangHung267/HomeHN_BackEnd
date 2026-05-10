package com.homehn.backend.repository;

import com.homehn.backend.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {
    List<ReviewEntity> findByRoom_IdOrderByCreatedAtDesc(Long roomId);
    boolean existsByReviewer_IdAndRoom_Id(Long reviewerId, Long roomId);
    Optional<ReviewEntity> findByReviewer_IdAndRoom_Id(Long reviewerId, Long roomId);

    @Query("SELECT AVG(r.rating) FROM ReviewEntity r WHERE r.room.id = :roomId")
    Double avgRatingByRoom(@Param("roomId") Long roomId);

    @Query("SELECT COUNT(r) FROM ReviewEntity r WHERE r.room.id = :roomId")
    long countByRoom(@Param("roomId") Long roomId);
}
