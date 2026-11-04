package app.config;

import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingEdit;
import app.services.routeSecurity.routes.Routes;
import io.javalin.Javalin;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
@SuppressWarnings("unchecked")
class YouthIslandHistoryRoutesTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb").withUsername("test").withPassword("test");
    private static EntityManagerFactory emf;
    private static Javalin app;
    private static String anneToken;
    private static String boToken;
    private static String regularToken;
    private static Long anneId;

    @BeforeAll
    static void setup() {
        System.setProperty("ISSUER", "history-test");
        System.setProperty("TOKEN_EXPIRE_TIME", "3600000");
        System.setProperty("SECRET_KEY", "history-test-secret-key-at-least-32-bytes");
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = Tenant.builder().name("history").type("test").status(1).build();
            em.persist(tenant);
            Role userRole = new Role("USER");
            Role youthRole = new Role("YOUTH_ISLAND");
            em.persist(userRole);
            em.persist(youthRole);
            for (String name : List.of("Anne", "Bo", "Regular")) {
                User user = new User(name.toLowerCase() + "@history.test", "test-password");
                user.setFirstName(name);
                user.setLastName("Tester");
                user.setTenant(tenant);
                user.addRole(userRole);
                if (!name.equals("Regular")) user.addRole(youthRole);
                em.persist(user);
                if (name.equals("Anne")) anneId = user.getId();
            }
            em.getTransaction().commit();
        }
        Routes routes = new Routes(emf);
        app = new ApplicationConfig(emf).security().route(routes.getRouteResource("auth"))
                .route(routes.getRoutes()).exceptions().apiExceptions().start(0);
        anneToken = login("anne@history.test");
        boToken = login("bo@history.test");
        regularToken = login("regular@history.test");
    }

    @AfterAll
    static void teardown() {
        if (app != null) app.stop();
        if (emf != null) emf.close();
        for (String key : List.of("ISSUER", "TOKEN_EXPIRE_TIME", "SECRET_KEY")) System.clearProperty(key);
    }

    @Test
    void records_before_after_for_header_and_all_serving_changes_with_immutable_editor_and_timestamp() {
        Map<String, Object> booking = createBooking();
        long id = ((Number) booking.get("id")).longValue();
        List<Map<String, Object>> original = (List<Map<String, Object>>) booking.get("items");
        long keptId = ((Number) original.get(0).get("id")).longValue();
        long removedId = ((Number) original.get(1).get("id")).longValue();
        Map<String, Object> kept = new LinkedHashMap<>(original.get(0));
        kept.put("guestCount", 12);
        kept.put("servingDateTime", "2026-11-10T13:30:00");
        kept.put("productName", "Updated lunch");
        kept.put("menu", "New vegetarian menu");
        kept.put("room", "Artilleri");
        Map<String, Object> special = new LinkedHashMap<>(original.get(2));
        special.put("unitPriceOverride", 75.50);
        booking.put("notes", "Updated notes");
        booking.put("allergies", "No nuts");
        booking.put("items", List.of(kept, special, item("NEW", "Added meal", "2026-11-11T18:00:00", null)));

        Map<String, Object> updated = update(id, booking, anneToken, 200);
        assertEquals(2, ((Number) updated.get("version")).intValue());
        assertTrue(((List<Map<String, Object>>) updated.get("items")).stream().anyMatch(item -> ((Number) item.get("id")).longValue() == keptId));
        assertTrue(((List<Map<String, Object>>) updated.get("items")).stream().noneMatch(item -> ((Number) item.get("id")).longValue() == removedId));

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.find(User.class, anneId).setFirstName("Renamed");
            em.getTransaction().commit();
        }
        Map<String, Object> edit = history(id).get(0);
        assertEquals("Anne Tester", edit.get("editedByName"));
        assertEquals("anne@history.test", edit.get("editedByEmail"));
        assertNotNull(edit.get("recordedAt"));
        assertTrue(String.valueOf(edit.get("recordedAt")).endsWith("Z"));
        assertEquals(true, edit.get("detailsAvailable"));
        List<Map<String, Object>> changes = (List<Map<String, Object>>) edit.get("changes");
        assertTrue(changes.stream().anyMatch(change -> change.get("field").equals("guestCount") && change.get("before").equals(10) && change.get("after").equals(12)));
        for (String field : List.of("notes", "allergies", "servingDateTime", "productName", "menu", "room", "unitPriceOverride")) {
            assertTrue(changes.stream().anyMatch(change -> change.get("field").equals(field)), field);
        }
        assertTrue(changes.stream().anyMatch(change -> change.get("type").equals("REMOVED") && ((Number) change.get("itemId")).longValue() == removedId));
        assertTrue(changes.stream().anyMatch(change -> change.get("type").equals("ADDED") && change.get("itemName").equals("Added meal")));
        assertEquals("Original notes", ((Map<String, Object>) edit.get("beforeSnapshot")).get("notes"));
        assertEquals("Updated notes", ((Map<String, Object>) edit.get("afterSnapshot")).get("notes"));
    }

    @Test
    void rejects_stale_versions_without_overwriting_or_recording_a_false_edit() {
        Map<String, Object> original = createBooking();
        long id = ((Number) original.get("id")).longValue();
        Map<String, Object> first = new LinkedHashMap<>(original);
        first.put("notes", "First person saved");
        update(id, first, anneToken, 200);
        original.put("notes", "Stale second edit");
        update(id, original, boToken, 409);
        Map<String, Object> current = read(id);
        assertEquals("First person saved", current.get("notes"));
        assertEquals(1, history(id).size());
        current.put("notes", "Fresh second edit");
        update(id, current, boToken, 200);
        assertEquals(3, ((Number) read(id).get("version")).intValue());
        assertEquals("Bo Tester", history(id).get(1).get("editedByName"));
    }

    @Test
    void simultaneous_saves_record_one_version_and_reject_the_stale_save() throws Exception {
        Map<String, Object> original = createBooking();
        long id = ((Number) original.get("id")).longValue();
        var executor = Executors.newFixedThreadPool(2);
        try {
            List<Callable<Integer>> saves = List.of(
                    () -> request(anneToken).body(new LinkedHashMap<>(original)).put("/youth-island-booking/{id}", id).statusCode(),
                    () -> request(boToken).body(new LinkedHashMap<>(original)).put("/youth-island-booking/{id}", id).statusCode());
            List<Integer> statuses = new ArrayList<>();
            for (var result : executor.invokeAll(saves)) statuses.add(result.get());
            statuses.sort(Integer::compareTo);
            assertEquals(List.of(200, 409), statuses);
            assertEquals(1, history(id).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rejects_foreign_or_duplicate_serving_ids_atomically() {
        Map<String, Object> a = createBooking();
        Map<String, Object> b = createBooking();
        long id = ((Number) a.get("id")).longValue();
        List<Map<String, Object>> aItems = (List<Map<String, Object>>) a.get("items");
        aItems.get(0).put("id", ((List<Map<String, Object>>) b.get("items")).get(0).get("id"));
        update(id, a, anneToken, 400);
        Map<String, Object> fresh = read(id);
        var duplicated = new ArrayList<>((List<Map<String, Object>>) fresh.get("items"));
        duplicated.add(duplicated.get(0));
        fresh.put("items", duplicated);
        update(id, fresh, anneToken, 400);
        assertEquals(1, ((Number) read(id).get("version")).intValue());
        assertTrue(history(id).isEmpty());
    }

    @Test
    void unchanged_and_legacy_payloads_keep_line_ids_without_fake_field_changes() {
        Map<String, Object> booking = createBooking();
        long id = ((Number) booking.get("id")).longValue();
        List<Object> ids = ((List<Map<String, Object>>) booking.get("items")).stream().map(item -> item.get("id")).toList();
        booking = update(id, booking, boToken, 200);
        assertTrue(((List<?>) history(id).get(0).get("changes")).isEmpty());
        ((List<Map<String, Object>>) booking.get("items")).forEach(item -> item.remove("id"));
        Map<String, Object> updated = update(id, booking, boToken, 200);
        assertEquals(ids, ((List<Map<String, Object>>) updated.get("items")).stream().map(item -> item.get("id")).toList());
        assertTrue(((List<?>) history(id).get(1).get("changes")).isEmpty());
    }

    @Test
    void preserves_metadata_only_history_and_enforces_history_access() {
        Map<String, Object> booking = createBooking();
        long id = ((Number) booking.get("id")).longValue();
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            var edit = YouthIslandBookingEdit.builder().booking(em.getReference(YouthIslandBooking.class, id))
                    .editedBy(em.getReference(User.class, anneId)).editedAt(LocalDateTime.of(2026, 9, 1, 12, 0)).build();
            em.persist(edit);
            em.flush();
            em.createNativeQuery("UPDATE youth_island_booking_edits SET recorded_at = NULL WHERE id = :id").setParameter("id", edit.getId()).executeUpdate();
            em.getTransaction().commit();
        }
        assertEquals(false, history(id).get(0).get("detailsAvailable"));
        assertNull(history(id).get(0).get("beforeSnapshot"));
        request(regularToken).get("/youth-island-booking/{id}/history", id).then().statusCode(403);
        request(null).get("/youth-island-booking/{id}/history", id).then().statusCode(401);
        request(boToken).get("/youth-island-booking/{id}/history", Long.MAX_VALUE).then().statusCode(404);
    }

    private static RequestSpecification request(String token) {
        RequestSpecification request = given().baseUri("http://localhost").port(app.port()).basePath("/api").contentType(ContentType.JSON);
        return token == null ? request : request.header("Authorization", "Bearer " + token);
    }

    private static String login(String email) {
        return request(null).body(Map.of("email", email, "password", "test-password")).post("/auth/login")
                .then().statusCode(200).extract().path("token");
    }

    private static Map<String, Object> createBooking() {
        Map<String, Object> body = new LinkedHashMap<>(Map.of(
                "customerName", "History regression", "startDateTime", "2026-11-10T08:00:00", "endDateTime", "2026-11-12T20:00:00",
                "spectraReservationNumber", "H-" + System.nanoTime(), "orderDate", "2026-11-04", "notes", "Original notes"));
        body.put("items", List.of(item("MAIN", "Lunch", "2026-11-10T12:00:00", null),
                item("REMOVE", "To remove", "2026-11-11T08:00:00", null), item("1019", "Special", "2026-11-11T15:00:00", 50)));
        return request(anneToken).body(body).post("/youth-island-booking/").then().statusCode(201).extract().as(Map.class);
    }

    private static Map<String, Object> item(String number, String name, String datetime, Number price) {
        Map<String, Object> item = new LinkedHashMap<>(Map.of("guestCount", 10, "servingDateTime", datetime, "productNumber", number, "productName", name));
        if (price != null) item.put("unitPriceOverride", price);
        return item;
    }

    private static Map<String, Object> read(long id) {
        return request(boToken).get("/youth-island-booking/{id}", id).then().statusCode(200).extract().as(Map.class);
    }

    private static Map<String, Object> update(long id, Map<String, Object> body, String token, int status) {
        var response = request(token).body(body).put("/youth-island-booking/{id}", id).then().statusCode(status);
        return status == 200 ? response.extract().as(Map.class) : Map.of();
    }

    private static List<Map<String, Object>> history(long id) {
        return request(boToken).get("/youth-island-booking/{id}/history", id).then().statusCode(200).extract().as(List.class);
    }
}
