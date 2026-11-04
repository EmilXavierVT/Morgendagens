package app.config;

import app.entities.*;
import app.services.entityServices.*;
import app.services.economic.YouthIslandInvoiceService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
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
    private static CleaningAppointmentService cleaningAppointmentService;

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
        cleaningAppointmentService = new CleaningAppointmentService(emf);
    }

    @AfterAll
    static void teardown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void youth_island_special_serving_uses_deployed_product_price_override_in_invoice_preview() {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = Tenant.builder().name("youth-special").type("test").status(1).build();
            em.persist(tenant);
            User user = new User("youth-special@example.test", "test-password");
            user.setTenant(tenant);
            em.persist(user);
            em.persist(Product.builder().name("DIVERSE SALG").economicProductNumber("1019")
                    .economicAvailable(true).price(0).type(2).build());
            em.persist(Product.builder().name("Regular meal").economicProductNumber("YI-REGULAR")
                    .economicAvailable(true).price(100).type(2).build());
            em.persist(EconomicCustomer.builder().name("Ungdomsø Event").customerNumber(12345)
                    .economicAvailable(true).build());
            YouthIslandBooking booking = YouthIslandBooking.builder()
                    .orderedBy(user).customerName("Special serving regression")
                    .eventDate(LocalDate.of(2026, 11, 4))
                    .startDateTime(LocalDateTime.of(2026, 11, 4, 8, 0))
                    .endDateTime(LocalDateTime.of(2026, 11, 5, 18, 0))
                    .spectraReservationNumber("special-serving-regression")
                    .orderDate(LocalDate.of(2026, 11, 1)).build();
            booking.setItems(List.of(
                    YouthIslandBookingItem.builder().guestCount(2).servingTime(LocalTime.of(8, 0))
                            .servingDateTime(LocalDateTime.of(2026, 11, 5, 8, 0))
                            .productNumber("1019").productName("Special breakfast")
                            .unitPriceOverride(new BigDecimal("87.50")).build(),
                    YouthIslandBookingItem.builder().guestCount(1).servingTime(LocalTime.of(9, 0))
                            .servingDateTime(LocalDateTime.of(2026, 11, 5, 9, 0))
                            .productNumber("DIVERSE").productName("Legacy special serving")
                            .unitPriceOverride(new BigDecimal("50.00")).build(),
                    YouthIslandBookingItem.builder().guestCount(1).servingTime(LocalTime.of(23, 0))
                            .servingDateTime(LocalDateTime.of(2026, 11, 4, 23, 0))
                            .productNumber("YI-REGULAR").productName("Evening meal").build()));
            em.persist(booking);
            em.getTransaction().commit();
        }

        var service = new YouthIslandInvoiceService(emf);
        var discounted = service.preview("2026-11-04", 12345, true);
        assertEquals(3, discounted.getLineCount());
        assertEquals("Evening meal", discounted.getLines().get(0).getProductName());
        assertEquals(LocalDateTime.of(2026, 11, 5, 8, 0), discounted.getLines().get(1).getServingDateTime());
        assertEquals("1019", discounted.getLines().get(2).getProductNumber());
        assertEquals(0, new BigDecimal("87.50").compareTo(discounted.getLines().get(1).getUnitNetPrice()));
        assertEquals(0, new BigDecimal("217.75").compareTo(discounted.getTotalNetAmount()));
        assertEquals(0, new BigDecimal("325.00").compareTo(service.preview("2026-11-04", 12345, false).getTotalNetAmount()));
    }

    @Test
    void youth_serving_date_migration_backfills_legacy_times_without_changing_explicit_dates() {
        Long legacyId;
        Long datedId;
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = Tenant.builder().name("youth-date-migration").type("test").status(1).build();
            em.persist(tenant);
            User user = new User("youth-migration@example.test", "test-password");
            user.setTenant(tenant);
            em.persist(user);
            YouthIslandBooking booking = YouthIslandBooking.builder().orderedBy(user)
                    .customerName("Legacy date migration").eventDate(LocalDate.of(2026, 11, 4))
                    .startDateTime(LocalDateTime.of(2026, 11, 4, 18, 0))
                    .endDateTime(LocalDateTime.of(2026, 11, 5, 18, 0))
                    .spectraReservationNumber("legacy-date-regression").orderDate(LocalDate.of(2026, 11, 1)).build();
            YouthIslandBookingItem legacy = YouthIslandBookingItem.builder().guestCount(1)
                    .servingTime(LocalTime.of(19, 15)).productNumber("TEST").productName("Legacy evening").build();
            YouthIslandBookingItem dated = YouthIslandBookingItem.builder().guestCount(1)
                    .servingTime(LocalTime.of(8, 0)).servingDateTime(LocalDateTime.of(2026, 11, 5, 8, 0))
                    .productNumber("TEST").productName("Next morning").build();
            booking.setItems(List.of(legacy, dated));
            em.persist(booking);
            em.getTransaction().commit();
            legacyId = legacy.getId();
            datedId = dated.getId();
        }
        CleaningSchemaMigration.migrate(emf);
        CleaningSchemaMigration.migrate(emf);
        try (EntityManager em = emf.createEntityManager()) {
            assertEquals(Boolean.TRUE, em.createNativeQuery("SELECT serving_date_time = TIMESTAMP '2026-11-04 19:15:00' FROM youth_island_booking_items WHERE id = :id")
                    .setParameter("id", legacyId).getSingleResult());
            assertEquals(Boolean.TRUE, em.createNativeQuery("SELECT serving_date_time = TIMESTAMP '2026-11-05 08:00:00' FROM youth_island_booking_items WHERE id = :id")
                    .setParameter("id", datedId).getSingleResult());
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

    @Test
    void cleaning_appointment_task_edits_can_apply_to_future_assignments_for_same_customer() {
        Tenant tenant = Tenant.builder()
                .name("cleaning-task-template")
                .type("cleaning")
                .status(1)
                .build();
        tenantService.create(tenant);

        User client = new User("cleaning-template-client@example.com", "pw");
        client.setTenant(tenant);
        client.setFirstName("Template");
        client.setLastName("Client");
        userService.create(client);
        userService.setCleaningClient(client.getId());

        User staff = new User("cleaning-template-staff@example.com", "pw");
        staff.setTenant(tenant);
        staff.setFirstName("Template");
        staff.setLastName("Staff");
        userService.create(staff);
        userService.setCleaningStaff(staff.getId());

        CleaningAppointment earlier = buildCleaningAppointment(
                client,
                staff,
                LocalDateTime.of(2026, 1, 1, 9, 0),
                List.of(buildCleaningTask("Old task", false, 1))
        );
        earlier = cleaningAppointmentService.create(earlier);

        CleaningAppointment source = buildCleaningAppointment(
                client,
                staff,
                LocalDateTime.of(2026, 1, 8, 9, 0),
                List.of(buildCleaningTask("Old task", false, 1))
        );
        source = cleaningAppointmentService.create(source);

        CleaningAppointment future = buildCleaningAppointment(
                client,
                staff,
                LocalDateTime.of(2026, 1, 15, 9, 0),
                List.of(buildCleaningTask("Kitchen reset", true, 1), buildCleaningTask("Old task", false, 2))
        );
        future = cleaningAppointmentService.create(future);

        CleaningAppointment update = buildCleaningAppointment(
                client,
                staff,
                source.getAppointmentTime(),
                List.of(buildCleaningTask("Kitchen reset", false, 1), buildCleaningTask("Floors", false, 2))
        );
        update.setId(source.getId());
        update.setProject(source.getProject());

        cleaningAppointmentService.update(update, true);

        CleaningAppointment refreshedEarlier = cleaningAppointmentService.getById(earlier.getId());
        CleaningAppointment refreshedSource = cleaningAppointmentService.getById(source.getId());
        CleaningAppointment refreshedFuture = cleaningAppointmentService.getById(future.getId());

        assertEquals(List.of("Old task"), taskTitles(refreshedEarlier));
        assertEquals(List.of("Kitchen reset", "Floors"), taskTitles(refreshedSource));
        assertEquals(List.of("Kitchen reset", "Floors"), taskTitles(refreshedFuture));
        assertTrue(refreshedFuture.getTasks().get(0).isCompleted());
        assertFalse(refreshedFuture.getTasks().get(1).isCompleted());
    }

    private CleaningAppointment buildCleaningAppointment(User client, User staff, LocalDateTime appointmentTime, List<CleaningAssignmentTask> tasks) {
        CleaningAppointment appointment = new CleaningAppointment();
        appointment.setCleaningClient(client);
        appointment.setCleaningStaff(staff);
        appointment.setAppointmentTime(appointmentTime);
        appointment.setDurationMinutes(120);
        appointment.setTasks(tasks);
        return appointment;
    }

    private CleaningAssignmentTask buildCleaningTask(String title, boolean completed, int sortOrder) {
        CleaningAssignmentTask task = new CleaningAssignmentTask();
        task.setTitle(title);
        task.setCompleted(completed);
        task.setSortOrder(sortOrder);
        return task;
    }

    private List<String> taskTitles(CleaningAppointment appointment) {
        return appointment.getTasks().stream()
                .map(CleaningAssignmentTask::getTitle)
                .toList();
    }
}
