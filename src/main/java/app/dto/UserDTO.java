package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private Long id;
    private Long tenantId;
    private int role;
    private String firstName;
    private String lastName;
    private int zipCode;
    private String email;
    private String password;
    private String phoneNumber;
    private List<Long> messageIds;
}
