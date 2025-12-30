package tn.ahmed.userservice;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}
	@Bean
	public Keycloak keycloakAdmin() {
		return KeycloakBuilder.builder()
				.serverUrl("http://localhost:8090")
				.realm("master")        // use master realm for admin login
				.username("admin")      // Keycloak admin username
				.password("admin")      // Keycloak admin password
				.clientId("admin-cli")  // standard admin client
				.grantType(OAuth2Constants.PASSWORD)
				.build();
	}
}
