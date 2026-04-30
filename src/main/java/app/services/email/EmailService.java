package app.services.email;

import app.dto.EmailRequestDTO;
import app.exceptions.ApiException;
import app.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

public class EmailService {
    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String DEFAULT_RESEND_URL = "https://api.resend.com/emails";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public EmailService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public void send(EmailRequestDTO request) {
        validate(request);

        String apiKey = requireConfig("RESEND_API_KEY");
        String from = requireConfig("EMAIL_FROM");
        String apiUrl = configOrDefault("RESEND_API_URL", DEFAULT_RESEND_URL);
        String payload = createPayload(request, from);

        HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        try {
            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                logger.warn("Email provider rejected request with status {}: {}", response.statusCode(), response.body());
                throw new ApiException(502, "Email provider rejected the request with status " + response.statusCode());
            }
        } catch (IOException e) {
            throw new ApiException(502, "Email could not be sent");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Email request was interrupted");
        }
    }
    

    private String createPayload(EmailRequestDTO request, String from) {
        ObjectNode root = objectMapper.createObjectNode();

        root.put("from", from);
        root.putArray("to").add(request.getTo());
        root.put("subject", request.getSubject());
        if (Boolean.TRUE.equals(request.getHtml())) {
            root.put("html", request.getBody());
        } else {
            root.put("text", request.getBody());
        }

        String replyTo = configOrDefault("EMAIL_REPLY_TO", null);
        if (replyTo != null && !replyTo.isBlank()) {
            root.put("reply_to", replyTo);
        }

        try {
            return objectMapper.writeValueAsString(root);
        } catch (JsonProcessingException e) {
            throw new ApiException(500, "Email payload could not be created");
        }
    }

    private void validate(EmailRequestDTO request) {
        if (request == null) {
            throw new ApiException(400, "Request body is required");
        }
        if (request.getTo() == null || !EMAIL_PATTERN.matcher(request.getTo()).matches()) {
            throw new ApiException(400, "A valid recipient email is required");
        }
        if (request.getSubject() == null || request.getSubject().isBlank()) {
            throw new ApiException(400, "Subject is required");
        }
        if (request.getBody() == null || request.getBody().isBlank()) {
            throw new ApiException(400, "Body is required");
        }
    }

    private String requireConfig(String key) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = getLocalProperty(key);
        }
        value = normalizeConfigValue(value);
        if (value == null || value.isBlank()) {
            throw new ApiException(500, key + " must be configured");
        }
        return value;
    }

    private String configOrDefault(String key, String fallback) {
        String value = System.getenv(key);
        if (value == null || value.isBlank()) {
            value = getLocalProperty(key);
        }
        value = normalizeConfigValue(value);
        return value == null || value.isBlank() ? fallback : value;
    }

    private String getLocalProperty(String key) {
        if (System.getenv("DEPLOYED") != null) {
            return null;
        }
        try {
            return Utils.getPropertyValue(key, "config.properties");
        } catch (ApiException e) {
            return null;
        }
    }

    private String normalizeConfigValue(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }
}
