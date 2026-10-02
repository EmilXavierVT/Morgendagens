package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductDTO {
    private Long id;
    private String name;
    private String description;
    private double price;
    private int type;
    private String economicProductNumber;
    private String economicProductGroupName;
    private Integer economicProductGroupNumber;
    private boolean economicAvailable;
    private OffsetDateTime economicLastUpdated;
    private String economicSelf;
    private List<Long> productInRequestIds;
}
