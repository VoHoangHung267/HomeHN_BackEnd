package com.homehn.backend.service.impl;

import com.homehn.backend.entity.RentalBookingEntity;
import com.homehn.backend.entity.RoomEntity;
import com.homehn.backend.repository.RentalBookingRepository;
import com.homehn.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ContractLifecycleService {
    private static final ZoneId APP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final Set<RentalBookingEntity.Status> ACTIVE_CONTRACT_STATUSES = Set.of(
            RentalBookingEntity.Status.ACTIVE,
            RentalBookingEntity.Status.EXPIRING_SOON,
            RentalBookingEntity.Status.RENEWAL_PENDING
    );

    private final RentalBookingRepository bookingRepo;
    private final RoomRepository roomRepo;
    private final NotificationService notificationService;

    @Transactional
    @Scheduled(cron = "0 5 0 * * *", zone = "Asia/Ho_Chi_Minh")
    public void runDailyLifecycleSync() {
        syncNow();
    }

    @Transactional
    public void syncNow() {
        LocalDate today = LocalDate.now(APP_ZONE);
        List<RentalBookingEntity> bookings = bookingRepo.findByStatusIn(List.of(
                RentalBookingEntity.Status.DEPOSIT_PAID,
                RentalBookingEntity.Status.ACTIVE,
                RentalBookingEntity.Status.EXPIRING_SOON,
                RentalBookingEntity.Status.RENEWAL_PENDING
        ));

        for (RentalBookingEntity booking : bookings) {
            RoomEntity room = booking.getRoom();
            LocalDate contractEndDate = contractEndDate(booking);

            if (booking.getStatus() == RentalBookingEntity.Status.DEPOSIT_PAID
                    && !booking.getMoveInDate().isAfter(today)) {
                booking.setStatus(RentalBookingEntity.Status.ACTIVE);
                booking.setPaymentMessage("Đã nhận cọc. Hợp đồng thuê đang có hiệu lực.");
                room.setStatus(RoomEntity.RoomStatus.RENTED);
            }

            if (booking.getStatus() == RentalBookingEntity.Status.ACTIVE
                    && !today.isBefore(contractEndDate.minusDays(30))
                    && !today.isAfter(contractEndDate)) {
                booking.setStatus(RentalBookingEntity.Status.EXPIRING_SOON);
                booking.setPaymentMessage("Hợp đồng sắp hết hạn trong vòng 30 ngày. Người thuê có thể yêu cầu gia hạn, chủ trọ có thể mở phòng cho khách mới xem trước.");
                notificationService.notifyUser(
                        booking.getSeeker(),
                        "CONTRACT_EXPIRING",
                        "Hợp đồng sắp hết hạn",
                        "Hợp đồng thuê phòng \"" + room.getTitle() + "\" sẽ hết hạn vào ngày " + contractEndDate + ".",
                        booking.getId()
                );
                notificationService.notifyUser(
                        booking.getLandlord(),
                        "CONTRACT_EXPIRING",
                        "Hợp đồng sắp hết hạn",
                        "Hợp đồng thuê phòng \"" + room.getTitle() + "\" sẽ hết hạn vào ngày " + contractEndDate + ".",
                        booking.getId()
                );
            }

            if (ACTIVE_CONTRACT_STATUSES.contains(booking.getStatus()) && !today.isBefore(contractEndDate)) {
                booking.setStatus(RentalBookingEntity.Status.COMPLETED);
                if (room.getStatus() == RoomEntity.RoomStatus.AVAILABLE_SOON) {
                    room.setStatus(RoomEntity.RoomStatus.ACTIVE);
                    booking.setPaymentMessage("Hợp đồng cũ đã kết thúc. Phòng đang hiển thị lại cho khách mới.");
                } else {
                    room.setStatus(RoomEntity.RoomStatus.HIDDEN_REVIEW);
                    booking.setPaymentMessage("Hợp đồng đã hết hạn nhưng chưa được xử lý. Phòng đang tạm ẩn, chủ trọ có thể tự mở lại.");
                }
            }

            bookingRepo.save(booking);
            roomRepo.save(room);
        }
    }

    public LocalDate contractEndDate(RentalBookingEntity booking) {
        return booking.getMoveInDate().plusMonths(booking.getLeaseMonths()).minusDays(1);
    }
}
