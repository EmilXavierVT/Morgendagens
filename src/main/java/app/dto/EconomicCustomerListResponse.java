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
public class EconomicCustomerListResponse {
    private List<EconomicCustomerResponse> collection = new ArrayList<>();

    public void setCollection(List<EconomicCustomerResponse> collection) {
        this.collection = collection == null ? new ArrayList<>() : collection;
    }
}
