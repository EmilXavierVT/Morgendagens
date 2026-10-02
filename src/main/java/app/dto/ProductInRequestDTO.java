package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Time;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductInRequestDTO {
    private Long id;
    private Long requestId;
    private Long productId;
    private String productNumber;
    private String productName;
    private String productDescription;
    private BigDecimal unitPrice;
    private Time time;
    private int amount;
}
