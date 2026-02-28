package app.services.entityServices;

import app.dao.UserDAO;
import app.entities.User;
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

}
