package app;

import app.config.HibernateConfig;
import app.services.apiServices.routes.Routes;
import io.javalin.Javalin;
import jakarta.persistence.EntityManagerFactory;

public class App {

//    public static void initiate()
//    {
//         EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
//
//        Populate populate = new Populate(emf);
//
//        populate.populate().forEach( (s,e) -> System.out.println(s + e));
//    }

    public void javalinService(){

        Routes routes = new Routes();

        Javalin app = Javalin.create(
                config ->{
                    config.routes.apiBuilder(
                            routes.getRoutes());
                    config.bundledPlugins.enableRouteOverview("/routes");
                    config.routes.exception(RuntimeException.class,
                            (e, ctx) ->
                                    ctx.status(400).json(e.getMessage()));

                })
                .start(7030);
    }
}
