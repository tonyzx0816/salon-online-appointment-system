package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.AppointmentEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AppointmentRepository extends CrudRepository<AppointmentEntity, Integer> {
    List<AppointmentEntity> findByCustomerIdOrderByStartTimeDesc(Integer customerId);

    List<AppointmentEntity> findByProviderIdOrderByStartTimeDesc(Integer providerId);

    List<AppointmentEntity> findByCustomerIdAndStatusOrderByCreatedAtDesc(
            Integer customerId,
            AppointmentEntity.Status status
    );

    List<AppointmentEntity> findByProviderIdAndStatusOrderByCreatedAtDesc(
            Integer providerId,
            AppointmentEntity.Status status
    );
}

