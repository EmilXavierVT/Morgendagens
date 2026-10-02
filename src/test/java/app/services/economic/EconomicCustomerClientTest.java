package app.services.economic;

import app.dto.EconomicCustomerListResponse;
import app.exceptions.ApiException;
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

class EconomicCustomerClientTest {
    private HttpServer server;
    private HttpExchange exchange;
    private String responseBody;
    private int responseStatus;

    @BeforeEach
    void setUp() throws Exception {
        responseStatus = 200;
        responseBody = "{}";
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/customers", request -> {
            exchange = request;
            byte[] response = responseBody.getBytes(StandardCharsets.UTF_8);
            request.sendResponseHeaders(responseStatus, response.length);
            request.getResponseBody().write(response);
            request.close();
        });
        server.start();
    }

    @AfterEach
    void tearDown() {
        if (server != null) server.stop(0);
    }

    @Test
    void listsCustomersWithoutADataStore() {
        responseBody = """
                {"collection":[{"customerNumber":42,"name":"Example ApS","email":"hello@example.dk",
                "customerGroup":{"customerGroupNumber":1,"name":"Customers"}}]}
                """;

        EconomicCustomerListResponse response = client().listCustomers(1000, 3);

        assertEquals(42, response.getCollection().get(0).getCustomerNumber());
        assertEquals("/customers?pagesize=1000&skippages=3", exchange.getRequestURI().toString());
        assertEquals("app-secret", exchange.getRequestHeaders().getFirst("X-AppSecretToken"));
        assertEquals("agreement-token", exchange.getRequestHeaders().getFirst("X-AgreementGrantToken"));
    }

    @Test
    void convertsCustomerErrorsWithoutExposingCredentials() {
        responseStatus = 403;
        responseBody = "{\"message\":\"bad agreement-token\",\"logId\":\"economic-log-2\"}";

        ApiException exception = assertThrows(ApiException.class, () -> client().listCustomers(1000, 0));

        assertEquals(403, exception.getCode());
        assertEquals("Insufficient e-conomic permissions", exception.getMessage());
        assertFalse(exception.getMessage().contains("agreement-token"));
    }

    private EconomicCustomerClient client() {
        return new EconomicCustomerClient(
                HttpClient.newHttpClient(),
                "http://localhost:" + server.getAddress().getPort(),
                "app-secret",
                "agreement-token");
    }
}
