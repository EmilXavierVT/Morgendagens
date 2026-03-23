package app.config;

import app.entities.*;
import org.hibernate.cfg.Configuration;

final class EntityRegistry {

    private EntityRegistry() {}

    static void registerEntities(Configuration configuration) {
        configuration.addAnnotatedClass(User.class);
        configuration.addAnnotatedClass(Product.class);
        configuration.addAnnotatedClass(Tenant.class);
        configuration.addAnnotatedClass(Request.class);
        configuration.addAnnotatedClass(ProductInRequest.class);
        configuration.addAnnotatedClass(Message.class);
        configuration.addAnnotatedClass(Role.class);
    }
}
