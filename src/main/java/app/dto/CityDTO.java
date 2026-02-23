package app.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CityDTO {
    @JsonProperty("primærtnavn")
    private String name;
    @JsonProperty("visueltcenter")
    private String[] center;

    private String lat;
    private String lon;

    @JsonCreator
    public CityDTO(@JsonProperty("primærtnavn") String name, @JsonProperty("visueltcenter") String[] center) {
        this.name = name;
        this.center = center;
        this.lat = center[0];
        this.lon = center[1];
    }
}
