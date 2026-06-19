package app.config;

import app.entities.*;
import app.services.entityServices.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class ServiceCrudIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;

    private static UserService userService;
    private static TenantService tenantService;
    private static ProductService productService;
    private static RequestService requestService;
    private static MessageService messageService;
    private static ProductInRequestService productInRequestService;
    private static SubscriptionDealService subscriptionDealService;

    @BeforeAll
    static void setup() {
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);

        userService = new UserService(emf);
        tenantService = new TenantService(emf);
        productService = new ProductService(emf);
        requestService = new RequestService(emf);
        messageService = new MessageService(emf);
        productInRequestService = new ProductInRequestService(emf);
        subscriptionDealService = new SubscriptionDealService(emf);
    }

    @AfterAll
    static void teardown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void crud_all_entities() {
        Tenant tenant = Tenant.builder()
                .name("tenant-a")
                .type("default")
                .status(1)
                .build();
        tenantService.create(tenant);
        assertNotNull(tenant.getId());

        User user = new User("a@b.com", "pw");
        user.setFirstName("first");
        user.setLastName("last");
        user.setPhoneNumber("123");
        user.setZipCode(9999);
        user.setTenant(tenant);
        userService.create(user);
        userService.setCleaningClient(user.getId());
        userService.setSubscriber(user.getId());
        user = userService.getById(user.getId());
        assertNotNull(user.getId());

        SubscriptionDeal subscriptionDeal = SubscriptionDeal.builder()
                .user(user)
                .visitsPerMonth(4)
                .build();
        subscriptionDealService.create(subscriptionDeal);
        assertNotNull(subscriptionDeal.getId());

        Product product = Product.builder()
                .name("Coffee")
                .description("Fresh brewed coffee")
                .price(25.0)
                .type(1)
                .build();
        productService.create(product);
        assertNotNull(product.getId());

        Request request = Request.builder()
                .tenant(tenant)
                .type(1)
                .status(1)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .location("Aarhus")
                .build();
        requestService.create(request);
        assertNotNull(request.getId());

        ProductInRequest productInRequest = ProductInRequest.builder()
                .request(request)
                .product(product)
                .time(Time.valueOf(LocalTime.of(9, 0)))
                .build();
        productInRequestService.create(productInRequest);
        assertNotNull(productInRequest.getId());

        Message message = Message.builder()
                .user(user)
                .thread(1)
                .context("Welcome message")
                .date(LocalDateTime.now())
                .build();
        messageService.create(message);
        assertNotNull(message.getId());

        assertNotNull(userService.getById(user.getId()));
        assertNotNull(tenantService.getById(tenant.getId()));
        assertNotNull(productService.getById(product.getId()));
        assertNotNull(requestService.getById(request.getId()));
        assertNotNull(productInRequestService.getById(productInRequest.getId()));
        assertNotNull(messageService.getById(message.getId()));
        assertNotNull(subscriptionDealService.getById(subscriptionDeal.getId()));

        tenant.setName("tenant-updated");
        tenantService.update(tenant);
        assertEquals("tenant-updated", tenantService.getById(tenant.getId()).getName());

        // Re-fetch user before update so its messages list is populated —
        // otherwise orphanRemoval would delete the message row.
        User freshUser = userService.getById(user.getId());
        freshUser.setFirstName("updated");
        userService.update(freshUser);
        assertEquals("updated", userService.getById(user.getId()).getFirstName());

        product.setPrice(99.0);
        productService.update(product);
        assertEquals(99.0, productService.getById(product.getId()).getPrice());

        // Re-fetch request before update so its productsInRequest list is populated —
        // otherwise CascadeType.ALL + orphanRemoval would delete the productInRequest row.
        Request freshRequest = requestService.getById(request.getId());
        freshRequest.setStatus(2);
        requestService.update(freshRequest);
        assertEquals(2, requestService.getById(request.getId()).getStatus());

        productInRequest.setTime(Time.valueOf(LocalTime.of(12, 30)));
        productInRequestService.update(productInRequest);
        assertEquals(Time.valueOf(LocalTime.of(12, 30)),
                productInRequestService.getById(productInRequest.getId()).getTime());

        message.setContext("Updated message");
        messageService.update(message);
        assertEquals("Updated message", messageService.getById(message.getId()).getContext());

        subscriptionDeal.setVisitsPerMonth(6);
        subscriptionDealService.update(subscriptionDeal);
        assertEquals(6, subscriptionDealService.getById(subscriptionDeal.getId()).getVisitsPerMonth());

        assertFalse(userService.getAll().isEmpty());
        assertFalse(tenantService.getAll().isEmpty());
        assertFalse(productService.getAll().isEmpty());
        assertFalse(requestService.getAll().isEmpty());
        assertFalse(productInRequestService.getAll().isEmpty());
        assertFalse(messageService.getAll().isEmpty());
        assertFalse(subscriptionDealService.getAll().isEmpty());

        assertNotNull(productInRequestService.delete(productInRequest.getId()));
        assertNotNull(messageService.delete(message.getId()));
        assertNotNull(subscriptionDealService.delete(subscriptionDeal.getId()));
        assertNotNull(userService.delete(user.getId()));
        assertNotNull(requestService.delete(request.getId()));
        assertNotNull(productService.delete(product.getId()));
        assertNotNull(tenantService.delete(tenant.getId()));

        assertNull(productInRequestService.getById(productInRequest.getId()));
        assertNull(messageService.getById(message.getId()));
        assertNull(subscriptionDealService.getById(subscriptionDeal.getId()));
        assertNull(userService.getById(user.getId()));
        assertNull(requestService.getById(request.getId()));
        assertNull(productService.getById(product.getId()));
        assertNull(tenantService.getById(tenant.getId()));
    }
}
