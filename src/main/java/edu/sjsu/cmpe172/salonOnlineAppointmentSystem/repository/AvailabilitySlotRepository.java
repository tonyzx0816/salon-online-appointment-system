package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AvailabilitySlotEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AvailabilitySlotRepository extends CrudRepository<AvailabilitySlotEntity, Integer> {
    List<AvailabilitySlotEntity> findByStatus(AvailabilitySlotEntity.Status status);

    List<AvailabilitySlotEntity> findByProviderIdAndStartTimeAndEndTime(
            Integer providerId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );
}
