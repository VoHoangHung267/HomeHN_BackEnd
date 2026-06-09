package com.homehn.backend.service.impl;

import com.homehn.backend.dto.request.CreateViewingAppointmentRequest;
import com.homehn.backend.dto.request.UpdateViewingAppointmentRequest;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.entity.UserEntity;
import com.homehn.backend.entity.ViewingAppointmentEntity;
import com.homehn.backend.exception.AppException;
import com.homehn.backend.repository.RoomRepository;
import com.homehn.backend.repository.UserRepository;
import com.homehn.backend.repository.ViewingAppointmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewingAppointmentServiceTest {

    @Mock
    private ViewingAppointmentRepository appointmentRepo;

    @Mock
    private RoomRepository roomRepo;

    @Mock
    private UserRepository userRepo;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ViewingAppointmentService service;

    private UserEntity seeker;
    private UserEntity landlord;
    private RoomEntity room;

    @BeforeEach
    void setUp() {
        landlord = UserEntity.builder()
                .id(10L)
                .fullName("Chu nha")
                .role(UserEntity.Role.LANDLORD)
                .build();

        seeker = UserEntity.builder()
                .id(20L)
                .fullName("Nguoi thue")
                .role(UserEntity.Role.SEEKER)
                .build();

        room = RoomEntity.builder()
                .id(30L)
                .title("Phong test")
                .status(RoomEntity.RoomStatus.ACTIVE)
                .landlord(landlord)
                .build();
    }

    @Test
    void createShouldRejectWhenAcceptedAppointmentAlreadyExistsAtSameTime() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 10, 14, 0);
        CreateViewingAppointmentRequest req = new CreateViewingAppointmentRequest();
        req.setRequestedAt(requestedAt);
        req.setMessage("Em muon xem phong");

        when(userRepo.findById(seeker.getId())).thenReturn(Optional.of(seeker));
        when(roomRepo.findById(room.getId())).thenReturn(Optional.of(room));
        when(appointmentRepo.existsByRoom_IdAndSeeker_IdAndStatusIn(
                eq(room.getId()),
                eq(seeker.getId()),
                any(List.class)
        )).thenReturn(false);
        when(appointmentRepo.existsByRoom_IdAndRequestedAtAndStatusIn(
                room.getId(),
                requestedAt,
                List.of(ViewingAppointmentEntity.Status.ACCEPTED)
        )).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () ->
                service.create(room.getId(), req, seeker.getId())
        );

        assertEquals("Khung giờ này đã có lịch xem phòng được xác nhận. Vui lòng chọn giờ khác.", exception.getMessage());
        verify(appointmentRepo, never()).save(any(ViewingAppointmentEntity.class));
    }

    @Test
    void updateStatusShouldRejectAcceptWhenAnotherAcceptedAppointmentAlreadyExists() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 10, 14, 0);
        ViewingAppointmentEntity appointment = ViewingAppointmentEntity.builder()
                .id(99L)
                .room(room)
                .landlord(landlord)
                .seeker(seeker)
                .requestedAt(requestedAt)
                .status(ViewingAppointmentEntity.Status.PENDING)
                .build();

        UpdateViewingAppointmentRequest req = new UpdateViewingAppointmentRequest();
        req.setStatus(ViewingAppointmentEntity.Status.ACCEPTED);
        req.setNote("Ok");

        when(appointmentRepo.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepo.existsByRoom_IdAndRequestedAtAndStatusInAndIdNot(
                room.getId(),
                requestedAt,
                List.of(ViewingAppointmentEntity.Status.ACCEPTED),
                appointment.getId()
        )).thenReturn(true);

        AppException exception = assertThrows(AppException.class, () ->
                service.updateStatus(appointment.getId(), req, landlord.getId(), UserEntity.Role.LANDLORD)
        );

        assertEquals("Khung giờ này đã có lịch xem phòng được xác nhận. Vui lòng chọn giờ khác.", exception.getMessage());
        verify(appointmentRepo, never()).save(any(ViewingAppointmentEntity.class));
    }

    @Test
    void updateStatusShouldAllowFirstPendingAppointmentToBeAccepted() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 10, 14, 0);
        ViewingAppointmentEntity appointment = ViewingAppointmentEntity.builder()
                .id(100L)
                .room(room)
                .landlord(landlord)
                .seeker(seeker)
                .requestedAt(requestedAt)
                .status(ViewingAppointmentEntity.Status.PENDING)
                .build();

        UpdateViewingAppointmentRequest req = new UpdateViewingAppointmentRequest();
        req.setStatus(ViewingAppointmentEntity.Status.ACCEPTED);
        req.setNote("Moi ban den xem");

        when(appointmentRepo.findById(appointment.getId())).thenReturn(Optional.of(appointment));
        when(appointmentRepo.existsByRoom_IdAndRequestedAtAndStatusInAndIdNot(
                room.getId(),
                requestedAt,
                List.of(ViewingAppointmentEntity.Status.ACCEPTED),
                appointment.getId()
        )).thenReturn(false);
        when(appointmentRepo.save(any(ViewingAppointmentEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ViewingAppointmentEntity.Status resultStatus = service
                .updateStatus(appointment.getId(), req, landlord.getId(), UserEntity.Role.LANDLORD)
                .getStatus();

        assertEquals(ViewingAppointmentEntity.Status.ACCEPTED, resultStatus);
        verify(appointmentRepo).save(appointment);
    }
}
