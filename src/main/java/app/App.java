package app;

import app.config.ThymeleafConfig;
import app.controller.SystemController;
import io.javalin.Javalin;
import io.javalin.rendering.template.JavalinThymeleaf;

public class App {
    public static void initiate()
    {
        Javalin app = Javalin.create(config ->
        {
            config.staticFiles.add("/public");
            config.fileRenderer(new JavalinThymeleaf(ThymeleafConfig.templateEngine()));
            config.staticFiles.add("/templates");
        }).start(7072);

        SystemController.addRoutes(app);
        // Routing
        // add controllers here

    }
}
