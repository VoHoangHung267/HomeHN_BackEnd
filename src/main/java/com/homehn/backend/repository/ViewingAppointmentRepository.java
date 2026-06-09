package com.homehn.backend.repository;

import com.homehn.backend.entity.ViewingAppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ViewingAppointmentRepository extends JpaRepository<ViewingAppointmentEntity, Long> {
    List<ViewingAppointmentEntity> findBySeeker_IdOrderByRequestedAtDesc(Long seekerId);
    List<ViewingAppointmentEntity> findByLandlord_IdOrderByRequestedAtDesc(Long landlordId);
    long countBySeeker_Id(Long seekerId);
    long countByLandlord_Id(Long landlordId);
    boolean existsByRoom_IdAndSeeker_IdAndStatusIn(
            Long roomId,
            Long seekerId,
            List<ViewingAppointmentEntity.Status> statuses
    );

    boolean existsByRoom_IdAndRequestedAtAndStatusIn(
            Long roomId,
            LocalDateTime requestedAt,
            List<ViewingAppointmentEntity.Status> statuses
    );

    boolean existsByRoom_IdAndRequestedAtAndStatusInAndIdNot(
            Long roomId,
            LocalDateTime requestedAt,
            List<ViewingAppointmentEntity.Status> statuses,
            Long id
    );
}
