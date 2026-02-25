package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ProviderEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProviderRepository extends CrudRepository<ProviderEntity, Integer> {

}
