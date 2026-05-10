package com.homehn.backend.dto.response;

import com.homehn.backend.entity.ViewingAppointmentEntity;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewingAppointmentResponse {
    private Long id;
    private Long roomId;
    private String roomTitle;
    private String roomPrimaryImage;
    private Long seekerId;
    private String seekerName;
    private String seekerPhone;
    private Long landlordId;
    private String landlordName;
    private LocalDateTime requestedAt;
    private String message;
    private String landlordNote;
    private ViewingAppointmentEntity.Status status;
    private LocalDateTime createdAt;

    public static ViewingAppointmentResponse from(ViewingAppointmentEntity appointment) {
        String primaryImage = appointment.getRoom().getImages().stream()
                .filter(img -> img.isPrimary())
                .map(img -> img.getImageUrl())
                .findFirst()
                .orElse(null);

        return ViewingAppointmentResponse.builder()
                .id(appointment.getId())
                .roomId(appointment.getRoom().getId())
                .roomTitle(appointment.getRoom().getTitle())
                .roomPrimaryImage(primaryImage)
                .seekerId(appointment.getSeeker().getId())
                .seekerName(appointment.getSeeker().getFullName())
                .seekerPhone(appointment.getSeeker().getPhone())
                .landlordId(appointment.getLandlord().getId())
                .landlordName(appointment.getLandlord().getFullName())
                .requestedAt(appointment.getRequestedAt())
                .message(appointment.getMessage())
                .landlordNote(appointment.getLandlordNote())
                .status(appointment.getStatus())
                .createdAt(appointment.getCreatedAt())
                .build();
    }
}
