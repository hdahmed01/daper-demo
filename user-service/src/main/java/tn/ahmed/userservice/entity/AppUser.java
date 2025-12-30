package tn.ahmed.userservice.entity;



import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "users")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AppUser {

    @Id
    private String id;

    private String keycloakId;  // Keycloak user ID
    private String username;
    private String email;
    private String teamId;      // optional
}
