package app.dto;

import java.util.Set;

public  class UserDTOBuilder {
    private String email;
    private String password;
    private Set<String> roles;

    UserDTOBuilder() {
    }

    public UserDTOBuilder email(String email) {
        this.email = email;
        return this;
    }

    public UserDTOBuilder password(String password) {
        this.password = password;
        return this;
    }

    public UserDTOBuilder roles(Set<String> roles) {
        this.roles = roles;
        return this;
    }

    public UserDTO build() {
        return new UserDTO(this.email, this.password, this.roles);
    }

    public String toString() {
        return "UserDTO.UserDTOBuilder(username=" + this.email + ", password=" + this.password + ", roles=" + this.roles + ")";
    }
}