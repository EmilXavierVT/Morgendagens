package app.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class RequestDTO {
    private Long id;
    private Long tenantId;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String location;
    private int status;
    private int type;
    private List<Long> productInRequestIds;
    private WeatherDTO weatherDTO;
}
