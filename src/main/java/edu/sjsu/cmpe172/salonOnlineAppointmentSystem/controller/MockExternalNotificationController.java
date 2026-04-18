package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationRequest;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Stand-in for a third-party notification vendor API (same process for the demo;
 * boundary is HTTP + a vendor-shaped URL namespace).
 */
@RestController
@RequestMapping("/external/v1/notifications")
public class MockExternalNotificationController {

    @PostMapping("/booking-confirmations")
    public ResponseEntity<BookingConfirmationNotificationResponse> acceptBookingConfirmation(
            @RequestBody BookingConfirmationNotificationRequest body
    ) {
        String id = "ext-" + body.appointmentId() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return ResponseEntity.ok(new BookingConfirmationNotificationResponse(id, "ACCEPTED"));
    }
}
