package app;

import app.dto.WeatherDTO;
import app.services.WeatherService;

public class Main {
    public static void main(String[] args) {
//        App.initiate();

        WeatherDTO weatherDTO = WeatherService.getWeather("Odense");
        System.out.println(weatherDTO);
    }
}
