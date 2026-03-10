package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AvailabilitySlotRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AvailabilitySlotRepository slotRepository;
    private final UserRepository userRepository;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AvailabilitySlotRepository slotRepository,
            UserRepository userRepository
    ) {
        this.appointmentRepository = appointmentRepository;
        this.slotRepository = slotRepository;
        this.userRepository = userRepository;
    }

    public AppointmentEntity book(Integer slotId, String customerName, String customerEmail) {
        LocalDateTime now = LocalDateTime.now();

        AvailabilitySlotEntity slot = slotRepository.findById(slotId)
                .orElseThrow(() -> new IllegalArgumentException("Slot not found: " + slotId));

        if (slot.status() != AvailabilitySlotEntity.Status.OPEN) {
            throw new IllegalStateException("Slot is not OPEN: " + slotId);
        }

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

        // For M3 skeleton purposes, default to the seeded service_id=1
        int defaultServiceId = 1;

        AppointmentEntity saved = appointmentRepository.save(new AppointmentEntity(
                null,
                customer.userId(),
                slot.providerId(),
                defaultServiceId,
                slot.startTime(),
                slot.endTime(),
                AppointmentEntity.Status.BOOKED,
                now
        ));

        LocalDateTime slotCreatedAt = slot.createdAt() != null ? slot.createdAt() : now;
        slotRepository.save(new AvailabilitySlotEntity(
                slot.slotId(),
                slot.providerId(),
                slot.startTime(),
                slot.endTime(),
                AvailabilitySlotEntity.Status.BLOCKED,
                slotCreatedAt
        ));

        return saved;
    }

    public AppointmentEntity getById(Integer appointmentId) {
        return appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));
    }

    public List<AppointmentEntity> listAll() {
        List<AppointmentEntity> out = new ArrayList<>();
        appointmentRepository.findAll().forEach(out::add);
        return out;
    }
}

