package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.model;

import java.time.LocalDateTime;

public record SlotDTO(
        Integer slotId,
        String providerName,
        LocalDateTime startTime,
        LocalDateTime endTime
) {
}
