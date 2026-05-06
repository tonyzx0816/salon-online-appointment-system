package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.controller;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification.BookingConfirmationNotificationResponse;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service.BookingConfirmationNotificationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/appointments")
public class AppointmentNotificationRestController {
    private final BookingConfirmationNotificationService confirmationNotificationService;

    public AppointmentNotificationRestController(
            BookingConfirmationNotificationService confirmationNotificationService
    ) {
        this.confirmationNotificationService = confirmationNotificationService;
    }

    /**
     * Re-sends booking confirmation email (SMTP). Same path as after booking; requires mail to be configured.
     */
    @PostMapping("/{appointmentId}/confirmation-notification")
    public ResponseEntity<BookingConfirmationNotificationResponse> sendConfirmationNotification(
            @PathVariable Integer appointmentId
    ) {
        BookingConfirmationNotificationResponse body = confirmationNotificationService.dispatch(appointmentId);
        return ResponseEntity.ok(body);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleNotFound(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<String> handleMailNotConfigured(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(ex.getMessage());
    }
}
