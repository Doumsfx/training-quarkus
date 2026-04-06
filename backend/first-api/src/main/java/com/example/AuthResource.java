package com.example;

import io.quarkus.oidc.IdToken;
import io.quarkus.security.Authenticated;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import org.eclipse.microprofile.jwt.JsonWebToken;

@Path("/auth")
public class AuthResource {

    @Inject
    @IdToken
    JsonWebToken idToken;

    @Inject
    JsonWebToken accessToken;

    @GET
    @Path("/check")
    @Produces(MediaType.APPLICATION_JSON)
    public Response check(@Context SecurityContext securityContext) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        String username = securityContext.getUserPrincipal().getName();
        return Response.ok(new UserInfoResponse(username, null, null, null)).build();
    }

    @GET
    @Path("/userinfo")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public UserInfoResponse getUserInfo(@Context SecurityContext securityContext) {
        String username = securityContext.getUserPrincipal().getName();
        String email = idToken.getClaim("email");
        String firstName = idToken.getClaim("given_name");
        String lastName = idToken.getClaim("family_name");

        return new UserInfoResponse(username, email, firstName, lastName);
    }

    @GET
    @Path("/token")
    @Produces(MediaType.APPLICATION_JSON)
    @Authenticated
    public AccessTokenResponse getAccessToken() {
        return new AccessTokenResponse(accessToken.getRawToken());
    }

    @GET
    @Path("/login")
    @Authenticated
    public Response login() {
        // Après authentification OIDC, on renvoie vers le frontend.
        return Response.seeOther(java.net.URI.create("http://localhost:5173/")).build();
    }

    @GET
    @Path("/post-logout")
    public Response postLogout() {
        return Response.seeOther(java.net.URI.create("http://localhost:5173/")).build();
    }
    

    /**
     * Classe pour la réponse des infos utilisateur
     */
    public static class UserInfoResponse {
        public String username;
        public String email;
        public String firstName;
        public String lastName;
        public String timestamp;

        public UserInfoResponse(String username, String email, String firstName, String lastName) {
            this.username = username;
            this.email = email;
            this.firstName = firstName;
            this.lastName = lastName;
            this.timestamp = java.time.Instant.now().toString();
        }
    }

    public record AccessTokenResponse(String accessToken) {}
}
