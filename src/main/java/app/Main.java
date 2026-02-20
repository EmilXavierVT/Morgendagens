package app;

import app.dto.WeatherDTO;
import app.services.WeatherService;

public class Main {
    public static void main(String[] args) {
//        App.initiate();

        WeatherDTO weatherDTO = WeatherService.getWeather("https://api.open-meteo.com/v1/forecast?latitude=55.6759&longitude=12.5655&daily=rain_sum&hourly=temperature_2m&timezone=Europe%2FBerlin&forecast_days=1");
        System.out.println(weatherDTO);
    }
}
