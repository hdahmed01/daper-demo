package tn.ahmed.userservice.service;



import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakSyncService {

    @Autowired
    private Keycloak keycloakAdmin;

    @Autowired
    private UserService userService;

    private static final String REALM = "taskmanagement";

    // Runs every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void syncUsers() {
        List<UserRepresentation> users = keycloakAdmin.realm(REALM).users().list();
        for (UserRepresentation kcUser : users) {
            userService.upsertUser(
                    kcUser.getId(),
                    kcUser.getUsername(),
                    kcUser.getEmail()
            );
        }
        System.out.println("✅ Keycloak users synced: " + users.size());
    }
}
