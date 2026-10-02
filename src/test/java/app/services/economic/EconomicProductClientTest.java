package app.services.economic;

import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.dto.ProductDTO;
import app.exceptions.ApiException;
import app.services.routeSecurity.ObjectMapperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EconomicProductClientTest {
    private HttpServer server;
    private HttpExchange exchange;
    private String responseBody;
    private String updateResponseBody;
    private String requestBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws Exception {
        responseStatus = 200;
        responseBody = "{}";
        updateResponseBody = "{}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/products", request -> {
            exchange = request;
            String body = responseBody;
            if ("PUT".equals(request.getRequestMethod())) {
                requestBody = new String(request.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                body = updateResponseBody;
            }
            byte[] response = body.getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(responseStatus, response.length);
            request.getResponseBody().write(response);
            request.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void listsProductsWithPaginationAndAuthenticationHeaders() {
        responseBody = """
                {"collection":[{"productNumber":"FOOD-1","name":"Seasonal plate","salesPrice":125.00,
                "barred":false,"productGroup":{"productGroupNumber":2,"name":"Mains"}}]}
                """;

        EconomicProductListResponse response = client().listProducts(1000, 2);

        assertEquals("FOOD-1", response.getCollection().get(0).getProductNumber());
        assertEquals(2, response.getCollection().get(0).getProductGroup().getProductGroupNumber());
        assertEquals("/products?pagesize=1000&skippages=2", exchange.getRequestURI().toString());
        assertEquals("app-secret", exchange.getRequestHeaders().getFirst("X-AppSecretToken"));
        assertEquals("agreement-token", exchange.getRequestHeaders().getFirst("X-AgreementGrantToken"));
        assertEquals("application/json", exchange.getRequestHeaders().getFirst("Content-Type"));
        assertEquals("application/json", exchange.getRequestHeaders().getFirst("Accept"));
    }

    @Test
    void convertsUpstreamErrorsWithoutExposingCredentials() {
        responseStatus = 401;
        responseBody = "{\"message\":\"bad app-secret\",\"logId\":\"economic-log-1\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listProducts(1000, 0));

        assertEquals(401, exception.getCode());
        assertEquals("Invalid e-conomic credentials", exception.getMessage());
        assertFalse(exception.getMessage().contains("app-secret"));
        assertFalse(exception.getMessage().contains("agreement-token"));
    }

    @Test
    void requiresBothCredentialsBeforeSendingARequest() {
        EconomicProductClient client = new EconomicProductClient(
                HttpClient.newHttpClient(), baseUrl(), "", "agreement-token");

        ApiException exception = assertThrows(ApiException.class, () -> client.listProducts(1000, 0));

        assertEquals(500, exception.getCode());
        assertEquals("e-conomic credentials are not configured", exception.getMessage());
    }

    @Test
    void updatesProductWhilePreservingWritableEconomicFields() throws Exception {
        responseBody = """
                {"productNumber":"FOOD-1","name":"Old name","description":"Old description",
                "salesPrice":125.0,"costPrice":40.0,"barred":false,
                "productGroup":{"productGroupNumber":2,"name":"Mains","self":"%s/product-groups/2"},
                "unit":{"unitNumber":1,"name":"stk.","self":"%s/units/1"},
                "inventory":{"available":20,"packageVolume":2.5},
                "self":"%s/products/FOOD-1"}
                """.formatted(baseUrl(), baseUrl(), baseUrl());
        updateResponseBody = """
                {"productNumber":"FOOD-1","name":"New name","salesPrice":149.5,
                "productGroup":{"productGroupNumber":2,"name":"Mains"}}
                """;
        ProductDTO update = new ProductDTO();
        update.setName("New name");
        update.setDescription("");
        update.setPrice(149.5);

        EconomicProductResponse updated = client().updateProduct(baseUrl() + "/products/FOOD-1", update);

        JsonNode payload = ObjectMapperService.getMapper().readTree(requestBody);
        assertEquals("PUT", exchange.getRequestMethod());
        assertEquals("New name", updated.getName());
        assertEquals("New name", payload.get("name").asText());
        assertEquals(149.5, payload.get("salesPrice").asDouble());
        assertEquals(40.0, payload.get("costPrice").asDouble());
        assertEquals(2, payload.get("productGroup").get("productGroupNumber").asInt());
        assertEquals(1, payload.get("unit").get("unitNumber").asInt());
        assertEquals(2.5, payload.get("inventory").get("packageVolume").asDouble());
        assertFalse(payload.has("description"));
        assertFalse(payload.has("self"));
        assertFalse(payload.get("inventory").has("available"));
    }

    private EconomicProductClient client() {
        return new EconomicProductClient(
                HttpClient.newHttpClient(), baseUrl(), "app-secret", "agreement-token");
    }

    private String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }
}
