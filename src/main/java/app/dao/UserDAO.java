package app.dao;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;


import java.util.HashSet;
import java.util.Set;


public class UserDAO implements IDAO<User> {
    private final EntityManagerFactory emf;

    public UserDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public User create(User user) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            user.getMessages().forEach(message -> message.setUser(user));
            em.persist(user);
            em.getTransaction().commit();
        return user;
        }
    }

    @Override
    public User getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(User.class, id);

        }
    }

    @Override
    public User update(User user) {
         try(EntityManager em = emf.createEntityManager()) {
             em.getTransaction().begin();
             User updatedUser = em.merge(user);
             em.getTransaction().commit();
             return updatedUser;
         }
    }

    @Override
    public User delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            User user = getById(id);
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
            TypedQuery<User> query = em.createQuery("SELECT u FROM User u", User.class);
            return new HashSet<>(query.getResultList());
        }
    }
}
