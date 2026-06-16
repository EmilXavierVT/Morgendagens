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
import static org.hamcrest.Matchers.*;

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
        System.setProperty("ISSUER", "morgendagens-test");
        System.setProperty("TOKEN_EXPIRE_TIME", "3600000");
        System.setProperty("SECRET_KEY", "morgendagens-test-secret-key-32bytes!!");

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
        System.clearProperty("ISSUER");
        System.clearProperty("TOKEN_EXPIRE_TIME");
        System.clearProperty("SECRET_KEY");
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
    void register_with_existing_email_returns_409() {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + USER_EMAIL + "\",\"password\":\"new-password\"}")
                .when().post("/auth/register")
                .then()
                .statusCode(409)
                .body("status", equalTo(409))
                .body("msg", equalTo("User already exists"));
    }

    @Test
    void protected_endpoint_without_token_returns_401() {
        given()
                .when().get("/tenant/all")
                .then()
                .statusCode(401);
    }

    @Test
    void health_endpoint_is_public() {
        given()
                .when().get("/health")
                .then()
                .statusCode(200)
                .body("status", equalTo("ok"));
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

    @Test
    void change_password_updates_credentials() {
        String email = "change.password." + System.nanoTime() + "@example.com";
        registerUser(email, "secret");
        String token = loginAndGetToken(email, "secret");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentPassword": "secret",
                          "newPassword": "super-secret"
                        }
                        """)
                .when().put("/auth/change-password")
                .then()
                .statusCode(200)
                .body("msg", equalTo("Password changed successfully"));

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"secret\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(401);

        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"super-secret\"}")
                .when().post("/auth/login")
                .then()
                .statusCode(200)
                .body("token", notNullValue());
    }

    @Test
    void change_password_with_wrong_current_password_returns_400() {
        String email = "change.password.wrong." + System.nanoTime() + "@example.com";
        registerUser(email, "secret");
        String token = loginAndGetToken(email, "secret");

        given()
                .header("Authorization", "Bearer " + token)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentPassword": "incorrect",
                          "newPassword": "super-secret"
                        }
                        """)
                .when().put("/auth/change-password")
                .then()
                .statusCode(400)
                .body("msg", equalTo("Current password is incorrect"));
    }

    @Test
    void change_password_without_token_returns_401() {
        given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "currentPassword": "secret",
                          "newPassword": "super-secret"
                        }
                        """)
                .when().put("/auth/change-password")
                .then()
                .statusCode(401);
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
                          "streetName": "Testvej",
                          "streetNumber": "12A",
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
                .body("lastName", equalTo("Tester"))
                .body("streetName", equalTo("Testvej"))
                .body("streetNumber", equalTo("12A"));
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
                          "streetName": "Updated Street",
                          "streetNumber": "44B",
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
                .body("firstName", equalTo("New"))
                .body("streetName", equalTo("Updated Street"))
                .body("streetNumber", equalTo("44B"));
    }

    @Test
    void set_employee_adds_employee_role_to_user() {
        long tenantId = createTenantId("tenant-user-employee");
        long userId = createUserId(tenantId, "Employee", "Candidate", "employee.role." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/employee", userId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) userId))
                .body("roles", hasItems("EMPLOYEE"));
    }

    @Test
    void set_cleaning_staff_adds_cleaning_staff_role_to_user() {
        long tenantId = createTenantId("tenant-user-cleaning-staff");
        long userId = createUserId(tenantId, "Cleaner", "Candidate", "cleaner.role." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/cleaning-staff", userId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) userId))
                .body("roles", hasItems("CLEANING_STAFF"));
    }

    @Test
    void set_cleaning_client_adds_cleaning_client_role_to_user() {
        long tenantId = createTenantId("tenant-user-cleaning-client");
        long userId = createUserId(tenantId, "Client", "Candidate", "cleaning.client.role." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/cleaning-client", userId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) userId))
                .body("roles", hasItems("CLEANING_CLIENT"));
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

    @Test
    void get_requests_by_user_id_returns_requests_for_users_tenant() {
        long tenantId = createTenantId("tenant-request-by-user");
        long userId = createUserId(tenantId, "Test", "User", "request.byuser." + System.nanoTime() + "@example.com");
        createRequestId(tenantId, 1, 2);
        createRequestId(tenantId, 2, 3);

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/request/user/{userId}", userId)
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].tenantId", equalTo((int) tenantId))
                .body("[1].tenantId", equalTo((int) tenantId));
    }

    @Test
    void get_requests_by_user_id_returns_empty_when_no_requests_exist() {
        long tenantId = createTenantId("tenant-no-requests");
        long userId = createUserId(tenantId, "Empty", "User", "no.requests." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/request/user/{userId}", userId)
                .then()
                .statusCode(200)
                .body("$", hasSize(0));
    }

    // ─── WorkLog routes ────────────────────────────────────────────────────────

    @Test
    void worklog_create_and_get_by_id_for_employee_user() {
        long tenantId = createTenantId("tenant-worklog-create");
        long userId = createUserId(tenantId, "Work", "Logger", "worklog.create." + System.nanoTime() + "@example.com");
        assignEmployeeRole(userId);

        Number workLogIdNumber = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startTime": "2026-03-13T08:00:00",
                          "endTime": "2026-03-13T16:00:00",
                          "userId": %d
                        }
                        """.formatted(userId))
                .when().post("/worklog/")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("userId", equalTo((int) userId))
                .extract()
                .path("id");
        long workLogId = workLogIdNumber.longValue();

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/worklog/{id}", workLogId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) workLogId))
                .body("userId", equalTo((int) userId))
                .body("startTime", equalTo("2026-03-13T08:00:00"))
                .body("endTime", equalTo("2026-03-13T16:00:00"));
    }

    @Test
    void employee_can_create_own_worklog() {
        String email = "worklog.self." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-worklog-self");
        long userId = createUserId(tenantId, "Self", "Logger", email);
        assignEmployeeRole(userId);
        String employeeToken = loginAndGetToken(email, "secret");

        given()
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startTime": "2026-03-13T08:00:00",
                          "endTime": "2026-03-13T16:00:00",
                          "userId": %d
                        }
                        """.formatted(userId))
                .when().post("/worklog/")
                .then()
                .statusCode(201)
                .body("userId", equalTo((int) userId));
    }

    @Test
    void employee_cannot_create_worklog_for_other_employee() {
        String email = "worklog.blocked." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-worklog-blocked");
        long employeeId = createUserId(tenantId, "Blocked", "Logger", email);
        long otherEmployeeId = createUserId(tenantId, "Other", "Logger", "worklog.other." + System.nanoTime() + "@example.com");
        assignEmployeeRole(employeeId);
        assignEmployeeRole(otherEmployeeId);
        String employeeToken = loginAndGetToken(email, "secret");

        given()
                .header("Authorization", "Bearer " + employeeToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startTime": "2026-03-13T08:00:00",
                          "endTime": "2026-03-13T16:00:00",
                          "userId": %d
                        }
                        """.formatted(otherEmployeeId))
                .when().post("/worklog/")
                .then()
                .statusCode(403)
                .body("msg", equalTo("Employees can only create worklogs for themselves"));
    }

    @Test
    void worklog_create_for_non_employee_user_returns_400() {
        long tenantId = createTenantId("tenant-worklog-non-employee");
        long userId = createUserId(tenantId, "Plain", "User", "worklog.reject." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startTime": "2026-03-13T08:00:00",
                          "endTime": "2026-03-13T16:00:00",
                          "userId": %d
                        }
                        """.formatted(userId))
                .when().post("/worklog/")
                .then()
                .statusCode(400)
                .body("msg", equalTo("User must have EMPLOYEE role"));
    }

    @Test
    void get_worklogs_by_user_id_returns_only_that_users_logs() {
        long tenantId = createTenantId("tenant-worklog-by-user");
        long employeeId = createUserId(tenantId, "Hours", "One", "worklog.one." + System.nanoTime() + "@example.com");
        long otherEmployeeId = createUserId(tenantId, "Hours", "Two", "worklog.two." + System.nanoTime() + "@example.com");
        assignEmployeeRole(employeeId);
        assignEmployeeRole(otherEmployeeId);

        createWorkLogId(employeeId, "2026-03-13T08:00:00", "2026-03-13T12:00:00");
        createWorkLogId(employeeId, "2026-03-13T13:00:00", "2026-03-13T17:00:00");
        createWorkLogId(otherEmployeeId, "2026-03-14T08:00:00", "2026-03-14T12:00:00");

        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().get("/worklog/user/{userId}", employeeId)
                .then()
                .statusCode(200)
                .body("$", hasSize(2))
                .body("[0].userId", equalTo((int) employeeId))
                .body("[1].userId", equalTo((int) employeeId));
    }

    @Test
    void cleaning_staff_can_create_and_get_own_appointment() {
        String staffEmail = "cleaning.staff." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-appointment-create");
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", staffEmail);
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", "cleaning.client." + System.nanoTime() + "@example.com");
        assignCleaningStaffRole(cleaningStaffId);
        String cleaningStaffToken = loginAndGetToken(staffEmail, "secret");

        Number appointmentIdNumber = given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 90,
                          "vacation": false
                        }
                        """.formatted(cleaningClientId, cleaningStaffId))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("cleaningClientId", equalTo((int) cleaningClientId))
                .body("cleaningStaffId", equalTo((int) cleaningStaffId))
                .body("appointmentTime", equalTo("2026-03-13T09:00:00"))
                .body("cancellationTime", nullValue())
                .body("durationMinutes", equalTo(90))
                .body("vacation", equalTo(false))
                .extract()
                .path("id");
        long appointmentId = appointmentIdNumber.longValue();

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .when().get("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) appointmentId))
                .body("cleaningClientId", equalTo((int) cleaningClientId))
                .body("cleaningStaffId", equalTo((int) cleaningStaffId))
                .body("cancellationTime", nullValue())
                .body("vacation", equalTo(false));
    }

    @Test
    void cleaning_staff_can_update_and_delete_own_appointment() {
        String staffEmail = "cleaning.staff.update." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-appointment-update");
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", staffEmail);
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", "cleaning.client.update." + System.nanoTime() + "@example.com");
        assignCleaningStaffRole(cleaningStaffId);
        String cleaningStaffToken = loginAndGetToken(staffEmail, "secret");
        long appointmentId = createCleaningAppointmentId(cleaningClientId, cleaningStaffId, false);

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-14T10:30:00",
                          "cancellationTime": "2026-03-14T08:00:00",
                          "durationMinutes": 120,
                          "vacation": true
                        }
                        """.formatted(cleaningClientId, cleaningStaffId))
                .when().put("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) appointmentId))
                .body("appointmentTime", equalTo("2026-03-14T10:30:00"))
                .body("cancellationTime", equalTo("2026-03-14T08:00:00"))
                .body("durationMinutes", equalTo(120))
                .body("vacation", equalTo(true));

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .when().delete("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(204);

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .when().get("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(404);
    }

    @Test
    void cleaning_staff_cannot_delete_other_staff_appointment() {
        String staffEmail = "cleaning.staff.delete.blocked." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-appointment-delete-blocked");
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", staffEmail);
        long otherCleaningStaffId = createUserId(tenantId, "Other", "Staff", "cleaning.staff.delete.other." + System.nanoTime() + "@example.com");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", "cleaning.client.delete.blocked." + System.nanoTime() + "@example.com");
        assignCleaningStaffRole(cleaningStaffId);
        assignCleaningStaffRole(otherCleaningStaffId);
        String cleaningStaffToken = loginAndGetToken(staffEmail, "secret");
        long appointmentId = createCleaningAppointmentId(cleaningClientId, otherCleaningStaffId, false);

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .when().delete("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(403)
                .body("msg", equalTo("Cleaning staff can only delete their own appointments"));
    }

    @Test
    void cleaning_staff_cannot_create_appointment_for_other_staff() {
        String staffEmail = "cleaning.staff.blocked." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-appointment-blocked");
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", staffEmail);
        long otherCleaningStaffId = createUserId(tenantId, "Other", "Staff", "cleaning.staff.other." + System.nanoTime() + "@example.com");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", "cleaning.client.blocked." + System.nanoTime() + "@example.com");
        assignCleaningStaffRole(cleaningStaffId);
        assignCleaningStaffRole(otherCleaningStaffId);
        String cleaningStaffToken = loginAndGetToken(staffEmail, "secret");

        given()
                .header("Authorization", "Bearer " + cleaningStaffToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 90,
                          "vacation": true
                        }
                        """.formatted(cleaningClientId, otherCleaningStaffId))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(403)
                .body("msg", equalTo("Cleaning staff can only create appointments for themselves"));
    }

    @Test
    void regular_user_cannot_create_cleaning_appointment() {
        long tenantId = createTenantId("tenant-cleaning-appointment-role-protection");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", "cleaning.client.protected." + System.nanoTime() + "@example.com");

        given()
                .header("Authorization", "Bearer " + userToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 90,
                          "vacation": false
                        }
                        """.formatted(cleaningClientId))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(403);
    }

    @Test
    void cleaning_client_can_create_appointment_for_self_when_staff_is_provided() {
        String clientEmail = "cleaning.client.self." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-client-create");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", clientEmail);
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", "cleaning.staff.assigned." + System.nanoTime() + "@example.com");
        assignCleaningClientRole(cleaningClientId);
        assignCleaningStaffRole(cleaningStaffId);
        String cleaningClientToken = loginAndGetToken(clientEmail, "secret");

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 120,
                          "vacation": false
                        }
                        """.formatted(cleaningClientId, cleaningStaffId))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(201)
                .body("cleaningClientId", equalTo((int) cleaningClientId))
                .body("cleaningStaffId", equalTo((int) cleaningStaffId))
                .body("cancellationTime", nullValue());
    }

    @Test
    void cleaning_client_can_create_appointment_without_staff() {
        String clientEmail = "cleaning.client.unassigned." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-client-unassigned");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", clientEmail);
        assignCleaningClientRole(cleaningClientId);
        String cleaningClientToken = loginAndGetToken(clientEmail, "secret");

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 120,
                          "vacation": false
                        }
                        """.formatted(cleaningClientId))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(201)
                .body("cleaningClientId", equalTo((int) cleaningClientId))
                .body("cancellationTime", nullValue())
                .body("cleaningStaffId", nullValue());
    }

    @Test
    void cleaning_client_can_read_update_and_delete_own_appointment() {
        String clientEmail = "cleaning.client.crud." + System.nanoTime() + "@example.com";
        long tenantId = createTenantId("tenant-cleaning-client-crud");
        long cleaningClientId = createUserId(tenantId, "Cleaning", "Client", clientEmail);
        long cleaningStaffId = createUserId(tenantId, "Cleaning", "Staff", "cleaning.staff.crud." + System.nanoTime() + "@example.com");
        assignCleaningClientRole(cleaningClientId);
        assignCleaningStaffRole(cleaningStaffId);
        String cleaningClientToken = loginAndGetToken(clientEmail, "secret");
        long appointmentId = createCleaningAppointmentId(cleaningClientId, cleaningStaffId, false);

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .when().get("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(200)
                .body("id", equalTo((int) appointmentId))
                .body("cleaningClientId", equalTo((int) cleaningClientId));

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .when().get("/cleaning-appointment/all")
                .then()
                .statusCode(200)
                .body("$", hasSize(1))
                .body("[0].id", equalTo((int) appointmentId));

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-14T11:00:00",
                          "cancellationTime": "2026-03-14T08:30:00",
                          "durationMinutes": 60,
                          "vacation": true
                        }
                        """.formatted(cleaningClientId, cleaningStaffId))
                .when().put("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(200)
                .body("appointmentTime", equalTo("2026-03-14T11:00:00"))
                .body("cancellationTime", equalTo("2026-03-14T08:30:00"))
                .body("durationMinutes", equalTo(60))
                .body("vacation", equalTo(true));

        given()
                .header("Authorization", "Bearer " + cleaningClientToken)
                .when().delete("/cleaning-appointment/{id}", appointmentId)
                .then()
                .statusCode(204);
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

    private static void registerUser(String email, String password) {
        given()
                .contentType(ContentType.JSON)
                .body("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}")
                .when().post("/auth/register")
                .then()
                .statusCode(201);
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
                          "streetName": "Helper Street",
                          "streetNumber": "7",
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

    private static void assignEmployeeRole(long userId) {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/employee", userId)
                .then()
                .statusCode(200);
    }

    private static void assignCleaningStaffRole(long userId) {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/cleaning-staff", userId)
                .then()
                .statusCode(200);
    }

    private static void assignCleaningClientRole(long userId) {
        given()
                .header("Authorization", "Bearer " + adminToken)
                .when().put("/user/{id}/cleaning-client", userId)
                .then()
                .statusCode(200);
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

    private static long createWorkLogId(long userId, String startTime, String endTime) {
        Number workLogId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "startTime": "%s",
                          "endTime": "%s",
                          "userId": %d
                        }
                        """.formatted(startTime, endTime, userId))
                .when().post("/worklog/")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return workLogId.longValue();
    }

    private static long createCleaningAppointmentId(long cleaningClientId, long cleaningStaffId, boolean vacation) {
        Number appointmentId = given()
                .header("Authorization", "Bearer " + adminToken)
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "cleaningClientId": %d,
                          "cleaningStaffId": %d,
                          "appointmentTime": "2026-03-13T09:00:00",
                          "cancellationTime": null,
                          "durationMinutes": 90,
                          "vacation": %s
                        }
                        """.formatted(cleaningClientId, cleaningStaffId, vacation))
                .when().post("/cleaning-appointment/")
                .then()
                .statusCode(201)
                .extract()
                .path("id");
        return appointmentId.longValue();
    }
}
