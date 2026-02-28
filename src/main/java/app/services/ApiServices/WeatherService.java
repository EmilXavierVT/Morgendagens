package app.services.ApiServices;

import app.dto.CityDTO;
import app.dto.WeatherDTO;
import app.utils.ApiFetcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class WeatherService {
        private static ObjectMapper objectMapper = ObjectMapperService.getMapper();

    public static WeatherDTO getWeather(String city){
        String url = "https://api.open-meteo.com/v1/forecast?latitude=*&longitude=$&daily=rain_sum&hourly=temperature_2m&timezone=Europe%2FBerlin&forecast_days=1";

        CityDTO cityDTO = getCity(city);
        String latitude = cityDTO.getLat();
        String longitude = cityDTO.getLon();

        url = url.replace("*", latitude);
        url = url.replace("$",longitude);

        WeatherDTO weatherDTO = null;
        JsonNode response = ApiFetcher.getApiDataWithMapper(url,objectMapper);

        try {
            weatherDTO = objectMapper.treeToValue(response, WeatherDTO.class);
            weatherDTO.setCityDTO(cityDTO);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return weatherDTO;
    }

    private static CityDTO getCity(String city){


        city = URLEncoder.encode(city, StandardCharsets.UTF_8);
        String cityURL = "https://api.dataforsyningen.dk/steder?q="+city+"&per_side=1";

        JsonNode response = ApiFetcher.getApiDataWithMapper(cityURL,objectMapper);

        try {
            CityDTO cityDTO = objectMapper.treeToValue(response, CityDTO[].class)[0];
            return  cityDTO;
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

    }
}
