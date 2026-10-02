package app.config;

import app.dto.EconomicCustomerListResponse;
import app.dto.EconomicCustomerResponse;
import app.dto.EconomicProductListResponse;
import app.dto.EconomicProductResponse;
import app.entities.EconomicCustomer;
import app.entities.Product;
import app.services.economic.EconomicCustomerClient;
import app.services.economic.EconomicMirrorSyncService;
import app.services.economic.EconomicProductClient;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
class EconomicMirrorSyncServiceTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    @BeforeAll
    static void setup() {
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);
    }

    @AfterAll
    static void teardown() {
        if (emf != null) emf.close();
    }

    @Test
    void syncCreatesThenUpdatesProductsAndCustomersByEconomicNumber() {
        StubProductClient productClient = new StubProductClient();
        StubCustomerClient customerClient = new StubCustomerClient();
        EconomicMirrorSyncService service = new EconomicMirrorSyncService(emf, productClient, customerClient);

        service.syncProducts();
        service.syncCustomers();

        assertEquals(1L, count(Product.class));
        assertEquals(1L, count(EconomicCustomer.class));
        assertEquals("Lunch 2026", firstProduct().getName());
        assertEquals("Ungdomsoen", firstCustomer().getName());

        EconomicMirrorSyncService.SyncResult unchangedProducts = service.syncProducts();
        EconomicMirrorSyncService.SyncResult unchangedCustomers = service.syncCustomers();

        assertEquals(1L, count(Product.class));
        assertEquals(1L, count(EconomicCustomer.class));
        assertEquals(0, unchangedProducts.getCreated());
        assertEquals(0, unchangedProducts.getUpdated());
        assertEquals(1, unchangedProducts.getUnchanged());
        assertEquals(0, unchangedCustomers.getCreated());
        assertEquals(0, unchangedCustomers.getUpdated());
        assertEquals(1, unchangedCustomers.getUnchanged());
        productClient.name = "Updated lunch 2026";
        productClient.salesPrice = BigDecimal.valueOf(175);
        customerClient.name = "Ungdomsoen updated";

        service.syncProducts();
        service.syncCustomers();

        assertEquals(1L, count(Product.class));
        assertEquals(1L, count(EconomicCustomer.class));
        assertEquals("Updated lunch 2026", firstProduct().getName());
        assertEquals(175.0, firstProduct().getPrice());
        assertEquals("Ungdomsoen updated", firstCustomer().getName());
    }

    private long count(Class<?> entityClass) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select count(entity) from " + entityClass.getSimpleName() + " entity", Long.class)
                    .getSingleResult();
        }
    }

    private Product firstProduct() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select product from Product product", Product.class)
                    .setMaxResults(1)
                    .getSingleResult();
        }
    }

    private EconomicCustomer firstCustomer() {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("select customer from EconomicCustomer customer", EconomicCustomer.class)
                    .setMaxResults(1)
                    .getSingleResult();
        }
    }

    private static class StubProductClient extends EconomicProductClient {
        private String name = "Lunch 2026";
        private BigDecimal salesPrice = BigDecimal.valueOf(125);

        StubProductClient() {
            super(HttpClient.newHttpClient(), "http://localhost", "app-secret", "agreement-token");
        }

        @Override
        public EconomicProductListResponse listProducts(int pageSize, int skipPages) {
            EconomicProductListResponse response = new EconomicProductListResponse();
            if (skipPages > 0) return response;

            EconomicProductResponse product = new EconomicProductResponse();
            product.setProductNumber("YI-2026-LUNCH");
            product.setName(name);
            product.setDescription("Youth Island lunch");
            product.setSalesPrice(salesPrice);
            product.setBarred(false);
            product.setLastUpdated(OffsetDateTime.parse("2026-05-01T10:15:30+02:00"));
            product.setSelf("http://localhost/products/YI-2026-LUNCH");
            response.setCollection(List.of(product));
            return response;
        }
    }

    private static class StubCustomerClient extends EconomicCustomerClient {
        private String name = "Ungdomsoen";

        StubCustomerClient() {
            super(HttpClient.newHttpClient(), "http://localhost", "app-secret", "agreement-token");
        }

        @Override
        public EconomicCustomerListResponse listCustomers(int pageSize, int skipPages) {
            EconomicCustomerListResponse response = new EconomicCustomerListResponse();
            if (skipPages > 0) return response;

            EconomicCustomerResponse customer = new EconomicCustomerResponse();
            customer.setCustomerNumber(42);
            customer.setName(name);
            customer.setEmail("bookings@example.com");
            customer.setBarred(false);
            customer.setLastUpdated(OffsetDateTime.parse("2026-05-01T10:15:30+02:00"));
            customer.setSelf("http://localhost/customers/42");
            response.setCollection(List.of(customer));
            return response;
        }
    }
}
