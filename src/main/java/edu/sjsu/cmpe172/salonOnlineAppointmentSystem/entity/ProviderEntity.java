package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("providers")
public record ProviderEntity(
        @Id
        Integer providerId,
        Integer userId,
        String displayName,
        Boolean active,
        LocalDateTime createdAt
) {
}
