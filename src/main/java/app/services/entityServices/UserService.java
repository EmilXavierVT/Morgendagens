package app.services.entityServices;

import app.dao.UserDAO;
import app.dto.UserUpdateDTO;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;
import org.mindrot.jbcrypt.BCrypt;

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

public class UserService implements CrudService<User> {
    private final UserDAO userDAO;
    private final TenantService tenantService;
    private static final Set<String> EDITABLE_ROLES = Set.of(
            "USER", "ADMIN", "EMPLOYEE", "CLEANING_STAFF", "CLEANING_MANAGER",
            "CLEANING_CLIENT", "SUBSCRIBER", "FLEX", "YOUTH_ISLAND");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    public UserService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userDAO = new UserDAO(emf);
        this.tenantService = new TenantService(emf);
    }

    public User create(User user) {
        return userDAO.create(user);
    }

    public User getById(Long id) {
        return userDAO.getById(id);
    }

    public User update(User user) {
        return userDAO.update(user);
    }

    public User updateDetails(Long id, UserUpdateDTO dto) {
        User user = userDAO.getByIdWithMessagesAndRoles(id);
        if (user == null) throw new ApiException(404, "User not found");

        if (dto.getEmail() != null) {
            String email = dto.getEmail().trim();
            if (!EMAIL.matcher(email).matches()) throw new ApiException(400, "Enter a valid email address");
            if (userDAO.isEmailInUse(email, id)) throw new ApiException(409, "Email is already in use");
            user.setEmail(email);
        }
        if (dto.getZipCode() != null) {
            if (dto.getZipCode() < 0) throw new ApiException(400, "Zip code must be a non-negative number");
            user.setZipCode(dto.getZipCode());
        }
        if (dto.getTenantId() != null) {
            Tenant tenant = tenantService.getById(dto.getTenantId());
            if (tenant == null) throw new ApiException(400, "Tenant not found");
            user.setTenant(tenant);
        }
        if (dto.getRoles() != null) {
            if (dto.getRoles().isEmpty()
                    || dto.getRoles().stream().anyMatch(role -> role == null || !EDITABLE_ROLES.contains(role))) {
                throw new ApiException(400, "Choose at least one valid role");
            }
            Set<Role> roles = new HashSet<>();
            for (String role : dto.getRoles()) roles.add(new Role(role));
            user.setRoles(roles);
        }
        if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
            if (dto.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
                throw new ApiException(400, "Password must be at most 72 UTF-8 bytes");
            }
            user.setPassword(BCrypt.hashpw(dto.getPassword(), BCrypt.gensalt(12)));
        }
        if (dto.getFirstName() != null) user.setFirstName(dto.getFirstName().trim());
        if (dto.getLastName() != null) user.setLastName(dto.getLastName().trim());
        if (dto.getStreetName() != null) user.setStreetName(dto.getStreetName().trim());
        if (dto.getStreetNumber() != null) user.setStreetNumber(dto.getStreetNumber().trim());
        if (dto.getPhoneNumber() != null) user.setPhoneNumber(dto.getPhoneNumber().trim());

        // Update the existing account so omitted fields and message ownership stay intact.
        return userDAO.update(user);
    }

    public User delete(Long id) {
        return userDAO.delete(id);
    }

    public Set<User> getAll() {
        return userDAO.getAll();
    }

    public User setAdmin(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "ADMIN");
    }

    public User setEmployee(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "EMPLOYEE");
    }

    public User setCleaningStaff(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        userDAO.addUserRole(user.getEmail(), "EMPLOYEE");
        return userDAO.addUserRole(user.getEmail(), "CLEANING_STAFF");
    }

    public User setCleaningManager(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        userDAO.addUserRole(user.getEmail(), "USER");
        return userDAO.addUserRole(user.getEmail(), "CLEANING_MANAGER");
    }

    public User setCleaningClient(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "CLEANING_CLIENT");
    }

    public User setSubscriber(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "SUBSCRIBER");
    }

    public User setFlex(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "FLEX");
    }

    public User setYouthIsland(Long id) {
        User user = userDAO.getById(id);
        if (user == null) throw new ApiException(404, "User not found");
        return userDAO.addUserRole(user.getEmail(), "YOUTH_ISLAND");
    }

    public User getByEmail(String email) {
        return userDAO.getByEmailWithRoles(email);
    }

}
