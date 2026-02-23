package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherDTO {
    @JsonProperty("hourly")
    private HourlyDTO hourlyDTO;
    @JsonProperty("daily")
    private DailyDTO dailyDTO;

    private CityDTO cityDTO;



}
