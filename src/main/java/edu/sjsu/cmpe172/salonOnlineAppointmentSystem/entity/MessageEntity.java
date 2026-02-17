package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("messages")
public record MessageEntity(
        @Id
        Integer messageId,
        Integer conversationId,
        Integer senderId,
        String content,
        LocalDateTime sentAt,
        LocalDateTime readAt
) {
}
