package com.homehn.backend.repository;

import com.homehn.backend.entity.ContractAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractAdjustmentRepository extends JpaRepository<ContractAdjustmentEntity, Long> {
    List<ContractAdjustmentEntity> findByBooking_IdOrderByCreatedAtDesc(Long bookingId);
    Optional<ContractAdjustmentEntity> findFirstByBooking_IdAndStatusOrderByCreatedAtDesc(
            Long bookingId,
            ContractAdjustmentEntity.Status status
    );
}
