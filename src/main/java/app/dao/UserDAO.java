package app.dao;
import app.entities.Message;
import app.entities.Role;
import app.entities.Tenant;
import app.entities.User;
import app.exceptions.ValidationException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;


import java.util.HashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class UserDAO implements IDAO<User>,ISecurityDAO {
    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public User create(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            List<Message> managedMessages = new ArrayList<>();
            for (Message message : user.getMessages()) {
                if (message == null) continue;
                Message managedMessage = message;
                if (message.getId() != null) {
                    managedMessage = em.find(Message.class, message.getId());
                    if (managedMessage == null) {
                        throw new IllegalArgumentException("Message with id " + message.getId() + " does not exist");
                    }
                }
                managedMessage.setUser(user);
                managedMessages.add(managedMessage);
            }
            user.setMessages(managedMessages);
            em.persist(user);
            em.getTransaction().commit();
            return getByIdWithMessages(user.getId());
        }
    }


    @Override
    public User getById(Long id) {
        return getByIdWithMessages(id);
    }

    @Override
    public User update(User user) {
         try(EntityManager em = emf.createEntityManager()) {
             em.getTransaction().begin();
             User updatedUser = em.merge(user);
             em.getTransaction().commit();
             return getByIdWithMessages(updatedUser.getId());
         }
    }

    @Override
    public User delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User user = em.find(User.class, id);
            if (user != null) {
                em.remove(user);
            }
            em.getTransaction().commit();
            return user;
        }
    }

    @Override
    public Set<User> getAll() {
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<User> query = em.createQuery(
                    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.messages",
                    User.class
            );
            return new HashSet<>(query.getResultList());
        }
    }

    public User getByIdWithMessages(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<User> query = em.createQuery(
                    "SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.messages WHERE u.id = :id",
                    User.class
            );
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }


//hashing

    @Override
    public User getVerifiedUser(String username, String password) throws ValidationException {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email", User.class);
            query.setParameter("email", username);
            User user;
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                throw new ValidationException("User not found");
            }
            if (user.verifyPassword(password)) {
                return user;
            } else {
                throw new ValidationException("User could not be validated");
            }
        }
    }

    @Override
    public User createUser(String username, String password) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            Tenant tenant = Tenant.builder()
                    .name(username)
                    .type("custom")
                    .status(1)
                    .build();
            em.persist(tenant);

            User user = new User(username, password);
            user.setTenant(tenant);

            Role userRole = em.find(Role.class, "USER");
            if (userRole == null) {
                userRole = new Role("USER");
                em.persist(userRole);
            }
            user.addRole(userRole);
            em.persist(user);

            em.getTransaction().commit();
            return user;
        }
    }

    @Override
    public Role createRole(String roleName) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Role role = em.find(Role.class, roleName);
            if (role == null) {
                role = new Role(roleName);
                em.persist(role);
            }
            em.getTransaction().commit();
            return role;
        }
    }

    @Override
    public User addUserRole(String email, String roleName) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<User> query = em.createQuery(
                    "SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email", User.class);
            query.setParameter("email", email);
            User user;
            try {
                user = query.getSingleResult();
            } catch (NoResultException e) {
                return null;
            }
            em.getTransaction().begin();
            Role role = em.find(Role.class, roleName);
            if (role == null) {
                role = new Role(roleName);
                em.persist(role);
            }
            user.addRole(role);
            em.merge(user);
            em.getTransaction().commit();
            return user;
        }
    }


}
