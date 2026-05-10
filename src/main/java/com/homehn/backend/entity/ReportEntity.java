package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity @Table(name = "reports")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ReportEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "reporter_id") private UserEntity reporter;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id")     private RoomEntity room;
    private String reason;
    @Enumerated(EnumType.STRING) @Builder.Default private Status status = Status.PENDING;
    @Column(name = "admin_note", columnDefinition = "TEXT") private String adminNote;
    @Enumerated(EnumType.STRING) @Column(name = "landlord_response_type")
    private LandlordResponseType landlordResponseType;
    @Column(name = "landlord_response_note", columnDefinition = "TEXT")
    private String landlordResponseNote;
    @Column(name = "landlord_responded_at")
    private LocalDateTime landlordRespondedAt;
    @Column(name = "created_at") private LocalDateTime createdAt;
    @Column(name = "updated_at") private LocalDateTime updatedAt;
    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }
    public enum Status { PENDING, REVIEWED, RESOLVED, DISMISSED }
    public enum LandlordResponseType { WILL_FIX, CONTEST }
}
