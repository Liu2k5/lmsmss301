package com.example.user_service.service;

import com.example.user_service.dto.RegisterRequest;
import com.example.user_service.dto.AdminUserUpdateRequest;
import com.example.user_service.entity.Role;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Service
public class KeycloakAdminService {

    private final RestClient rest = RestClient.create();

    @Value("${keycloak.url}")
    private String url;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String secret;

    private String adminToken() {

        var form = new LinkedMultiValueMap<String, String>();

        form.add("grant_type", "client_credentials");
        form.add("client_id", clientId);
        form.add("client_secret", secret);

        Map<?, ?> res = rest.post()
            .uri(url + "/realms/" + realm
                + "/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(form)
            .retrieve()
            .body(Map.class);

        return (String) res.get("access_token");
    }

    public void createUser(RegisterRequest r) {

        String token = adminToken();

        String[] name = r.fullname().trim().split("\\s+", 2);

        Map<String, Object> body = Map.of(
            "username", r.username(),
            "email", r.email(),
            "firstName", name[0],
            "lastName", name.length > 1 ? name[1] : name[0],
            "enabled", true,
            "emailVerified", false,
            "credentials", List.of(
                Map.of(
                    "type", "password",
                    "value", r.password(),
                    "temporary", false
                )
            )
        );

        rest.post()
            .uri(url + "/admin/realms/" + realm + "/users")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(body)
            .retrieve()
            .toBodilessEntity();

        List<Map<String, Object>> found = rest.get()
            .uri(url + "/admin/realms/" + realm
                + "/users?username=" + r.username() + "&exact=true")
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        String userId = (String) found.get(0).get("id");

        Map<String, Object> role = rest.get()
            .uri(url + "/admin/realms/" + realm
                + "/roles/" + r.role().realmName())
            .header("Authorization", "Bearer " + token)
            .retrieve()
            .body(new ParameterizedTypeReference<>() {});

        rest.post()
            .uri(url + "/admin/realms/" + realm
                + "/users/" + userId + "/role-mappings/realm")
            .header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON)
            .body(List.of(role))
            .retrieve()
            .toBodilessEntity();
    }

    public void updateUser(String username, AdminUserUpdateRequest request, Role previousRole) {
        String token = adminToken();
        String userId = findUserId(username, token);

        Map<String, Object> current = rest.get()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        Map<String, Object> updated = new HashMap<>(current);

        String[] name = request.fullname().trim().split("\\s+", 2);
        updated.put("firstName", name[0]);
        updated.put("lastName", name.length > 1 ? name[1] : name[0]);
        updated.put("enabled", request.enabled());

        rest.put()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(updated)
                .retrieve()
                .toBodilessEntity();

        if (previousRole != request.role()) {
            Map<String, Object> oldRole = getRealmRole(previousRole, token);
            rest.method(org.springframework.http.HttpMethod.DELETE)
                    .uri(url + "/admin/realms/" + realm + "/users/" + userId
                            + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(oldRole))
                    .retrieve()
                    .toBodilessEntity();

            Map<String, Object> newRole = getRealmRole(request.role(), token);
            rest.post()
                    .uri(url + "/admin/realms/" + realm + "/users/" + userId
                            + "/role-mappings/realm")
                    .header("Authorization", "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(List.of(newRole))
                    .retrieve()
                    .toBodilessEntity();
        }
    }

    public void setUserEnabled(String username, boolean enabled) {
        String token = adminToken();
        String userId = findUserId(username, token);
        Map<String, Object> current = rest.get()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
        current.put("enabled", enabled);

        rest.put()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(current)
                .retrieve()
                .toBodilessEntity();
    }

    public void resetPassword(String username, String newPassword) {
        String token = adminToken();
        String userId = findUserId(username, token);
        rest.put()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId + "/reset-password")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("type", "password", "value", newPassword, "temporary", false))
                .retrieve()
                .toBodilessEntity();
    }

    public void changePassword(String currentPassword, String newPassword, String accessToken) {
        rest.put()
                .uri(url + "/realms/" + realm + "/account/credentials/password")
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of(
                        "currentPassword", currentPassword,
                        "newPassword", newPassword,
                        "confirmation", newPassword))
                .retrieve()
                .toBodilessEntity();
    }

    public void replaceRealmRole(String username, Role previousRole, Role newRole) {
        String token = adminToken();
        String userId = findUserId(username, token);
        Map<String, Object> oldRole = getRealmRole(previousRole, token);
        rest.method(org.springframework.http.HttpMethod.DELETE)
                .uri(url + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(oldRole))
                .retrieve()
                .toBodilessEntity();

        Map<String, Object> role = getRealmRole(newRole, token);
        rest.post()
                .uri(url + "/admin/realms/" + realm + "/users/" + userId + "/role-mappings/realm")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(role))
                .retrieve()
                .toBodilessEntity();
    }

    private String findUserId(String username, String token) {
        List<Map<String, Object>> found = rest.get()
                .uri(url + "/admin/realms/" + realm
                        + "/users?username={username}&exact=true", username)
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});

        if (found == null || found.isEmpty()) {
            throw new IllegalStateException("Keycloak user not found: " + username);
        }
        return (String) found.get(0).get("id");
    }

    private Map<String, Object> getRealmRole(Role role, String token) {
        return rest.get()
                .uri(url + "/admin/realms/" + realm + "/roles/" + role.realmName())
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }
}