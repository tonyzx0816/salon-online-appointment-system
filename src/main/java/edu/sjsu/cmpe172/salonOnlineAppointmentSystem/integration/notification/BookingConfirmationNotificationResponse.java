package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.integration.notification;

public record BookingConfirmationNotificationResponse(
        String notificationId,
        String status
) {
}
