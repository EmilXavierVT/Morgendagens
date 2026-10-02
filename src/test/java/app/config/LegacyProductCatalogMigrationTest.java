package app.config;

import app.entities.Product;
import app.entities.ProductInRequest;
import app.entities.Request;
import app.entities.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@Testcontainers
class LegacyProductCatalogMigrationTest {
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
    void preservesBookingLineAsSnapshotWithoutDeletingLocalCatalog() {
        Long lineId;

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Tenant tenant = Tenant.builder().name("migration").type("default").status(1).build();
            em.persist(tenant);

            Request request = Request.builder()
                    .tenant(tenant)
                    .type(1)
                    .status(1)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusHours(1))
                    .location("Copenhagen")
                    .build();
            em.persist(request);

            Product product = Product.builder()
                    .name("Legacy lunch")
                    .description("Stored before live catalog migration")
                    .price(125.50)
                    .type(2)
                    .economicProductNumber("0101")
                    .build();
            em.persist(product);

            ProductInRequest line = ProductInRequest.builder()
                    .request(request)
                    .product(product)
                    .amount(4)
                    .build();
            em.persist(line);
            em.getTransaction().commit();
            lineId = line.getId();
        }

        LegacyProductCatalogMigration.migrate(emf);

        try (EntityManager em = emf.createEntityManager()) {
            ProductInRequest migrated = em.find(ProductInRequest.class, lineId);
            assertNull(migrated.getProduct());
            assertEquals("0101", migrated.getProductNumber());
            assertEquals("Legacy lunch", migrated.getProductName());
            assertEquals("Stored before live catalog migration", migrated.getProductDescription());
            assertEquals(0, migrated.getUnitPrice().compareTo(new BigDecimal("125.50")));
            assertEquals(1L, em.createQuery("select count(p) from Product p", Long.class).getSingleResult());
        }
    }
}
