package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.ConversationEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends CrudRepository<ConversationEntity, Integer> {
    Optional<ConversationEntity> findByAppointmentId(Integer appointmentId);

    List<ConversationEntity> findByCustomerIdOrProviderIdOrderByCreatedAtDesc(Integer customerId, Integer providerId);
}
