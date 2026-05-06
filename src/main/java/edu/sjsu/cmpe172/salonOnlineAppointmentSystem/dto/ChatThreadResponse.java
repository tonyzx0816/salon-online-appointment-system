package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.dto;

import java.util.List;

public record ChatThreadResponse(
        String headline,
        String peerName,
        String appointmentSummary,
        List<ChatMessageLine> messages
) {
    public record ChatMessageLine(
            String text,
            String sentAt,
            boolean fromMe
    ) {
    }
}
