package app.config;

import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.services.entityServices.TenantService;
import app.services.routeSecurity.routes.Routes;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Properties;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

@Testcontainers
class RoutesTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static final String ADMIN_EMAIL = "emilxavierthorsen@gmail.com";
    private static final String ADMIN_PASSWORD = "1234";
    private static final String USER_EMAIL = "user.routes.test@example.com";
    private static final String USER_PASSWORD = "user123";

    private static Javalin app;
    private static EntityManagerFactory emf;
    private static TenantService tenantService;

    private static String adminToken;
    private static String userToken;

    @BeforeAll
    static void init() {
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);

        tenantService = new TenantService(emf);

        Routes routes = new Routes(emf);
        app = new ApplicationConfig(emf)
                .security()
                .route(routes.getRouteResource("auth"))
                .route(routes.getRoutes())
                .exceptions()
                .apiExceptions()
                .start(0);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
        RestAssured.basePath = "/api";

        seedTestUsers();

        adminToken = loginAndGetToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        userToken = loginAndGetToken(USER_EMAIL, USER_PASSWORD);
    }

    private static void seedTestUsers() {
        Tenant seedTenant = Tenant.builder().name("seed-tenant").type("test").status(1).build();
        tenantService.create(seedTenant);

        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Role adminRole = em.find(Role.class, "ADMIN");
            if (adminRole == null) { adminRole = new Role("ADMIN"); em.persist(adminRole); }
            Role userRole = em.find(Role.class, "USER");
            if (userRole == null) { userRole = new Role("USER"); em.persist(userRole); }

            User admin = new User(ADMIN_EMAIL, ADMIN_PASSWORD);
            admin.setTenant(em.find(Tenant.class, seedTenant.getId()));
            admin.addRole(adminRole);
            admin.addRole(userRole);
            em.persist(admin);

            User user = new User(USER_EMAIL, USER_PASSWORD);
            user.setTenant(em.find(Tenant.class, seedTenant.getId()));
            user.addRole(userRole);
            em.persist(user);

            em.getTransaction().commit();
        }
    }

    @AfterAll
    static void teardown() {
        if (app != null) app.stop();
        if (emf != null) emf.close();
    }

    // ─── Security layer ────────────────────────────────────────────────────────

    @Test
    void login_with_valid_credentials_returns_token() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"" + ADMIN_PASSWORD + "\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void login_with_wrong_password_returns_401() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + ADMIN_EMAIL + "\",\"password\":\"wrongpassword\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);
    }

    @Test
    void protected_endpoint_without_token_returns_401() {
        given()
                .when().get("/tenant/all")
                .then()
                .statusCode(401);
    }

    @Test
    void admin_only_endpoint_with_user_role_returns_403() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"blocked\",\"type\":\"x\",\"status\":1}")
                .when().post("/tenant/")
                .then()
                .statusCode(403);
    }

    @Test
    void user_can_access_read_endpoint() {
        given()
                .header("Authorization", "Bearer " + userToken)
                .when().get("/tenant/all")
                .then()
                .statusCode(200);
    }

    // ─── Tenant routes ─────────────────────────────────────────────────────────

    @Test
    void getall_tenants_returns_200() {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/tenant/all")
                .then()
                .statusCode(200);
    }

    // ─── User routes ───────────────────────────────────────────────────────────

    @Test
    void user_create_and_get_by_id() {
        long tenantId = createTenantId("tenant-user-create");

        Number userIdNumber = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "firstName": "Emil",
                          "lastName": "Tester",
                          "zipCode": 2800,
                          "email": "emil.create.%d@example.com",
                          "password": "secret",
                          "phoneNumber": "11111111",
                          "messageIds": []
                        }
                        """.formatted(tenantId, System.nanoTime()))
                .when().post("/user/")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");
        long userId = userIdNumber.longValue();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/user/{id}", userId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) userId))
                .body("tenantId", equalTo((int) tenantId))
                .body("firstName", equalTo("Emil"))
                .body("lastName", equalTo("Tester"));
    }

    @Test
    void user_update_changes_fields() {
        long tenantId = createTenantId("tenant-user-update");
        long userId = createUserId(tenantId, "Old", "Name", "old.user." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "firstName": "New",
                          "lastName": "Name",
                          "zipCode": 2900,
                          "email": "new.user.%d@example.com",
                          "password": "new-secret",
                          "phoneNumber": "22222222",
                          "messageIds": []
                        }
                        """.formatted(tenantId, System.nanoTime()))
                .when().put("/user/{id}", userId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) userId))
                .body("firstName", equalTo("New"));
    }

    // ─── Request routes ────────────────────────────────────────────────────────

    @Test
    void request_create_and_get_by_id() {
        long tenantId = createTenantId("tenant-request-create");

        Number requestIdNumber = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "startDate": "2026-03-13T08:00:00",
                          "endDate": "2026-03-13T10:00:00",
                          "location": "Lyngby",
                          "status": 1,
                          "type": 2,
                          "productInRequestIds": []
                        }
                        """.formatted(tenantId))
                .when().post("/request/")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract()
                .path("id");
        long requestId = requestIdNumber.longValue();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/request/{id}", requestId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) requestId))
                .body("tenantId", equalTo((int) tenantId))
                .body("location", equalTo("Lyngby"))
                .body("startDate", equalTo("2026-03-13T08:00:00"))
                .body("endDate", equalTo("2026-03-13T10:00:00"));
    }

    @Test
    void request_delete_then_get_returns_404() {
        long tenantId = createTenantId("tenant-request-delete");
        long requestId = createRequestId(tenantId, 1, 3);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().delete("/request/{id}", requestId)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/request/{id}", requestId)
                .then()
                .statusCode(404);
    }

    // ─── Helpers ───────────────────────────────────────────────────────────────

    private static String loginAndGetToken(String email, String password) {
        return given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .extract()
                .path("token");
    }

    private static long createTenantId(String name) {
        Tenant tenant = Tenant.builder()
                .name(name)
                .type("test")
                .status(1)
                .build();
        return tenantService.create(tenant).getId();
    }

    private static long createUserId(long tenantId, String firstName, String lastName, String email) {
        Number userId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "firstName": "%s",
                          "lastName": "%s",
                          "zipCode": 2800,
                          "email": "%s",
                          "password": "secret",
                          "phoneNumber": "11111111",
                          "messageIds": []
                        }
                        """.formatted(tenantId, firstName, lastName, email))
                .when().post("/user/")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return userId.longValue();
    }

    private static long createRequestId(long tenantId, int status, int type) {
        Number requestId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "startDate": "2026-03-13T08:00:00",
                          "endDate": "2026-03-13T10:00:00",
                          "location": "Lyngby",
                          "status": %d,
                          "type": %d,
                          "productInRequestIds": []
                        }
                        """.formatted(tenantId, status, type))
                .when().post("/request/")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return requestId.longValue();
    }
}
