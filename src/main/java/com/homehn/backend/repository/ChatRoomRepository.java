package com.homehn.backend.repository;

import com.homehn.backend.entity.ChatRoomEntity;
import org.springframework.data.jpa.repository.*;

import java.util.List;
import java.util.Optional;

public interface ChatRoomRepository extends JpaRepository<ChatRoomEntity, Long> {
    Optional<ChatRoomEntity> findByRoom_IdAndSeeker_Id(Long roomId, Long seekerId);
    List<ChatRoomEntity> findBySeeker_Id(Long seekerId);
    List<ChatRoomEntity> findByLandlord_Id(Long landlordId);
    boolean existsByRoom_IdAndSeeker_Id(Long roomId, Long seekerId);
}
