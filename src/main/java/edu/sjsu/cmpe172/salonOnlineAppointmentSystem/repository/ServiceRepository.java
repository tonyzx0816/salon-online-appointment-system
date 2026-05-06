package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ServiceEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ServiceRepository extends CrudRepository<ServiceEntity, Integer> {
}
