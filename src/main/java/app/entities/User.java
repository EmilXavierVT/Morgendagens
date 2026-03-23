package app.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.mindrot.jbcrypt.BCrypt;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "users")

public class User implements IEntity, ISecurityUser{
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    @ToString.Exclude
    private Tenant tenant;

    private String firstName;
    private String lastName;
    private int zipCode;
    @Column(unique = true)
    private String email;
    private String password;
    private String phoneNumber;

    @OneToMany(mappedBy = "user", cascade = {CascadeType.PERSIST, CascadeType.MERGE}, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<Message> messages = new ArrayList<>();

    @ManyToMany(fetch = FetchType.EAGER, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_name")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
    public User(String email, String password ){
        String salt = BCrypt.gensalt(12);
        String hashedPassword = BCrypt.hashpw(password, salt);
        this.email = email;
        this.password = hashedPassword;
        this.roles = new HashSet<>();
        this.messages = new ArrayList<>();
    }


    @Override
    public Set<String> getRolesAsStrings() {

        return this.roles.stream()
                .map((role)->role.getRoleName())
                .collect(Collectors.toSet());
    }

    @Override
    public boolean verifyPassword(String pw) {
        return BCrypt.checkpw(pw, this.password);
    }

    @Override
    public void addRole(Role role) {
        this.roles.add(role);
    }

    @Override
    public void removeRole(String role) {

    }

}
