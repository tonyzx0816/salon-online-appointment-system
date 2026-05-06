package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto.AppointmentListRow;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ServiceEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AppointmentRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ServiceRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class AppointmentService {
    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository appointmentRepository;
    private final AppointmentBookingTxService bookingTxService;
    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final Counter bookingSuccessCounter;
    private final Counter bookingFailureCounter;
    private final Timer bookingLatencyTimer;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            AppointmentBookingTxService bookingTxService,
            ProviderRepository providerRepository,
            ServiceRepository serviceRepository,
            UserRepository userRepository,
            MeterRegistry meterRegistry
    ) {
        this.appointmentRepository = appointmentRepository;
        this.bookingTxService = bookingTxService;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
        this.userRepository = userRepository;
        this.bookingSuccessCounter = meterRegistry.counter("booking_requests_total", "result", "success");
        this.bookingFailureCounter = meterRegistry.counter("booking_requests_total", "result", "failure");
        this.bookingLatencyTimer = Timer.builder("booking_latency_ms")
                .description("End-to-end booking latency in milliseconds")
                .register(meterRegistry);
    }

    public AppointmentEntity book(Integer slotId, String customerName, String customerEmail) {
        long startedNanos = System.nanoTime();
        log.info("booking.start slotId={} customerRef={}", slotId, anonymizeEmail(customerEmail));

        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                AppointmentEntity created = bookingTxService.bookOnce(slotId, customerName, customerEmail);
                long elapsedNanos = System.nanoTime() - startedNanos;
                bookingSuccessCounter.increment();
                bookingLatencyTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
                log.info(
                        "booking.success slotId={} appointmentId={} retries={} latencyMs={}",
                        slotId,
                        created.appointmentId(),
                        attempt - 1,
                        elapsedNanos / 1_000_000.0
                );
                return created;
            } catch (org.springframework.dao.OptimisticLockingFailureException ex) {
                // Another transaction won the race and updated this slot first.
                log.warn("booking.retry slotId={} attempt={} reason=optimistic-lock", slotId, attempt);
                if (attempt == maxRetries) {
                    long elapsedNanos = System.nanoTime() - startedNanos;
                    bookingFailureCounter.increment();
                    bookingLatencyTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
                    log.error(
                            "booking.failed slotId={} retries={} latencyMs={} reason=concurrency-conflict",
                            slotId,
                            attempt,
                            elapsedNanos / 1_000_000.0,
                            ex
                    );
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
                    long elapsedNanos = System.nanoTime() - startedNanos;
                    bookingFailureCounter.increment();
                    bookingLatencyTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
                    log.error(
                            "booking.failed slotId={} retries={} latencyMs={} reason=interrupted",
                            slotId,
                            attempt,
                            elapsedNanos / 1_000_000.0
                    );
                    throw new IllegalStateException("Booking retry interrupted for slot " + slotId + ".", ignored);
                }
            } catch (RuntimeException ex) {
                long elapsedNanos = System.nanoTime() - startedNanos;
                bookingFailureCounter.increment();
                bookingLatencyTimer.record(elapsedNanos, TimeUnit.NANOSECONDS);
                log.error(
                        "booking.failed slotId={} retries={} latencyMs={} reason=unexpected-error",
                        slotId,
                        attempt - 1,
                        elapsedNanos / 1_000_000.0,
                        ex
                );
                throw ex;
            }
        }

        // Should be unreachable because we always either return or throw in the loop.
        throw new IllegalStateException("Could not book slot " + slotId + ".");
    }

    private String anonymizeEmail(String customerEmail) {
        if (customerEmail == null || customerEmail.isBlank()) {
            return "unknown";
        }

        int atIndex = customerEmail.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }

        return customerEmail.charAt(0) + "***" + customerEmail.substring(atIndex);
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

    public List<AppointmentListRow> listHistoryForUser(UserEntity user) {
        List<AppointmentEntity> appts = switch (user.role()) {
            case CUSTOMER -> appointmentRepository.findByCustomerIdOrderByStartTimeDesc(user.userId());
            case PROVIDER -> providerRepository.findByUserId(user.userId())
                    .map(p -> appointmentRepository.findByProviderIdOrderByStartTimeDesc(p.providerId()))
                    .orElse(List.of());
            default -> List.of();
        };
        LocalDateTime now = LocalDateTime.now();
        List<AppointmentListRow> rows = new ArrayList<>();
        for (AppointmentEntity a : appts) {
            rows.add(toListRow(a, now));
        }
        return rows;
    }

    private AppointmentListRow toListRow(AppointmentEntity a, LocalDateTime now) {
        String providerName = providerRepository.findById(a.providerId())
                .map(ProviderEntity::displayName)
                .orElse("—");
        String customerName = userRepository.findById(a.customerId())
                .map(UserEntity::name)
                .orElse("—");
        String serviceName = serviceRepository.findById(a.serviceId())
                .map(ServiceEntity::name)
                .orElse("—");
        boolean canModify = a.status() == AppointmentEntity.Status.BOOKED && a.startTime().isAfter(now);
        return new AppointmentListRow(
                a.appointmentId(),
                providerName,
                customerName,
                serviceName,
                a.startTime(),
                a.endTime(),
                a.status(),
                canModify
        );
    }

    public void cancelForCustomer(Integer appointmentId, int customerUserId) {
        bookingTxService.cancelBooked(appointmentId, customerUserId);
    }

    public AppointmentEntity rescheduleForCustomer(Integer appointmentId, Integer newSlotId, int customerUserId) {
        return bookingTxService.rescheduleBooked(appointmentId, newSlotId, customerUserId);
    }
}
