package app;

import app.config.HibernateConfig;
import app.utils.Populate;
import io.javalin.http.util.JsonEscapeUtil;
import jakarta.persistence.EntityManagerFactory;

public class App {

    public static void initiate()
    {
         EntityManagerFactory emf = HibernateConfig.getEntityManagerFactory();

        Populate populate = new Populate(emf);

        populate.populate().forEach( (s,e) -> System.out.println(s + e));
    }
}
