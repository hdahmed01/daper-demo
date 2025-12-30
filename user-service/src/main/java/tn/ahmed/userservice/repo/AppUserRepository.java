package tn.ahmed.userservice.repo;


import org.springframework.data.mongodb.repository.MongoRepository;
import tn.ahmed.userservice.entity.AppUser;

import java.util.Optional;

public interface AppUserRepository extends MongoRepository<AppUser, String> {
    Optional<AppUser> findByKeycloakId(String keycloakId);
}
