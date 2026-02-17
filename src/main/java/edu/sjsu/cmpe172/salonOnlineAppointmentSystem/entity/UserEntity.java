package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Table("users")
public record UserEntity(
        @Id
        Integer userId,
        String name,
        String email,
        String phone,
        String passwordHash,
        Role role,
        LocalDateTime createdAt
) {
    public enum Role {
        CUSTOMER, PROVIDER, ADMIN
    }
}
