package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationRequest;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class BookingConfirmationNotificationService {
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
        return notificationClient.send(request);
    }
}
