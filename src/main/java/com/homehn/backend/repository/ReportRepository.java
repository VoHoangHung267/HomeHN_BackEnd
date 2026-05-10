package com.homehn.backend.repository;

import com.homehn.backend.entity.ReportEntity;
import org.springframework.data.jpa.repository.*;

import java.util.List;

public interface ReportRepository extends JpaRepository<ReportEntity, Long> {
    List<ReportEntity> findByStatusOrderByCreatedAtDesc(ReportEntity.Status status);
    // Repositories.java - trong ReportRepository
    boolean existsByReporterIdAndRoomId(Long reporterId, Long roomId);

    // Đổi sang nested property đúng chuẩn:
    boolean existsByReporter_IdAndRoom_Id(Long reporterId, Long roomId);
}
