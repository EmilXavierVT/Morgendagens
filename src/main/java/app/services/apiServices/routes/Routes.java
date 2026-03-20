package app.services.apiServices.routes;


import app.config.HibernateConfig;
import io.javalin.apibuilder.EndpointGroup;
import jakarta.persistence.EntityManagerFactory;

import static io.javalin.apibuilder.ApiBuilder.*;

public class Routes {
    private final EntityManagerFactory emf;

    public Routes() {
        this(HibernateConfig.getEntityManagerFactory());
    }

    public Routes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
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
                get("/all", tenantRoutes::getAll);
                post("/", tenantRoutes::create);
                get("/{id}", tenantRoutes::getById);
                put("/{id}", tenantRoutes::update);
                delete("/{id}", tenantRoutes::delete);
            });

            path("/user", () -> {
                get("/all", userRoutes::getAll);
                post("/", userRoutes::create);
                get("/{id}", userRoutes::getById);
                put("/{id}", userRoutes::update);
                delete("/{id}", userRoutes::delete);
            });

            path("/product", () -> {
                get("/all", productRoutes::getAll);
                post("/", productRoutes::create);
                get("/{id}", productRoutes::getById);
                put("/{id}", productRoutes::update);
                delete("/{id}", productRoutes::delete);
            });

            path("/product-in-requests", () -> {
                get("/all", productInRequestRoutes::getAll);
                post("/", productInRequestRoutes::create);
                get("/{id}", productInRequestRoutes::getById);
                put("/{id}", productInRequestRoutes::update);
                delete("/{id}", productInRequestRoutes::delete);
            });

            path("/request", () -> {
                get("/all", requestRoutes::getAll);
                post("/", requestRoutes::create);
                get("/{id}", requestRoutes::getById);
                put("/{id}", requestRoutes::update);
                delete("/{id}", requestRoutes::delete);
            });

            path("/message", () -> {
                get("/all", messageRoutes::getAll);
                post("/", messageRoutes::create);
                get("/{id}", messageRoutes::getById);
                put("/{id}", messageRoutes::update);
                delete("/{id}", messageRoutes::delete);
            });
        };
    }
}
