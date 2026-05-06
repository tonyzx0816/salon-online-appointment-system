package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.service;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ServiceEntity;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.model.SlotDTO;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.AvailabilitySlotRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ProviderRepository;
import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository.ServiceRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class SlotService {
    private final AvailabilitySlotRepository slotRepository;
    private final ProviderRepository providerRepository;
    private final ServiceRepository serviceRepository;

    public SlotService(
            AvailabilitySlotRepository slotRepository,
            ProviderRepository providerRepository,
            ServiceRepository serviceRepository
    ) {
        this.slotRepository = slotRepository;
        this.providerRepository = providerRepository;
        this.serviceRepository = serviceRepository;
    }

    public List<SlotDTO> getOpenSlotsWithProviderNames() {
        List<AvailabilitySlotEntity> slots =
                slotRepository.findByStatus(AvailabilitySlotEntity.Status.OPEN);
        LocalDateTime now = LocalDateTime.now();

        return slots.stream().map(slot -> {
            if (slot.startTime() == null || !slot.startTime().isAfter(now)) {
                return null;
            }

            // Find provider by ID, default to "Unknown" if not found
            String providerName = providerRepository.findById(slot.providerId())
                    .map(ProviderEntity::displayName)
                    .orElse("Unknown Provider");

            String serviceName = slot.serviceId() == null
                    ? "—"
                    : serviceRepository.findById(slot.serviceId())
                            .map(ServiceEntity::name)
                            .orElse("—");

            return new SlotDTO(
                    slot.slotId(),
                    providerName,
                    serviceName,
                    slot.startTime(),
                    slot.endTime()
            );
        }).filter(slot -> slot != null).collect(Collectors.toList());
    }

    /** Open future slots for one provider (e.g. reschedule with the same stylist). */
    public List<SlotDTO> getOpenFutureSlotsForProvider(Integer providerId) {
        LocalDateTime now = LocalDateTime.now();
        String providerName = providerRepository.findById(providerId)
                .map(ProviderEntity::displayName)
                .orElse("Unknown Provider");
        return slotRepository.findByStatus(AvailabilitySlotEntity.Status.OPEN).stream()
                .filter(s -> s.providerId().equals(providerId))
                .filter(s -> s.startTime() != null && s.startTime().isAfter(now))
                .map(slot -> {
                    String serviceName = slot.serviceId() == null
                            ? "—"
                            : serviceRepository.findById(slot.serviceId())
                                    .map(ServiceEntity::name)
                                    .orElse("—");
                    return new SlotDTO(
                            slot.slotId(),
                            providerName,
                            serviceName,
                            slot.startTime(),
                            slot.endTime()
                    );
                })
                .collect(Collectors.toList());
    }
}
