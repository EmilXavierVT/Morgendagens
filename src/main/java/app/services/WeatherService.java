package app.services;

import app.dto.WeatherDTO;
import app.utils.ApiFetcher;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class WeatherService {
    public static WeatherDTO getWeather(String url){

        ObjectMapper objectMapper = ObjectMapperService.getMapper();
        WeatherDTO weatherDTO = null;

        JsonNode response = ApiFetcher.getApiDataWithMapper(url,objectMapper);

        try {
            weatherDTO = objectMapper.treeToValue(response, WeatherDTO.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return weatherDTO;
    }
}
