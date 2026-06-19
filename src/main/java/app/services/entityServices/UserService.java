package app.services.entityServices;

import app.dao.UserDAO;
import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class UserService implements CrudService<User> {
    private final UserDAO userDAO;

    public UserService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userDAO = new UserDAO(emf);
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
        return userDAO.addUserRole(user.getEmail(), "CLEANING_STAFF");
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

    public User getByEmail(String email) {
        return userDAO.getByEmailWithRoles(email);
    }

}
