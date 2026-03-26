package app.services.routeSecurity.routes;


import app.config.HibernateConfig;
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

        return () -> {
            get("/", ctx -> ctx.result("Hello Javalin World!"));

            path("/tenant", () -> {
                get("/all", tenantRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", tenantRoutes::create, Role.ADMIN);
                get("/{id}", tenantRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", tenantRoutes::update, Role.ADMIN);
                delete("/{id}", tenantRoutes::delete, Role.ADMIN);
            });

            path("/user", () -> {
                get("/all", userRoutes::getAll, Role.ADMIN);
                post("/", userRoutes::create, Role.ADMIN);
                get("/{id}", userRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", userRoutes::update, Role.USER, Role.ADMIN);
                delete("/{id}", userRoutes::delete, Role.ADMIN);
            });

            path("/product", () -> {
                get("/all", productRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", productRoutes::create, Role.ADMIN);
                get("/{id}", productRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", productRoutes::update, Role.ADMIN);
                delete("/{id}", productRoutes::delete, Role.ADMIN);
            });

            path("/product-in-requests", () -> {
                get("/all", productInRequestRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", productInRequestRoutes::create, Role.ADMIN);
                get("/{id}", productInRequestRoutes::getById, Role.USER, Role.ADMIN);
                put("/{id}", productInRequestRoutes::update, Role.ADMIN);
                delete("/{id}", productInRequestRoutes::delete, Role.ADMIN);
            });

            path("/request", () -> {
                get("/all", requestRoutes::getAll, Role.USER, Role.ADMIN);
                post("/", requestRoutes::create, Role.USER, Role.ADMIN);
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
                get("protected",ctx->ctx.json(on).status(200),Role.USER);
            });
            default -> throw new IllegalArgumentException("Unknown resource name: " + resourceName);
        };
    }

    public enum Role implements RouteRole {
        ANYONE,USER,ADMIN
    }

}
