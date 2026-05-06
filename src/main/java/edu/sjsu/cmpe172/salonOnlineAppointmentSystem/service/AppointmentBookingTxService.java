package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AvailabilitySlotRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AppointmentBookingTxService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AppointmentBookingTxService(
            AppointmentRepository appointmentRepository,
            AvailabilitySlotRepository slotRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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
                slot.serviceId(),
                slot.startTime(),
                slot.endTime(),
                AvailabilitySlotEntity.Status.BLOCKED,
                slotCreatedAt
        ));

        // 2) Create appointment (only after slot update succeeded).
        UserEntity customer = userRepository.findByEmail(customerEmail)
                .or(() -> userRepository.findByEmailIgnoreCase(customerEmail))
                .orElseGet(() -> userRepository.save(new UserEntity(
                        null,
                        customerName,
                        customerEmail,
                        null,
                        passwordEncoder.encode("changeme"),
                        UserEntity.Role.CUSTOMER,
                        now
                )));

        int serviceId = slot.serviceId() != null ? slot.serviceId() : 1;

        return appointmentRepository.save(new AppointmentEntity(
                null,
                customer.userId(),
                slot.providerId(),
                serviceId,
                slot.startTime(),
                slot.endTime(),
                AppointmentEntity.Status.BOOKED,
                now
        ));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelBooked(Integer appointmentId, Integer customerId) {
        LocalDateTime now = LocalDateTime.now();
        AppointmentEntity appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
        if (!appt.customerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only cancel your own appointments.");
        }
        if (appt.status() != AppointmentEntity.Status.BOOKED) {
            throw new IllegalStateException("Only active bookings can be cancelled.");
        }
        if (!appt.startTime().isAfter(now)) {
            throw new IllegalStateException("Cannot cancel an appointment that has already started or ended.");
        }
        releaseBlockedSlotMatching(appt);
        appointmentRepository.save(new AppointmentEntity(
                appt.appointmentId(),
                appt.customerId(),
                appt.providerId(),
                appt.serviceId(),
                appt.startTime(),
                appt.endTime(),
                AppointmentEntity.Status.CANCELLED,
                appt.createdAt()
        ));
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AppointmentEntity rescheduleBooked(Integer appointmentId, Integer newSlotId, Integer customerId) {
        LocalDateTime now = LocalDateTime.now();
        AppointmentEntity appt = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found."));
        if (!appt.customerId().equals(customerId)) {
            throw new IllegalArgumentException("You can only reschedule your own appointments.");
        }
        if (appt.status() != AppointmentEntity.Status.BOOKED) {
            throw new IllegalStateException("Only active bookings can be rescheduled.");
        }
        if (!appt.startTime().isAfter(now)) {
            throw new IllegalStateException("Cannot reschedule an appointment that has already started or ended.");
        }
        AvailabilitySlotEntity newSlot = slotRepository.findById(newSlotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found."));
        if (!newSlot.providerId().equals(appt.providerId())) {
            throw new IllegalArgumentException("Please choose a time with the same provider.");
        }
        if (newSlot.status() != AvailabilitySlotEntity.Status.OPEN) {
            throw new IllegalStateException("That time slot is no longer available.");
        }
        if (newSlot.startTime() == null || !newSlot.startTime().isAfter(now)) {
            throw new IllegalArgumentException("Please choose a future time slot.");
        }

        blockOpenSlot(newSlot, now);
        releaseBlockedSlotMatching(appt);

        int serviceId = newSlot.serviceId() != null ? newSlot.serviceId() : appt.serviceId();
        return appointmentRepository.save(new AppointmentEntity(
                appt.appointmentId(),
                appt.customerId(),
                appt.providerId(),
                serviceId,
                newSlot.startTime(),
                newSlot.endTime(),
                AppointmentEntity.Status.BOOKED,
                appt.createdAt()
        ));
    }

    private void blockOpenSlot(AvailabilitySlotEntity slot, LocalDateTime now) {
        LocalDateTime slotCreatedAt = slot.createdAt() != null ? slot.createdAt() : now;
        slotRepository.save(new AvailabilitySlotEntity(
                slot.slotId(),
                slot.version(),
                slot.providerId(),
                slot.serviceId(),
                slot.startTime(),
                slot.endTime(),
                AvailabilitySlotEntity.Status.BLOCKED,
                slotCreatedAt
        ));
    }

    private void releaseBlockedSlotMatching(AppointmentEntity appt) {
        List<AvailabilitySlotEntity> matches = slotRepository.findByProviderIdAndStartTimeAndEndTime(
                appt.providerId(),
                appt.startTime(),
                appt.endTime()
        );
        for (AvailabilitySlotEntity s : matches) {
            if (s.status() == AvailabilitySlotEntity.Status.BLOCKED) {
                LocalDateTime created = s.createdAt() != null ? s.createdAt() : LocalDateTime.now();
                slotRepository.save(new AvailabilitySlotEntity(
                        s.slotId(),
                        s.version(),
                        s.providerId(),
                        s.serviceId(),
                        s.startTime(),
                        s.endTime(),
                        AvailabilitySlotEntity.Status.OPEN,
                        created
                ));
                return;
            }
        }
    }
}

