package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final AppointmentBookingTxService bookingTxService;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentBookingTxService bookingTxService
    ) {
        this.appointmentRepository = appointmentRepository;
        this.bookingTxService = bookingTxService;
    }

    public AppointmentEntity book(Integer slotId, String customerName, String customerEmail) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                return bookingTxService.bookOnce(slotId, customerName, customerEmail);
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                // Another transaction won the race and updated this slot first.
                if (attempt == maxRetries) {
                    throw new IllegalStateException(
                            "Could not book slot " + slotId + " due to concurrent updates. Please try again.",
                            ex
                    );
                }

                // Small backoff to reduce retry storms under contention.
                try {
                    Thread.sleep(50L * attempt);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Should be unreachable because we always either return or throw in the loop.
        throw new IllegalStateException("Could not book slot " + slotId + ".");
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

