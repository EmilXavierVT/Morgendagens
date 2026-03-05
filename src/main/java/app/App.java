package app;

import app.config.HibernateConfig;
import app.services.ApiServices.routes.Routes;
import app.utils.Populate;
import io.javalin.Javalin;
import io.javalin.http.util.JsonEscapeUtil;
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


        EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();
        System.out.println(emf.toString());
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
