package com.homehn.backend.repository;

import com.homehn.backend.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    List<NotificationEntity> findByUser_IdOrderByCreatedAtDesc(Long userId);
    long countByUser_IdAndIsRead(Long userId, boolean isRead);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.user.id = :userId")
    int markAllRead(@Param("userId") Long userId);

    @Transactional
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE NotificationEntity n SET n.isRead = true WHERE n.id = :id AND n.user.id = :userId")
    int markOneRead(@Param("id") Long id, @Param("userId") Long userId);
}
