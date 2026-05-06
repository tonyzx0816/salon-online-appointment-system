package edu.sjsu.cmpe172.salonOnlineAppointmentSystem.repository;

import edu.sjsu.cmpe172.salonOnlineAppointmentSystem.entity.UserEntity;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends CrudRepository<UserEntity, Integer> {
    Optional<UserEntity> findByEmail(String email);

    Optional<UserEntity> findByEmailIgnoreCase(String email);
}

