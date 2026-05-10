package com.homehn.backend.repository;

import com.homehn.backend.entity.RoomImageEntity;
import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface RoomImageRepository extends JpaRepository<RoomImageEntity, Long> {
    List<RoomImageEntity> findByRoomIdOrderBySortOrderAsc(Long roomId);
}