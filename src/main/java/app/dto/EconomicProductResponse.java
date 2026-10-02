package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class EconomicProductResponse {
    private String productNumber;
    private String name;
    private String description;
    private BigDecimal salesPrice;
    private Boolean barred;
    private OffsetDateTime lastUpdated;
    private ProductGroup productGroup;
    private String self;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class ProductGroup {
        private Integer productGroupNumber;
        private String name;
    }
}
