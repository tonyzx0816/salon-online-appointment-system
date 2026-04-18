package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification;

import java.time.LocalDateTime;

/**
 * Single coarse-grained payload sent to the external notification vendor:
 * everything required to render and deliver one booking confirmation in one request.
 */
public record BookingConfirmationNotificationRequest(
        Integer appointmentId,
        String customerName,
        String customerEmail,
        String providerDisplayName,
        Integer serviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        String channel
) {
}
