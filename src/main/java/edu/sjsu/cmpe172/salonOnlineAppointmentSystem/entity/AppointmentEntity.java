package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("appointments")
public record AppointmentEntity(
        @Id
        Integer appointmentId,
        Integer customerId,
        Integer providerId,
        Integer serviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Status status,
        LocalDateTime createdAt
) {
    public enum Status {
        BOOKED, CANCELLED, COMPLETED
    }
}
