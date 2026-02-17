package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("services")
public record ServiceEntity(
        @Id
        Integer serviceId,
        String name,
        Integer durationMinutes,
        Integer price,
        Boolean active
) {
}
