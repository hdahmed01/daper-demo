package tn.ahmed.userservice.controller;



import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tn.ahmed.userservice.entity.AppUser;
import tn.ahmed.userservice.service.KeycloakSyncService;
import tn.ahmed.userservice.service.UserService;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final KeycloakSyncService keycloakSyncService;

    @Autowired
    public UserController(UserService userService, KeycloakSyncService keycloakSyncService) {
        this.userService = userService;
        this.keycloakSyncService = keycloakSyncService;
    }

    // ✅ Get all users
    @GetMapping
    public ResponseEntity<List<AppUser>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    // ✅ Get a user by Keycloak ID
    @GetMapping("/id/{keycloakId}")
    public ResponseEntity<AppUser> getUserByKeycloakId(@PathVariable String keycloakId) {
        AppUser user = userService.getUserByKeycloakId(keycloakId);
        return ResponseEntity.ok(user);
    }

    // ✅ Optional: manually trigger sync from Keycloak
    @PostMapping("/sync")
    public ResponseEntity<String> syncUsersFromKeycloak() {
        keycloakSyncService.syncUsers();
        return ResponseEntity.ok("✅ Users synced from Keycloak successfully");
    }
}
