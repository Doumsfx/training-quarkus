package com.example;

import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.quarkus.security.Authenticated;

@Path("/api/admin/users")
@Authenticated
public class ApiAdminUsersResource {

    @Inject
    KeycloakService keycloakService;

    @GET
    @Path("/count")
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("admin")
    public CountResponse getUserCount() {
        Integer count = keycloakService.getUserCount();
        return new CountResponse(count == null ? 0 : count);
    }

    public record CountResponse(int count) {}

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)

    public Response createUser(CreateUserRequest request) {
        try {
            if (request == null) {
                return Response.status(400).entity(new ErrorResponse("Body requis")).build();
            }

            String userId = keycloakService.createUser(
                    request.username(),
                    request.email(),
                    request.firstName(),
                    request.lastName());

            return Response.status(Response.Status.CREATED)
                    .entity(new CreateUserResponse(userId))
                    .build();
        } catch (jakarta.ws.rs.WebApplicationException e) {
            int status = e.getResponse() != null ? e.getResponse().getStatus() : 500;
            return Response.status(status)
                    .entity(new ErrorResponse(e.getMessage()))
                    .build();
        } catch (Exception e) {
            return Response.status(500)
                    .entity(new ErrorResponse("Erreur interne"))
                    .build();
        }
    }

    public record CreateUserRequest(String username, String email, String firstName, String lastName) {}

    public record CreateUserResponse(String id) {}

    public record ErrorResponse(String error) {}
}
