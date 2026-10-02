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
public class EconomicCustomerResponse {
    private Integer customerNumber;
    private String name;
    private String email;
    private String address;
    private String city;
    private String zip;
    private String country;
    private String mobilePhone;
    private String telephoneAndFaxNumber;
    private String corporateIdentificationNumber;
    private String vatNumber;
    private BigDecimal balance;
    private Boolean barred;
    private OffsetDateTime lastUpdated;
    private CustomerGroup customerGroup;
    private String self;

    @JsonIgnoreProperties(ignoreUnknown = true)
    @Getter
    @Setter
    @NoArgsConstructor
    public static class CustomerGroup {
        private Integer customerGroupNumber;
        private String name;
    }
}
