package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookingConfirmationNotificationService {
    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationNotificationService.class);

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final BookingConfirmationMailService mailService;

    public BookingConfirmationNotificationService(
            AppointmentService appointmentService,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            BookingConfirmationMailService mailService
    ) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.mailService = mailService;
    }

    /**
     * Sends booking confirmation only via SMTP ({@link BookingConfirmationMailService}).
     * Requires {@code spring.mail.host} and a From address ({@code app.mail.from} or {@code spring.mail.username}).
     */
    public BookingConfirmationNotificationResponse dispatch(Integer appointmentId) {
        log.info("notification.dispatch.start appointmentId={}", appointmentId);
        AppointmentEntity appt = appointmentService.getById(appointmentId);
        UserEntity customer = userRepository.findById(appt.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + appt.customerId()));
        ProviderEntity provider = providerRepository.findById(appt.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + appt.providerId()));

        Optional<String> sentId = mailService.sendBookingConfirmation(appt, customer, provider);
        if (sentId.isEmpty()) {
            throw new IllegalStateException(
                    "Confirmation email was not sent: set app.mail.from or spring.mail.username to a valid From address."
            );
        }

        log.info(
                "notification.dispatch.success appointmentId={} channel=smtp notificationId={}",
                appointmentId,
                sentId.get()
        );
        return new BookingConfirmationNotificationResponse(sentId.get(), "SENT");
    }
}
