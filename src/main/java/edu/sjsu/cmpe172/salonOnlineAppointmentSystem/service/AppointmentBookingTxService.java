package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AvailabilitySlotRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AppointmentBookingTxService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;

    public AppointmentBookingTxService(
            AppointmentRepository appointmentRepository,
            AvailabilitySlotRepository slotRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    /**
     * Executes ONE booking attempt inside a single database transaction.
     * If optimistic locking fails, the transaction is rolled back and the caller can retry.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentEntity bookOnce(Integer slotId, String customerName, String customerEmail) {
        LocalDateTime now = LocalDateTime.now();

        AvailabilitySlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (slot.status() != AvailabilitySlotEntity.Status.OPEN) {
            // Concurrency-friendly user message (avoid leaking internal details like "slotId" here).
            throw new IllegalStateException("Sorry, this slot was just booked by someone else. Please choose another slot.");
        }

        // 1) Locking step (optimistic): update OPEN -> BLOCKED with @Version check.
        // If another transaction modified the slot first, this save throws an optimistic locking failure.
        LocalDateTime slotCreatedAt = slot.createdAt() != null ? slot.createdAt() : now;
        slotRepository.save(new AvailabilitySlotEntity(
                slot.slotId(),
                slot.version(),
                slot.providerId(),
                slot.startTime(),
                slot.endTime(),
                AvailabilitySlotEntity.Status.BLOCKED,
                slotCreatedAt
        ));

        // 2) Create appointment (only after slot update succeeded).
        UserEntity customer = userRepository.findByEmail(customerEmail)
                .orElseGet(() -> userRepository.save(new UserEntity(
                        null,
                        customerName,
                        customerEmail,
                        null,
                        "dev-placeholder",
                        UserEntity.Role.CUSTOMER,
                        now
                )));

        // For this template, default to the seeded service_id=1
        int defaultServiceId = 1;

        return appointmentRepository.save(new AppointmentEntity(
                null,
                customer.userId(),
                slot.providerId(),
                defaultServiceId,
                slot.startTime(),
                slot.endTime(),
                AppointmentEntity.Status.BOOKED,
                now
        ));
    }
}

