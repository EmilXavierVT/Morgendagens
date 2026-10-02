package app.services.economic;

import app.dto.EconomicApiErrorResponse;
import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.dto.ProductDTO;
import app.exceptions.ApiException;
import app.services.routeSecurity.ObjectMapperService;
import app.utils.Utils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class EconomicProductClient {
    private static final Logger logger = LoggerFactory.getLogger(EconomicProductClient.class);
    private static final String DEFAULT_BASE_URL = "https://restapi.e-conomic.com";

    private final ObjectMapper objectMapper = ObjectMapperService.getMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String appSecretToken;
    private final String agreementGrantToken;

    public EconomicProductClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                getConfigValue("ECONOMIC_BASE_URL", DEFAULT_BASE_URL),
                getConfigValue("ECONOMIC_APP_SECRET_TOKEN", null),
                getConfigValue("ECONOMIC_AGREEMENT_GRANT_TOKEN", null));
    }

    public EconomicProductClient(HttpClient httpClient, String baseUrl, String appSecretToken,
                                 String agreementGrantToken) {
        this.httpClient = httpClient;
        String configuredBaseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = stripTrailingSlash(configuredBaseUrl);
        this.appSecretToken = appSecretToken;
        this.agreementGrantToken = agreementGrantToken;
    }

    public EconomicProductListResponse listProducts(int pageSize, int skipPages) {
        requireCredentials();
        HttpRequest request = HttpRequest.newBuilder(
                        URI.create(baseUrl + "/products?pagesize=" + pageSize + "&skippages=" + skipPages))
                .timeout(Duration.ofSeconds(20))
                .header("X-AppSecretToken", appSecretToken)
                .header("X-AgreementGrantToken", agreementGrantToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .GET()
                .build();
        return send(request);
    }

    public EconomicProductResponse updateProduct(String selfUrl, ProductDTO update) {
        requireCredentials();
        if (update == null || update.getName() == null || update.getName().isBlank()) {
            throw new ApiException(400, "Product name is required");
        }
        if (!Double.isFinite(update.getPrice()) || update.getPrice() < 0) {
            throw new ApiException(400, "Product price must be zero or greater");
        }

        URI productUri = requireProductUri(selfUrl);
        ObjectNode current = getProductDocument(productUri);
        ObjectNode payload = writableProductDocument(current);
        payload.put("name", update.getName().trim());
        payload.put("salesPrice", update.getPrice());
        if (update.getDescription() == null || update.getDescription().isBlank()) {
            payload.remove("description");
        } else {
            payload.put("description", update.getDescription().trim());
        }

        try {
            HttpRequest request = authenticatedRequest(productUri)
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), EconomicProductResponse.class);
            }
            throw convertError(response, "updateEconomicProduct");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.warn("operation=updateEconomicProduct upstreamStatus=network economicLogId=unavailable", e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private ObjectNode getProductDocument(URI productUri) {
        try {
            HttpRequest request = authenticatedRequest(productUri).GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode document = objectMapper.readTree(response.body());
                if (document instanceof ObjectNode objectNode) return objectNode;
                throw new ApiException(502, "e-conomic returned an invalid product");
            }
            throw convertError(response, "getEconomicProduct");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.warn("operation=getEconomicProduct upstreamStatus=network economicLogId=unavailable", e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private ObjectNode writableProductDocument(ObjectNode current) {
        ObjectNode payload = objectMapper.createObjectNode();
        copyFields(current, payload, "barCode", "barred", "costPrice", "description", "name",
                "productNumber", "recommendedPrice", "salesPrice");
        copyReference(current, payload, "productGroup", "productGroupNumber");
        copyReference(current, payload, "unit", "unitNumber");
        copyReference(current, payload, "departmentalDistribution",
                "departmentalDistributionNumber", "distributionType");

        JsonNode inventory = current.get("inventory");
        if (inventory instanceof ObjectNode inventoryObject) {
            ObjectNode writableInventory = objectMapper.createObjectNode();
            copyFields(inventoryObject, writableInventory, "packageVolume", "recommendedCostPrice");
            if (!writableInventory.isEmpty()) payload.set("inventory", writableInventory);
        }
        return payload;
    }

    private void copyFields(ObjectNode source, ObjectNode target, String... fields) {
        for (String field : fields) {
            if (source.has(field) && !source.get(field).isNull()) {
                target.set(field, source.get(field).deepCopy());
            }
        }
    }

    private void copyReference(ObjectNode source, ObjectNode target, String field, String... writableFields) {
        JsonNode reference = source.get(field);
        if (!(reference instanceof ObjectNode referenceObject)) return;
        ObjectNode writableReference = objectMapper.createObjectNode();
        copyFields(referenceObject, writableReference, writableFields);
        copyFields(referenceObject, writableReference, "self");
        if (!writableReference.isEmpty()) target.set(field, writableReference);
    }

    private HttpRequest.Builder authenticatedRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .header("X-AppSecretToken", appSecretToken)
                .header("X-AgreementGrantToken", agreementGrantToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private URI requireProductUri(String selfUrl) {
        if (selfUrl == null || selfUrl.isBlank()) {
            throw new ApiException(502, "e-conomic product link is missing");
        }
        URI uri = URI.create(selfUrl);
        URI configuredBase = URI.create(baseUrl);
        boolean sameOrigin = configuredBase.getScheme().equalsIgnoreCase(uri.getScheme())
                && configuredBase.getAuthority().equalsIgnoreCase(uri.getAuthority());
        if (!sameOrigin || uri.getPath() == null || !uri.getPath().startsWith("/products/")) {
            throw new ApiException(502, "e-conomic product link is invalid");
        }
        return uri;
    }

    private EconomicProductListResponse send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return objectMapper.readValue(response.body(), EconomicProductListResponse.class);
            }
            throw convertError(response, "listEconomicProducts");
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            logger.warn("operation=listEconomicProducts upstreamStatus=network economicLogId=unavailable", e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private ApiException convertError(HttpResponse<String> response, String operation) {
        EconomicApiErrorResponse error = parseError(response.body());
        String logId = error.getLogId() == null ? "unavailable" : error.getLogId();
        logger.warn("operation={} upstreamStatus={} economicLogId={}", operation, response.statusCode(), logId);
        return new ApiException(toApplicationStatus(response.statusCode()), safeMessage(response.statusCode()));
    }

    private EconomicApiErrorResponse parseError(String body) {
        try {
            return objectMapper.readValue(body == null || body.isBlank() ? "{}" : body,
                    EconomicApiErrorResponse.class);
        } catch (IOException e) {
            return new EconomicApiErrorResponse();
        }
    }

    private int toApplicationStatus(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 400 -> 400;
            case 401 -> 401;
            case 403 -> 403;
            case 429 -> 429;
            default -> 502;
        };
    }

    private String safeMessage(int upstreamStatus) {
        return switch (upstreamStatus) {
            case 400 -> "e-conomic rejected the product request";
            case 401 -> "Invalid e-conomic credentials";
            case 403 -> "Insufficient e-conomic permissions";
            case 429 -> "e-conomic rate limit exceeded";
            default -> "e-conomic request failed";
        };
    }

    private void requireCredentials() {
        if (appSecretToken == null || appSecretToken.isBlank()
                || agreementGrantToken == null || agreementGrantToken.isBlank()) {
            throw new ApiException(500, "e-conomic credentials are not configured");
        }
    }

    private static String stripTrailingSlash(String value) {
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
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
