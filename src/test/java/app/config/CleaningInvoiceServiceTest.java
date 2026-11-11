package app.config;

import app.dto.CleaningInvoiceLineDTO;
import app.dto.CleaningInvoicePreviewDTO;
import app.dto.CleaningInvoicePreviewDTO.BatchResult;
import app.entities.CleaningAppointment;
import app.entities.CleaningProject;
import app.entities.EconomicCustomer;
import app.entities.Product;
import app.exceptions.ApiException;
import app.services.economic.CleaningInvoiceService;
import app.services.economic.EconomicInvoiceClient;
import app.services.routeSecurity.ObjectMapperService;
import com.fasterxml.jackson.databind.JsonNode;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class CleaningInvoiceServiceTest {
    private static final String MONTH = "2026-10";
    private static final String PRODUCT_NUMBER = "1091";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb").withUsername("test").withPassword("test");
    private static EntityManagerFactory emf;

    private final List<String> requests = new CopyOnWriteArrayList<>();
    private final List<Integer> draftCustomers = new CopyOnWriteArrayList<>();
    private final List<JsonNode> bookingRequests = new CopyOnWriteArrayList<>();
    private final Map<Integer, StubResponse> draftResponses = new ConcurrentHashMap<>();
    private final Map<Integer, StubResponse> bookingResponses = new ConcurrentHashMap<>();
    private HttpServer economic;

    @BeforeAll
    static void setupDatabase() {
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);
    }

    @AfterAll
    static void closeDatabase() {
        if (emf != null) emf.close();
    }

    @BeforeEach
    void setUp() throws IOException {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.createQuery("delete from CleaningInvoiceRecord").executeUpdate();
            em.createQuery("delete from CleaningAppointment").executeUpdate();
            em.createQuery("delete from CleaningProject").executeUpdate();
            em.createQuery("delete from EconomicCustomer").executeUpdate();
            em.createQuery("delete from Product").executeUpdate();
            em.persist(Product.builder().name("Rengøring").price(280)
                    .economicProductNumber(PRODUCT_NUMBER).economicAvailable(true).build());
            em.getTransaction().commit();
        }
        economic = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        economic.createContext("/customers", exchange -> {
            String path = exchange.getRequestURI().getPath();
            if (path.endsWith("/templates/invoice")) {
                int customer = Integer.parseInt(path.split("/")[2]);
                respond(exchange, new StubResponse(200, "{\"customer\":{\"customerNumber\":" + customer + "}}"));
            } else {
                respond(exchange, new StubResponse(200, "{}"));
            }
        });
        economic.createContext("/invoices/drafts", exchange -> {
            JsonNode invoice = ObjectMapperService.getMapper().readTree(exchange.getRequestBody());
            int customer = invoice.path("customer").path("customerNumber").intValue();
            draftCustomers.add(customer);
            respond(exchange, draftResponses.getOrDefault(customer,
                    new StubResponse(201, "{\"draftInvoiceNumber\":" + (1000 + customer) + "}")));
        });
        economic.createContext("/invoices/booked", exchange -> {
            JsonNode invoice = ObjectMapperService.getMapper().readTree(exchange.getRequestBody());
            bookingRequests.add(invoice);
            int draft = invoice.path("draftInvoice").path("draftInvoiceNumber").intValue();
            respond(exchange, bookingResponses.getOrDefault(draft,
                    new StubResponse(201, "{\"bookedInvoiceNumber\":" + (1000 + draft) + "}")));
        });
        economic.start();
    }

    @AfterEach
    void stopEconomicStub() {
        if (economic != null) economic.stop(0);
    }

    @Test
    void missingEitherCredentialFailsTheBatchBeforeCallingEconomic() {
        addCustomers(1);
        for (CleaningInvoiceService service : List.of(service(null, "grant"), service("secret", ""))) {
            ApiException failure = assertThrows(ApiException.class,
                    () -> service.createInvoices(MONTH, PRODUCT_NUMBER, true, false));
            assertEquals(500, failure.getCode());
            assertEquals("e-conomic credentials are not configured", failure.getMessage());
            CleaningInvoicePreviewDTO preview = service.preview(MONTH, PRODUCT_NUMBER);
            assertFalse(line(preview, 1).isAlreadyInvoiced());
            assertFalse(ObjectMapperService.getMapper().valueToTree(preview).has("batch"));
        }
        assertTrue(requests.isEmpty());
    }

    @Test
    void reportsPartialFailureAndSkipsAnExistingInvoice() {
        CleaningInvoiceService service = service("secret", "grant");
        addCustomers(3);
        service.createInvoices(MONTH, PRODUCT_NUMBER, false, false);
        draftCustomers.clear();
        addCustomers(1, 2);
        draftResponses.put(2, new StubResponse(400, "{\"logId\":\"test-rejection\"}"));

        CleaningInvoicePreviewDTO result = service.createInvoices(MONTH, PRODUCT_NUMBER, true, false);

        assertEquals(new BatchResult(1, 1, 1), result.getBatch());
        assertEquals(List.of(1, 2), draftCustomers);
        assertEquals(2001, line(result, 1).getBookedInvoiceNumber());
        assertEquals("e-conomic rejected the invoice request", line(result, 2).getError());
        assertFalse(line(result, 2).isAlreadyInvoiced());
        assertEquals(1003, line(result, 3).getDraftInvoiceNumber());
        JsonNode response = ObjectMapperService.getMapper().valueToTree(result);
        assertEquals(1, response.path("batch").path("failed").intValue());
        assertTrue(response.path("lines").get(1).hasNonNull("error"));
    }

    @Test
    void reportsSuccessfulDraftsAndMakesNoProviderCallsWhenAllAreSkipped() {
        CleaningInvoiceService service = service("secret", "grant");
        addCustomers(1);

        CleaningInvoicePreviewDTO drafts = service.createInvoices(MONTH, PRODUCT_NUMBER, false, false);

        assertEquals(new BatchResult(1, 0, 0), drafts.getBatch());
        assertEquals("DRAFT", line(drafts, 1).getStatus());
        assertEquals(1001, line(drafts, 1).getDraftInvoiceNumber());
        assertTrue(bookingRequests.isEmpty());
        requests.clear();

        assertEquals(new BatchResult(0, 0, 1), service.createInvoices(MONTH, PRODUCT_NUMBER, true, false).getBatch());
        assertTrue(requests.isEmpty());
    }

    @Test
    void countsTheWholeBatchWhenEconomicReturnsBookedNumbers() {
        CleaningInvoiceService service = service("secret", "grant");
        addCustomers(1, 2);

        CleaningInvoicePreviewDTO result = service.createInvoices(MONTH, PRODUCT_NUMBER, true, false);

        assertEquals(new BatchResult(2, 0, 0), result.getBatch());
        assertEquals(List.of(1001, 1002), bookingRequests.stream()
                .map(request -> request.path("draftInvoice").path("draftInvoiceNumber").intValue()).toList());
        assertTrue(bookingRequests.stream().allMatch(request -> "Email".equals(request.path("sendBy").asText())));
        assertEquals("SENT", line(result, 1).getStatus());
        assertEquals(2002, line(service.preview(MONTH, PRODUCT_NUMBER), 2).getBookedInvoiceNumber());
    }

    @Test
    void rejectsMissingInvoiceNumbersAndKeepsTheConfirmedDraftAfterBookingFails() {
        CleaningInvoiceService service = service("secret", "grant");
        addCustomers(1, 2);
        draftResponses.put(1, new StubResponse(201, "{}"));
        bookingResponses.put(1002, new StubResponse(201, "{\"bookedInvoiceNumber\":0}"));

        CleaningInvoicePreviewDTO result = service.createInvoices(MONTH, PRODUCT_NUMBER, true, false);

        assertEquals(new BatchResult(0, 2, 0), result.getBatch());
        assertTrue(line(result, 1).getError().contains("draft invoice number"));
        assertFalse(line(result, 1).isAlreadyInvoiced());
        assertEquals(1, bookingRequests.size());
        assertTrue(line(result, 2).getError().contains("Check draft #1002"));
        CleaningInvoiceLineDTO saved = line(service.preview(MONTH, PRODUCT_NUMBER), 2);
        assertEquals("DRAFT", saved.getStatus());
        assertEquals(1002, saved.getDraftInvoiceNumber());
        assertNull(saved.getBookedInvoiceNumber());
    }

    @Test
    void forcedBookingFailurePreservesTheExistingBookedInvoice() {
        CleaningInvoiceService service = service("secret", "grant");
        addCustomers(1);
        service.createInvoices(MONTH, PRODUCT_NUMBER, true, false);
        draftResponses.put(1, new StubResponse(201, "{\"draftInvoiceNumber\":3001}"));
        bookingResponses.put(3001, new StubResponse(403, "{}"));

        CleaningInvoicePreviewDTO result = service.createInvoices(MONTH, PRODUCT_NUMBER, true, true);

        assertEquals(new BatchResult(0, 1, 0), result.getBatch());
        assertTrue(line(result, 1).getError().contains("Check draft #3001"));
        assertEquals(2001, line(service.preview(MONTH, PRODUCT_NUMBER), 1).getBookedInvoiceNumber());
    }

    private CleaningInvoiceService service(String secret, String grant) {
        EconomicInvoiceClient client = new EconomicInvoiceClient(HttpClient.newHttpClient(),
                "http://127.0.0.1:" + economic.getAddress().getPort(), secret, grant);
        return new CleaningInvoiceService(emf, client);
    }

    private void addCustomers(int... customerNumbers) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            for (int number : customerNumbers) {
                EconomicCustomer customer = EconomicCustomer.builder()
                        .customerNumber(number).name("Customer " + number).build();
                em.persist(customer);
                CleaningProject project = CleaningProject.builder()
                        .name("Project " + number).economicCustomer(customer).active(true).build();
                em.persist(project);
                em.persist(CleaningAppointment.builder().project(project)
                        .appointmentTime(LocalDateTime.of(2026, 10, 5, 9, 0)).durationMinutes(120).build());
            }
            em.getTransaction().commit();
        }
    }

    private static CleaningInvoiceLineDTO line(CleaningInvoicePreviewDTO response, int customer) {
        return response.getLines().stream().filter(line -> line.getCustomerNumber() == customer)
                .findFirst().orElseThrow();
    }

    private void respond(HttpExchange exchange, StubResponse response) throws IOException {
        requests.add(exchange.getRequestURI().getPath());
        byte[] body = response.body().getBytes(StandardCharsets.UTF_8);
        try (exchange) {
            exchange.sendResponseHeaders(response.status(), body.length);
            exchange.getResponseBody().write(body);
        }
    }

    private record StubResponse(int status, String body) {}
}
