package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthIslandBookingItemDTO {
    private Long id;
    private int guestCount;
    private LocalTime servingTime;
    private String productNumber;
    private String productName;
    private BigDecimal unitPriceOverride;
    private String menu;
    private String room;
}
