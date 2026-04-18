package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationRequest;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class BookingConfirmationNotificationClient {
    private final RestClient externalNotificationRestClient;

    public BookingConfirmationNotificationClient(
            @Qualifier("externalNotificationRestClient") RestClient externalNotificationRestClient
    ) {
        this.externalNotificationRestClient = externalNotificationRestClient;
    }

    public BookingConfirmationNotificationResponse send(BookingConfirmationNotificationRequest request) {
        return externalNotificationRestClient.post()
                .uri("/external/v1/notifications/booking-confirmations")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(BookingConfirmationNotificationResponse.class);
    }
}
