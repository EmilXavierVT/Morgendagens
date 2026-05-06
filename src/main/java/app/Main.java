package app;

public class Main {
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            System.err.println("Uncaught exception on thread " + thread.getName());
            throwable.printStackTrace(System.err);
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() ->
                System.err.println("JVM shutdown hook triggered. If no stack trace appears before this, the container/server likely stopped the process externally.")
        ));

        logRuntimeInfo();

//        App.initiate();

//        WeatherDTO weatherDTO = WeatherService.getWeather("Århus");
//        System.out.println(weatherDTO);
        App app = new App();
        app.javalinService();
    }

    private static void logRuntimeInfo() {
        Runtime runtime = Runtime.getRuntime();
        System.out.println("Starting Morgendagens");
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("Available processors: " + runtime.availableProcessors());
        System.out.println("Max memory MB: " + runtime.maxMemory() / 1024 / 1024);
        System.out.println("PORT env: " + valueOrDefault(System.getenv("PORT"), "7030"));
        System.out.println("DEPLOYED env present: " + (System.getenv("DEPLOYED") != null));
        System.out.println("CONNECTION_STR env present: " + (System.getenv("CONNECTION_STR") != null));
    }

    private static String valueOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }
}
