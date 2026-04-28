package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationRequest;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class BookingConfirmationNotificationService {
    private static final Logger log = LoggerFactory.getLogger(BookingConfirmationNotificationService.class);

    private final AppointmentService appointmentService;
    private final UserRepository userRepository;
    private final ProviderRepository providerRepository;
    private final BookingConfirmationNotificationClient notificationClient;

    public BookingConfirmationNotificationService(
            AppointmentService appointmentService,
            UserRepository userRepository,
            ProviderRepository providerRepository,
            BookingConfirmationNotificationClient notificationClient
    ) {
        this.appointmentService = appointmentService;
        this.userRepository = userRepository;
        this.providerRepository = providerRepository;
        this.notificationClient = notificationClient;
    }

    public BookingConfirmationNotificationResponse dispatch(Integer appointmentId) {
        log.info("notification.dispatch.start appointmentId={}", appointmentId);
        AppointmentEntity appt = appointmentService.getById(appointmentId);
        UserEntity customer = userRepository.findById(appt.customerId())
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + appt.customerId()));
        ProviderEntity provider = providerRepository.findById(appt.providerId())
                .orElseThrow(() -> new IllegalArgumentException("Provider not found: " + appt.providerId()));

        BookingConfirmationNotificationRequest request = new BookingConfirmationNotificationRequest(
                appt.appointmentId(),
                customer.name(),
                customer.email(),
                provider.displayName(),
                appt.serviceId(),
                appt.startTime(),
                appt.endTime(),
                "EMAIL"
        );
        try {
            BookingConfirmationNotificationResponse response = notificationClient.send(request);
            log.info(
                    "notification.dispatch.success appointmentId={} externalNotificationId={} status={}",
                    appointmentId,
                    response.notificationId(),
                    response.status()
            );
            return response;
        } catch (RuntimeException ex) {
            log.error(
                    "notification.dispatch.failed appointmentId={} reason={}",
                    appointmentId,
                    ex.getClass().getSimpleName(),
                    ex
            );
            throw ex;
        }
    }
}
