package com.homehn.backend.repository;

import com.homehn.backend.entity.MessageEntity;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface MessageRepository extends JpaRepository<MessageEntity, Long> {
    List<MessageEntity> findByChatRoom_IdOrderBySentAtAsc(Long chatRoomId);

    @Query("SELECT COUNT(m) FROM MessageEntity m WHERE m.chatRoom.id = :chatRoomId AND m.isRead = false AND m.sender.id != :userId")
    int countUnread(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);

    @Transactional
    @Modifying
    @Query("UPDATE MessageEntity m SET m.isRead = true WHERE m.chatRoom.id = :chatRoomId AND m.sender.id != :userId")
    void markAllAsRead(@Param("chatRoomId") Long chatRoomId, @Param("userId") Long userId);
}

