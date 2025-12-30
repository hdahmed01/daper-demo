package tn.ahmed.userservice.service;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import tn.ahmed.userservice.entity.AppUser;
import tn.ahmed.userservice.repo.AppUserRepository;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private AppUserRepository repository;

    // Save or update user
    public AppUser upsertUser(String keycloakId, String username, String email) {
        return repository.findByKeycloakId(keycloakId)
                .map(user -> {
                    user.setUsername(username);
                    user.setEmail(email);
                    return repository.save(user);
                })
                .orElseGet(() -> repository.save(AppUser.builder()
                        .keycloakId(keycloakId)
                        .username(username)
                        .email(email)
                        .build()));
    }

    public List<AppUser> getAllUsers() {
        return repository.findAll();
    }

    public AppUser getUserByKeycloakId(String keycloakId) {
        return repository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + keycloakId));
    }
}
