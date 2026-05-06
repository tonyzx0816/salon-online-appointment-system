package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;

import java.time.LocalDateTime;

public record AppointmentListRow(
        Integer appointmentId,
        String providerName,
        String customerName,
        String serviceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        AppointmentEntity.Status status,
        boolean canCancelOrReschedule
) {
}
