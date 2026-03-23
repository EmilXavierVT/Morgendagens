package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserDTO {
    private Long id;
    private Long tenantId;

    private String firstName;
    private String lastName;
    private int zipCode;
    private String email;
    private String password;
    private String phoneNumber;
    private List<Long> messageIds;
    Set<String> roles = new HashSet();

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        } else if (o != null && this.getClass() == o.getClass()) {
            UserDTO dto = (UserDTO)o;
            return Objects.equals(this.email, dto.email) && Objects.equals(this.roles, dto.roles);
        } else {
            return false;
        }
    }

    public UserDTO(String email, String password) {
        this.email = email;
        this.password = password;
    }

    public UserDTO(String email, Set<String> roles) {
        this.email = email;
        this.roles = roles;
    }

    public int hashCode() {
        return Objects.hash(new Object[]{this.email, this.roles});
    }
    public String toString() {
        String var10000 = this.getEmail();
        return "UserDTO(email=" + var10000 + ", password=" + this.getPassword() + ", roles=" + this.getRoles() + ")";
    }

    public UserDTO(String email, String password, Set<String> roles) {
        this.email = email;
        this.password = password;
        this.roles = roles;
    }

}
