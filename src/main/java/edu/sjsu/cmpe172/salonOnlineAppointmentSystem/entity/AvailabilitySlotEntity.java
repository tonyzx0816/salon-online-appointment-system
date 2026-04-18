package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("availability_slots")
public record AvailabilitySlotEntity(
        @Id
        Integer slotId,
        @Version
        Integer version,
        Integer providerId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Status status,
        LocalDateTime createdAt
) {
    public enum Status {
        OPEN, BLOCKED
    }
}
