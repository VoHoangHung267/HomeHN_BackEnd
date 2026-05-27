package com.homehn.backend.repository;

import com.homehn.backend.entity.RentalBookingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RentalBookingRepository extends JpaRepository<RentalBookingEntity, Long> {
    List<RentalBookingEntity> findAllByOrderByCreatedAtDesc();
    List<RentalBookingEntity> findBySeeker_IdOrderByCreatedAtDesc(Long seekerId);
    List<RentalBookingEntity> findByLandlord_IdOrderByCreatedAtDesc(Long landlordId);
    long countBySeeker_Id(Long seekerId);
    long countByLandlord_Id(Long landlordId);
    Optional<RentalBookingEntity> findByPaymentOrderId(String paymentOrderId);
    List<RentalBookingEntity> findByRoom_IdAndStatusIn(Long roomId, Collection<RentalBookingEntity.Status> statuses);
    boolean existsByRoom_IdAndStatusIn(Long roomId, Collection<RentalBookingEntity.Status> statuses);
    boolean existsByRoom_IdAndSeeker_IdAndStatusIn(
            Long roomId,
            Long seekerId,
            Collection<RentalBookingEntity.Status> statuses
    );
}
