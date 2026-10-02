package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
@NoArgsConstructor
public class EconomicProductListResponse {
    private List<EconomicProductResponse> collection = new ArrayList<>();

    public void setCollection(List<EconomicProductResponse> collection) {
        this.collection = collection == null ? new ArrayList<>() : collection;
    }
}
