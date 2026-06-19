package app.dao;

import app.entities.SubscriptionDeal;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SubscriptionDealDAO implements IDAO<SubscriptionDeal> {
    private final EntityManagerFactory emf;

    public SubscriptionDealDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public SubscriptionDeal create(SubscriptionDeal subscriptionDeal) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            attachUser(em, subscriptionDeal);
            em.persist(subscriptionDeal);
            em.getTransaction().commit();
            return getById(subscriptionDeal.getId());
        }
    }

    @Override
    public SubscriptionDeal getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT sd FROM SubscriptionDeal sd JOIN FETCH sd.user WHERE sd.id = :id",
                            SubscriptionDeal.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public SubscriptionDeal update(SubscriptionDeal subscriptionDeal) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            SubscriptionDeal existing = em.find(SubscriptionDeal.class, subscriptionDeal.getId());
            if (existing == null) {
                em.getTransaction().commit();
                return null;
            }

            attachUser(em, subscriptionDeal);
            SubscriptionDeal updated = em.merge(subscriptionDeal);
            em.getTransaction().commit();
            return getById(updated.getId());
        }
    }

    @Override
    public SubscriptionDeal delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            SubscriptionDeal subscriptionDeal = em.find(SubscriptionDeal.class, id);
            if (subscriptionDeal != null) {
                em.remove(subscriptionDeal);
            }
            em.getTransaction().commit();
            return subscriptionDeal;
        }
    }

    @Override
    public Set<SubscriptionDeal> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery(
                    "SELECT sd FROM SubscriptionDeal sd JOIN FETCH sd.user ORDER BY sd.id",
                    SubscriptionDeal.class
            ).getResultList());
        }
    }

    public List<SubscriptionDeal> getByUserId(Long userId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT sd FROM SubscriptionDeal sd JOIN FETCH sd.user WHERE sd.user.id = :userId ORDER BY sd.id",
                            SubscriptionDeal.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    private void attachUser(EntityManager em, SubscriptionDeal subscriptionDeal) {
        User user = subscriptionDeal.getUser();
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("SubscriptionDeal must reference a user");
        }

        User managedUser = em.find(User.class, user.getId());
        if (managedUser == null) {
            throw new IllegalArgumentException("User with id " + user.getId() + " does not exist");
        }

        subscriptionDeal.setUser(managedUser);
    }
}
