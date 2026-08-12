package com.vassarlabs.aulm.ui.api;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vassarlabs.aulm.ui.model.*;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;

public class ApiClient {

    private final String baseUrl;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private String token;

    public ApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LoginResponseDto login(String username, String password) {
        LoginPayload payload = new LoginPayload(username, password);
        return send("POST", "/api/auth/login", payload, LoginResponseDto.class, false);
    }

    public List<UserDto> listUsers() {
        UserDto[] users = send("GET", "/api/users", null, UserDto[].class, true);
        return users == null ? List.of() : List.of(users);
    }

    public UserDto createUser(CreateUserPayload payload) {
        return send("POST", "/api/users", payload, UserDto.class, true);
    }

    public UserDto updateUser(Long id, UpdateUserPayload payload) {
        return send("PUT", "/api/users/" + id, payload, UserDto.class, true);
    }

    public void deleteUser(Long id) {
        send("DELETE", "/api/users/" + id, null, Void.class, true);
    }

    public UserDto renewLicense(Long id, RenewLicensePayload payload) {
        return send("POST", "/api/users/" + id + "/license/renew", payload, UserDto.class, true);
    }

    private <T> T send(String method, String path, Object body, Class<T> responseType, boolean authenticated) {
        try {
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + path))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json");

            if (authenticated && token != null) {
                builder.header("Authorization", "Bearer " + token);
            }

            String json = body == null ? "" : mapper.writeValueAsString(body);
            HttpRequest.BodyPublisher bodyPublisher = body == null
                    ? HttpRequest.BodyPublishers.noBody()
                    : HttpRequest.BodyPublishers.ofString(json);
            builder.method(method, bodyPublisher);

            HttpResponse<String> response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                if (responseType == Void.class || response.body() == null || response.body().isBlank()) {
                    return null;
                }
                return mapper.readValue(response.body(), responseType);
            }

            String message = extractErrorMessage(response.body());
            throw new ApiClientException(message);
        } catch (ApiClientException e) {
            throw e;
        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiClientException("Could not reach the AULM backend at " + baseUrl + ". Is it running?", e);
        }
    }

    private String extractErrorMessage(String responseBody) {
        try {
            Map<?, ?> map = mapper.readValue(responseBody, Map.class);
            Object message = map.get("message");
            return message != null ? message.toString() : responseBody;
        } catch (Exception e) {
            return responseBody == null || responseBody.isBlank() ? "Request failed" : responseBody;
        }
    }

    private record LoginPayload(String username, String password) {
    }
}
