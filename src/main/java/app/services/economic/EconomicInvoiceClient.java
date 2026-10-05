package app.services.economic;

import app.exceptions.ApiException;
import app.services.routeSecurity.ObjectMapperService;
import app.utils.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

public class EconomicInvoiceClient {
    private static final Logger logger = LoggerFactory.getLogger(EconomicInvoiceClient.class);
    private static final String DEFAULT_BASE_URL = "https://restapi.e-conomic.com";

    private final ObjectMapper objectMapper = ObjectMapperService.getMapper();
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String appSecretToken;
    private final String agreementGrantToken;

    public record DraftInvoiceResult(Integer draftInvoiceNumber, String self) {}
    public record BookedInvoiceResult(Integer bookedInvoiceNumber, String self) {}
    public record InvoiceLine(String productNumber, String description, BigDecimal quantity, BigDecimal unitNetPrice,
                              BigDecimal discountPercentage) {
        public InvoiceLine(String productNumber, String description, BigDecimal quantity, BigDecimal unitNetPrice) {
            this(productNumber, description, quantity, unitNetPrice, BigDecimal.ZERO);
        }

        public InvoiceLine {
            if (discountPercentage == null) discountPercentage = BigDecimal.ZERO;
        }
    }

    public EconomicInvoiceClient() {
        this(HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build(),
                getConfigValue("ECONOMIC_BASE_URL", DEFAULT_BASE_URL),
                getConfigValue("ECONOMIC_APP_SECRET_TOKEN", null),
                getConfigValue("ECONOMIC_AGREEMENT_GRANT_TOKEN", null));
    }

    public EconomicInvoiceClient(HttpClient httpClient, String baseUrl, String appSecretToken,
                                 String agreementGrantToken) {
        this.httpClient = httpClient;
        String configuredBaseUrl = baseUrl == null || baseUrl.isBlank() ? DEFAULT_BASE_URL : baseUrl.trim();
        this.baseUrl = configuredBaseUrl.endsWith("/")
                ? configuredBaseUrl.substring(0, configuredBaseUrl.length() - 1)
                : configuredBaseUrl;
        this.appSecretToken = appSecretToken;
        this.agreementGrantToken = agreementGrantToken;
    }

    public DraftInvoiceResult createCleaningDraftInvoice(int customerNumber, String productNumber,
                                                         List<InvoiceLine> serviceLines,
                                                         LocalDate invoiceDate, String heading,
                                                         String description) {
        return createDraftInvoice(customerNumber, serviceLines.stream()
                        .map(line -> new InvoiceLine(productNumber, line.description(), line.quantity(), line.unitNetPrice()))
                        .toList(),
                invoiceDate, heading, description);
    }

    public DraftInvoiceResult createDraftInvoice(int customerNumber, List<InvoiceLine> serviceLines,
                                                 LocalDate invoiceDate, String heading,
                                                 String description) {
        requireCredentials();

        ObjectNode invoice = getObject("/customers/" + customerNumber + "/templates/invoice", "getEconomicInvoiceTemplate");
        invoice.put("date", invoiceDate.toString());
        invoice.put("currency", invoice.path("currency").asText("DKK"));

        ObjectNode notes = objectMapper.createObjectNode();
        notes.put("heading", heading);
        notes.put("textLine1", description);
        invoice.set("notes", notes);

        ArrayNode lines = objectMapper.createArrayNode();
        int sortKey = 1;
        for (InvoiceLine serviceLine : serviceLines) {
            ObjectNode line = getObject("/customers/" + customerNumber + "/templates/invoiceline/"
                    + encodePath(serviceLine.productNumber()) + "?quantity=" + serviceLine.quantity(), "getEconomicInvoiceLineTemplate");
            line.put("quantity", serviceLine.quantity());
            line.put("unitNetPrice", serviceLine.unitNetPrice());
            line.put("discountPercentage", serviceLine.discountPercentage());
            line.put("sortKey", sortKey++);
            line.put("description", serviceLine.description());
            lines.add(line);
        }
        invoice.set("lines", lines);

        ObjectNode created = postObject("/invoices/drafts", invoice, "createEconomicDraftInvoice");
        return new DraftInvoiceResult(
                created.path("draftInvoiceNumber").isMissingNode() ? null : created.path("draftInvoiceNumber").asInt(),
                created.path("self").asText(null)
        );
    }

    public BookedInvoiceResult bookDraftInvoice(int draftInvoiceNumber, boolean sendByEmail) {
        requireCredentials();

        ObjectNode payload = objectMapper.createObjectNode();
        ObjectNode draftInvoice = objectMapper.createObjectNode();
        draftInvoice.put("draftInvoiceNumber", draftInvoiceNumber);
        payload.set("draftInvoice", draftInvoice);
        payload.put("sendBy", sendByEmail ? "Email" : "none");

        ObjectNode booked = postObject("/invoices/booked", payload, "bookEconomicInvoice");
        return new BookedInvoiceResult(
                booked.path("bookedInvoiceNumber").isMissingNode() ? null : booked.path("bookedInvoiceNumber").asInt(),
                booked.path("self").asText(null)
        );
    }

    private ObjectNode getObject(String path, String operation) {
        HttpRequest request = authenticatedRequest(path).GET().build();
        return sendObject(request, operation);
    }

    private ObjectNode postObject(String path, ObjectNode payload, String operation) {
        try {
            HttpRequest request = authenticatedRequest(path)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                    .build();
            return sendObject(request, operation);
        } catch (IOException e) {
            throw new ApiException(500, "Could not prepare e-conomic invoice request");
        }
    }

    private ObjectNode sendObject(HttpRequest request, String operation) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                JsonNode document = objectMapper.readTree(response.body());
                if (document instanceof ObjectNode objectNode) return objectNode;
                throw new ApiException(502, "e-conomic returned an invalid invoice response");
            }
            throw convertError(response, operation);
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            logger.warn("operation={} upstreamStatus=network economicLogId=unavailable", operation, e);
            throw new ApiException(502, "Could not connect to e-conomic");
        }
    }

    private HttpRequest.Builder authenticatedRequest(String path) {
        return HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("X-AppSecretToken", appSecretToken)
                .header("X-AgreementGrantToken", agreementGrantToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json");
    }

    private ApiException convertError(HttpResponse<String> response, String operation) {
        String logId = "unavailable";
        try {
            JsonNode error = objectMapper.readTree(response.body() == null || response.body().isBlank() ? "{}" : response.body());
            logId = error.path("logId").asText("unavailable");
        } catch (IOException ignored) {
            // The public response stays generic, but logs keep the e-conomic status.
        }
        logger.warn("operation={} upstreamStatus={} economicLogId={}", operation, response.statusCode(), logId);
        int status = switch (response.statusCode()) {
            case 400 -> 400;
            case 401 -> 401;
            case 403 -> 403;
            case 429 -> 429;
            default -> 502;
        };
        String message = switch (response.statusCode()) {
            case 400 -> "e-conomic rejected the invoice request";
            case 401 -> "Invalid e-conomic credentials";
            case 403 -> "Insufficient e-conomic invoice permissions";
            case 429 -> "e-conomic rate limit exceeded";
            default -> "e-conomic invoice request failed";
        };
        return new ApiException(status, message);
    }

    private void requireCredentials() {
        if (appSecretToken == null || appSecretToken.isBlank()
                || agreementGrantToken == null || agreementGrantToken.isBlank()) {
            throw new ApiException(500, "e-conomic credentials are not configured");
        }
    }

    private String encodePath(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
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
