package app.services.economic;

import app.dto.EconomicApiErrorResponse;
import app.dto.EconomicCustomerListResponse;
import app.exceptions.ApiException;
import app.services.routeSecurity.ObjectMapperService;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EconomicCustomerClient {
    private static final Logger logger = LoggerFactory.getLogger(EconomicCustomerClient.class);
    private static final String DEFAULT_BASE_URL = "https://restapi.e-conomic.com";

    private final ObjectMapper objectMapper = ObjectMapperService.getMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String appSecretToken;
    private final String agreementGrantToken;

    public EconomicCustomerClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                getConfigValue("ECONOMIC_BASE_URL", DEFAULT_BASE_URL),
                getConfigValue("ECONOMIC_APP_SECRET_TOKEN", null),
                getConfigValue("ECONOMIC_AGREEMENT_GRANT_TOKEN", null));
    }

    public EconomicCustomerClient(HttpClient httpClient, String baseUrl, String appSecretToken,
                                  String agreementGrantToken) {
        this.httpClient = httpClient;
        String configuredBaseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = configuredBaseUrl.endsWith("/")
                ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1)
                : configuredBaseUrl;
        this.appSecretToken = appSecretToken;
        this.agreementGrantToken = agreementGrantToken;
    }

    public EconomicCustomerListResponse listCustomers(int pageSize, int skipPages) {
        requireCredentials();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/customers?pagesize=" + pageSize + "&skippages=" + skipPages))
                .timeout(Duration.ofSeconds(20))
                .header("X-AppSecretToken", appSecretToken)
                .header("X-AgreementGrantToken", agreementGrantToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request);
    }

    private EconomicCustomerListResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), EconomicCustomerListResponse.class);
            }
            throw convertError(response);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.warn("operation=listEconomicCustomers upstreamStatus=network economicLogId=unavailable", e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private ApiException convertError(HttpResponse<String> response) {
        EconomicApiErrorResponse error;
        try {
            error = objectMapper.readValue(response.body() == null || response.body().isBlank() ? "{}" : response.body(),
                    EconomicApiErrorResponse.class);
        } catch (IOException e) {
            error = new EconomicApiErrorResponse();
        }
        String logId = error.getLogId() == null ? "unavailable" : error.getLogId();
        logger.warn("operation=listEconomicCustomers upstreamStatus={} economicLogId={}", response.statusCode(), logId);
        int status = switch (response.statusCode()) {
            case 400 -> 400;
            case 401 -> 401;
            case 403 -> 403;
            case 429 -> 429;
            default -> 502;
        };
        String message = switch (response.statusCode()) {
            case 400 -> "e-conomic rejected the customer request";
            case 401 -> "Invalid e-conomic credentials";
            case 403 -> "Insufficient e-conomic permissions";
            case 429 -> "e-conomic rate limit exceeded";
            default -> "e-conomic request failed";
        };
        return new ApiException(status, message);
    }

    private void requireCredentials() {
        if (appSecretToken == null || appSecretToken.isBlank()
                || agreementGrantToken == null || agreementGrantToken.isBlank()) {
            throw new ApiException(500, "e-conomic credentials are not configured");
        }
    }

    private static String getConfigValue(String key, String defaultValue) {
        String propertyValue = System.getProperty(key);
        if (propertyValue != null && !propertyValue.isBlank()) return propertyValue.trim();
        String envValue = System.getenv(key);
        if (envValue != null && !envValue.isBlank()) return envValue.trim();
        try {
            return Utils.getPropertyValue(key, "config.properties");
        } catch (ApiException e) {
            return defaultValue;
        }
    }
}
