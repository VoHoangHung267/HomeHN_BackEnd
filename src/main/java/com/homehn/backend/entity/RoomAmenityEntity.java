package com.homehn.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity @Table(name = "room_amenities")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RoomAmenityEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "room_id") private RoomEntity room;
    @Column(name = "amenity_name") private String amenityName;
}
