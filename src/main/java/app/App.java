package app;

import app.config.ApplicationConfig;
import app.config.HibernateConfig;
import app.services.routeSecurity.routes.Routes;
import app.utils.Populate;
import jakarta.persistence.EntityManagerFactory;

public class App {

    public static void initiate()
    {
         EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        Populate populate = new Populate(emf);

        populate.populate().forEach( (s,e) -> System.out.println(s + e));
    }

    public void javalinService(){

        Routes routes = new Routes();
//        initiate();

//        Javalin app = Javalin.create(
//                config ->{
//                    config.routes.apiBuilder(
//                            routes.getRoutes());
//                    config.bundledPlugins.enableRouteOverview("/routes");
//                    config.routes.exception(RuntimeException.class,
//                            (e, ctx) ->
//                                    ctx.status(400).json(e.getMessage()));
//
//                })
//                .start(7030);

        new ApplicationConfig()
                .security()
                .route(routes.getRouteResource("auth"))
                .route(routes.getRoutes())
                .routeOverview()
                .cors()
                .exceptions()
                .apiExceptions()
                .start(getPort());
    }

    private int getPort() {
        String configuredPort = System.getenv("PORT");
        if (configuredPort == null || configuredPort.isBlank()) {
            return 7030;
        }
        return Integer.parseInt(configuredPort.trim());
    }
}
