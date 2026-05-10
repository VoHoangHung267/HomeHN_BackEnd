package com.homehn.backend.dto.response;

import com.homehn.backend.entity.ReportEntity;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {
    private Long          id;
    private Long          roomId;
    private String        roomTitle;
    private String        reporterName;
    private String        reporterEmail;
    private String        reason;
    private ReportEntity.Status status;
    private String        adminNote;
    private ReportEntity.LandlordResponseType landlordResponseType;
    private String        landlordResponseNote;
    private java.time.LocalDateTime landlordRespondedAt;
    private java.time.LocalDateTime createdAt;
}
