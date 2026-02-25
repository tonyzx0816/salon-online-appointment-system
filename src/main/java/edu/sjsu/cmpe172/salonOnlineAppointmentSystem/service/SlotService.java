package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.model.SlotDTO;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AvailabilitySlotRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotService {
    private final AvailabilitySlotRepository slotRepository;
    private final ProviderRepository providerRepository;

    public SlotService(AvailabilitySlotRepository slotRepository, ProviderRepository providerRepository) {
        this.slotRepository = slotRepository;
        this.providerRepository = providerRepository;
    }

    public List<SlotDTO> getOpenSlotsWithProviderNames() {
        List<AvailabilitySlotEntity> slots =
                slotRepository.findByStatus(AvailabilitySlotEntity.Status.OPEN);
        return slots.stream().map(slot -> {
            // Find provider by ID, default to "Unknown" if not found
            String providerName = providerRepository.findById(slot.providerId())
                    .map(ProviderEntity::displayName)
                    .orElse("Unknown Provider");

            return new SlotDTO(
                    slot.slotId(),
                    providerName,
                    slot.startTime(),
                    slot.endTime()
            );
        }).collect(Collectors.toList());
    }
}
