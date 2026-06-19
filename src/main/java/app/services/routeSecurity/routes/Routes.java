package app.services.routeSecurity.routes;


import app.config.HibernateConfig;
import app.controller.SystemController;
import app.services.routeSecurity.ISecurityController;
import app.services.routeSecurity.SecurityController;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.javalin.apibuilder.EndpointGroup;
import io.javalin.security.RouteRole;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final EntityManagerFactory emf;
    ObjectMapper objectMapper = new ObjectMapper();
    private final ISecurityController securityController;

    public Routes() {
        this(HibernateConfig.getEntityManagerFactory());
    }


    public Routes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.securityController = new SecurityController(emf);
    }

    public EndpointGroup getRoutes() {
        TenantRoutes tenantRoutes = new TenantRoutes(emf);
        UserRoutes userRoutes = new UserRoutes(emf);
        ProductRoutes productRoutes = new ProductRoutes(emf);
        ProductInRequestRoutes productInRequestRoutes = new ProductInRequestRoutes(emf);
        RequestRoutes requestRoutes = new RequestRoutes(emf);
        MessageRoutes messageRoutes = new MessageRoutes(emf);
        WorkLogRoutes workLogRoutes = new WorkLogRoutes(emf);
        CleaningAppointmentRoutes cleaningAppointmentRoutes = new CleaningAppointmentRoutes(emf);
        SubscriptionDealRoutes subscriptionDealRoutes = new SubscriptionDealRoutes(emf);
        EmailRoutes emailRoutes = new EmailRoutes();
        SystemController systemController = new SystemController();

        return () -> {
            get("/", ctx -> ctx.result("Hello Javalin World!"));
            get("/health", systemController::health, Role.ANYONE);

            path("/tenant", () -> {
                get("/all", tenantRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", tenantRoutes::create, Role.ADMIN);
                get("/{id}", tenantRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", tenantRoutes::update, Role.ADMIN);
                delete("/{id}", tenantRoutes::delete, Role.ADMIN);
            });

            path("/user", () -> {
                get("/all", userRoutes::getAll, Role.ADMIN, Role.USER);
                post("/", userRoutes::create, Role.ADMIN);
                get("/{id}", userRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", userRoutes::update, Role.USER, Role.ADMIN);
                delete("/{id}", userRoutes::delete, Role.ADMIN);
                put("/{id}/admin", userRoutes::setAdmin, Role.ADMIN);
                put("/{id}/employee", userRoutes::setEmployee, Role.ADMIN);
                put("/{id}/cleaning-staff", userRoutes::setCleaningStaff, Role.ADMIN);
                put("/{id}/cleaning-client", userRoutes::setCleaningClient, Role.ADMIN);
                put("/{id}/subscriber", userRoutes::setSubscriber, Role.ADMIN);
            });

            path("/product", () -> {
                get("/all", productRoutes::getAll, Role.ANYONE);
                post("/", productRoutes::create, Role.ADMIN);
                get("/{id}", productRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", productRoutes::update, Role.ADMIN);
                delete("/{id}", productRoutes::delete, Role.ADMIN);
            });

            path("/product-in-requests", () -> {
                get("/all", productInRequestRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", productInRequestRoutes::create, Role.ANYONE);
                get("/{id}", productInRequestRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", productInRequestRoutes::update, Role.ADMIN);
                delete("/{id}", productInRequestRoutes::delete, Role.ADMIN);
            });

            path("/request", () -> {
                get("/all", requestRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", requestRoutes::create, Role.ANYONE);
                get("/user/{userId}", requestRoutes::getByUserId, Role.USER, Role.ADMIN);
                get("/{id}", requestRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", requestRoutes::update, Role.USER, Role.ADMIN);
                delete("/{id}", requestRoutes::delete, Role.ADMIN);
            });

            path("/message", () -> {
                get("/all", messageRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", messageRoutes::create, Role.USER, Role.ADMIN);
                get("/{id}", messageRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", messageRoutes::update, Role.USER, Role.ADMIN);
                delete("/{id}", messageRoutes::delete, Role.ADMIN);
            });

            path("/worklog", () -> {
                get("/all", workLogRoutes::getAll, Role.ADMIN, Role.EMPLOYEE);
                post("/", workLogRoutes::create, Role.ADMIN, Role.EMPLOYEE);
                get("/user/{userId}", workLogRoutes::getByUserId, Role.ADMIN, Role.EMPLOYEE);
                get("/{id}", workLogRoutes::getById, Role.ADMIN, Role.EMPLOYEE);
                put("/{id}", workLogRoutes::update, Role.ADMIN, Role.EMPLOYEE);
                delete("/{id}", workLogRoutes::delete, Role.ADMIN);
            });

            path("/cleaning-appointment", () -> {
                get("/all", cleaningAppointmentRoutes::getAll, Role.ADMIN, Role.CLEANING_STAFF, Role.CLEANING_CLIENT);
                post("/", cleaningAppointmentRoutes::create, Role.ADMIN, Role.CLEANING_STAFF, Role.CLEANING_CLIENT);
                get("/{id}", cleaningAppointmentRoutes::getById, Role.ADMIN, Role.CLEANING_STAFF, Role.CLEANING_CLIENT);
                put("/{id}", cleaningAppointmentRoutes::update, Role.ADMIN, Role.CLEANING_STAFF, Role.CLEANING_CLIENT);
                delete("/{id}", cleaningAppointmentRoutes::delete, Role.ADMIN, Role.CLEANING_STAFF, Role.CLEANING_CLIENT);
            });

            path("/email", () -> {
                post("/send", emailRoutes::send, Role.USER, Role.ADMIN);
            });

            path("/subscription-deal", () -> {
                get("/all", subscriptionDealRoutes::getAll, Role.ADMIN, Role.SUBSCRIBER, Role.CLEANING_STAFF);
                post("/", subscriptionDealRoutes::create, Role.ADMIN, Role.SUBSCRIBER, Role.CLEANING_STAFF);
                get("/{id}", subscriptionDealRoutes::getById, Role.ADMIN, Role.SUBSCRIBER, Role.CLEANING_STAFF);
                put("/{id}", subscriptionDealRoutes::update, Role.ADMIN, Role.SUBSCRIBER, Role.CLEANING_STAFF);
                delete("/{id}", subscriptionDealRoutes::delete, Role.ADMIN, Role.SUBSCRIBER, Role.CLEANING_STAFF);
            });
        };
    }

    public EndpointGroup getRouteResource(String resourceName) {
        return switch (resourceName.toLowerCase()) {
            case "msg" -> () -> path("msg", () -> {
                ObjectNode on = objectMapper.createObjectNode();
                on.put("msg", "Hello World");
                get("hello", ctx -> ctx.json(on));
                post("echo", ctx -> ctx.result(ctx.body()));
            });
//
            case "auth" -> () -> path("auth", () -> {
                ObjectNode on = objectMapper.createObjectNode();
                on.put("msg","HELLO FROM THHE RESTRICTED AREA");
                post("register", securityController::register );
                post("login", securityController::login );
                put("change-password", securityController::changePassword, Role.USER, Role.ADMIN, Role.EMPLOYEE, Role.CLEANING_STAFF, Role.CLEANING_CLIENT, Role.SUBSCRIBER);
                get("protected",ctx->ctx.json(on).status(200),Role.USER);
            });
            default -> throw new IllegalArgumentException("Unknown resource name: " + resourceName);
        };
    }

    public enum Role implements RouteRole {
        ANYONE,USER,ADMIN,EMPLOYEE,CLEANING_STAFF,CLEANING_CLIENT,SUBSCRIBER
    }

}
