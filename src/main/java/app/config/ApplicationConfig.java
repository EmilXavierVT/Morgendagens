package app.config;

import app.services.apiServices.routes.Routes;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.javalin.Javalin;
import io.javalin.config.JavalinConfig;
import io.javalin.http.HttpStatus;
import io.javalin.json.JavalinJackson;
import io.javalin.validation.ValidationException;

import java.util.Map;

public class ApplicationConfig {

    private final Routes routes;

    public ApplicationConfig(Routes routes) {
        this.routes = routes;
    }

    public void configuration(JavalinConfig config){
        config.bundledPlugins.enableRouteOverview("/routes");
        config.router.contextPath = "/api";
        config.jsonMapper(new JavalinJackson().updateMapper(objectMapper -> {
            objectMapper.registerModule(new JavaTimeModule());
            objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }));

        config.routes.apiBuilder(routes.getRoutes());
        config.routes.exception(ValidationException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST);
            ctx.json(Map.of(
                    "message", "Path parameter must be a number",
                    "path", ctx.path()
            ));
        });
        config.routes.exception(MismatchedInputException.class, (e, ctx) -> {
            ctx.status(HttpStatus.BAD_REQUEST).json("Invalid or Empty Request Body");
        });
    }

    public Javalin startServer(int port) {
        var app = Javalin.create(this::configuration);
        app.start(port);
        return app;
    }

    public void stopServer(Javalin app) {
        app.stop();
    }
}