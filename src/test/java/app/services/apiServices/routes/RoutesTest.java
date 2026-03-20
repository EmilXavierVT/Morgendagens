package app.services.apiServices.routes;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.entities.Tenant;
import app.services.entityServices.TenantService;
import io.javalin.Javalin;
import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

class RoutesTest {
    private static Javalin app;
    private static EntityManagerFactory emf;
    private static TenantService tenantService;

    @BeforeAll
    static void init() {
        emf = HibernateConfig.getEntityManagerFactory();
        tenantService = new TenantService(emf);

        Routes routes = new Routes(emf);
        ApplicationConfig config = new ApplicationConfig(routes);
        app = config.startServer(0);

        RestAssured.baseURI = "http://localhost";
        RestAssured.port = app.port();
        RestAssured.basePath = "/api";
    }

    @AfterAll
    static void teardown() {
        if (app != null) app.stop();
        if (emf != null) emf.close();
    }

    @Test
    void getall_tenants_returns_200() {
        given()
                .when().get("/tenant/all")
                .then()
                .statusCode(200);
    }

    @Test
    void user_create_and_get_by_id() {
        long tenantId = createTenantId("tenant-user-create");

        Number userIdNumber = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "role": 1,
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
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "role": 2,
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
                .body("firstName", equalTo("New"))
                .body("role", equalTo(2));
    }

    @Test
    void request_create_and_get_by_id() {
        long tenantId = createTenantId("tenant-request-create");

        Number requestIdNumber = given()
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
                .when().delete("/request/{id}", requestId)
                .then()
                .statusCode(204);

        given()
                .when().get("/request/{id}", requestId)
                .then()
                .statusCode(404);
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
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "tenantId": %d,
                          "role": 1,
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
