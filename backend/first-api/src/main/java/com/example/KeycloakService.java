package com.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;
import jakarta.ws.rs.core.Form;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class KeycloakService {

    @RestClient
    KeycloakAdminClient keycloakAdminClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "quarkus.oidc.auth-server-url")
    String authServerUrl;

    @ConfigProperty(name = "quarkus.oidc.client-id")
    String clientId;

    @ConfigProperty(name = "quarkus.oidc.credentials.secret")
    String clientSecret;

    // Client "service account" recommandé pour appeler l'Admin API.
    // Si non renseigné, on fallback sur quarkus.oidc.* (moins idéal en prod).
    @ConfigProperty(name = "vsafe.keycloak.admin.client-id", defaultValue = "")
    String adminClientId;

    @ConfigProperty(name = "vsafe.keycloak.admin.client-secret", defaultValue = "")
    String adminClientSecret;

    @ConfigProperty(name = "vsafe.keycloak.realm", defaultValue = "vsafe-next")
    String realm;

    @ConfigProperty(name = "quarkus.rest-client.keycloak-admin.url", defaultValue = "http://localhost:8180")
    String keycloakAdminBaseUrl;

    private String adminAccessToken;
    private long tokenExpirationTime;

    /**
     * Obtient un token d'accès pour l'API Admin de Keycloak
     */
    public String getAdminAccessToken() {
        // Vérifie si le token est encore valide (avec 30 secondes de marge)
        if (adminAccessToken != null && System.currentTimeMillis() < tokenExpirationTime - 30000) {
            return adminAccessToken;
        }

        Client client = ClientBuilder.newClient();
        try {
            String effectiveClientId = (adminClientId != null && !adminClientId.isBlank()) ? adminClientId : clientId;
            String effectiveClientSecret = (adminClientSecret != null && !adminClientSecret.isBlank()) ? adminClientSecret : clientSecret;

            if (effectiveClientId == null || effectiveClientId.isBlank() || effectiveClientSecret == null || effectiveClientSecret.isBlank()) {
                throw new IllegalStateException(
                        "Client admin Keycloak non configuré. Configure vsafe.keycloak.admin.client-id et vsafe.keycloak.admin.client-secret");
            }

            Form form = new Form()
                    .param("grant_type", "client_credentials")
                    .param("client_id", effectiveClientId)
                    .param("client_secret", effectiveClientSecret);

            Response response = client.target(authServerUrl + "/protocol/openid-connect/token")
                    .request(MediaType.APPLICATION_JSON)
                    .post(Entity.form(form));

            if (response.getStatus() != 200) {
                String body = null;
                try {
                    body = response.readEntity(String.class);
                } catch (Exception ignored) {
                    // ignore
                }

                throw new RuntimeException("Token client-credentials refusé par Keycloak (status=" + response.getStatus() + ")" +
                        (body != null ? ": " + body : ""));
            }

            String json = response.readEntity(String.class);
            TokenResponse tokenResponse = objectMapper.readValue(json, TokenResponse.class);

            adminAccessToken = tokenResponse.access_token;
            tokenExpirationTime = System.currentTimeMillis() + (tokenResponse.expires_in * 1000L);
            return adminAccessToken;
        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de l'obtention du token admin Keycloak: " + e.getMessage(), e);
        } finally {
            client.close();
        }
    }

    /**
     * Obtient le nombre d'utilisateurs dans le realm
     */
    public Integer getUserCount() {
        try {
            String token = getAdminAccessToken();
            return keycloakAdminClient.getUsersCount("Bearer " + token, realm);
        } catch (WebApplicationException e) {
            throw new RuntimeException("Erreur lors de l'appel à l'API Keycloak: " + e.getMessage(), e);
        }
    }

    /**
     * Crée un user Keycloak avec un payload minimal.
     */
    public String createUser(String username, String email, String firstName, String lastName) {
        if (username == null || username.isBlank()) {
            throw new WebApplicationException("username est requis", 400);
        }

        String token = getAdminAccessToken();

        Client client = ClientBuilder.newClient();
        try {
            var payload = objectMapper.createObjectNode();
            payload.put("username", username.trim());
            payload.put("enabled", true);

            if (email != null && !email.isBlank()) payload.put("email", email);
            if (firstName != null && !firstName.isBlank()) payload.put("firstName", firstName);
            if (lastName != null && !lastName.isBlank()) payload.put("lastName", lastName);

            Response response = client
                    .target(keycloakAdminBaseUrl)
                    .path("admin")
                    .path("realms")
                    .path(realm)
                    .path("users")
                    .request(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + token)
                    .post(Entity.entity(payload.toString(), MediaType.APPLICATION_JSON));

            try {
                if (response.getStatus() != 201) {
                    String body = null;
                    try {
                        body = response.readEntity(String.class);
                    } catch (Exception ignored) {
                        // ignore
                    }
                    throw new WebApplicationException(
                            "Création user Keycloak refusée (status=" + response.getStatus() + ")" + (body != null ? ": " + body : ""),
                            response.getStatus());
                }

                String location = response.getHeaderString("Location");
                if (location == null || location.isBlank()) {
                    throw new WebApplicationException("Keycloak n'a pas renvoyé de header Location", 502);
                }

                return extractIdFromLocation(location);
            } finally {
                try {
                    response.close();
                } catch (Exception ignored) {
                    // ignore
                }
            }
        } finally {
            client.close();
        }
    }

    private static String extractIdFromLocation(String location) {
        String normalized = location.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    /**
     * Classe interne pour deserialiser la réponse du token
     */
    public static class TokenResponse {
        public String access_token;
        public Integer expires_in;
    }
}
