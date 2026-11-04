package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.util.Set;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserUpdateDTO {
    private Long tenantId;
    private String firstName;
    private String lastName;
    private String streetName;
    private String streetNumber;
    private Integer zipCode;
    private String email;
    private String phoneNumber;
    private Set<String> roles;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    @ToString.Exclude
    private String password;
}
