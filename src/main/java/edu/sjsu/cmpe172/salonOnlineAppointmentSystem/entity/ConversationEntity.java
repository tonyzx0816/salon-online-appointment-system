package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("conversations")
public record ConversationEntity(
        @Id
        Integer conversationId,
        Integer customerId,
        Integer providerId,
        Integer appointmentId,
        Status status,
        LocalDateTime createdAt
) {
    public enum Status {
        OPEN, CLOSED
    }
}
