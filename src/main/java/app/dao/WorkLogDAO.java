package app.dao;

import app.entities.User;
import app.entities.WorkLog;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WorkLogDAO implements IDAO<WorkLog> {
    private final EntityManagerFactory emf;

    public WorkLogDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public WorkLog create(WorkLog workLog) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            attachUser(em, workLog);
            em.persist(workLog);
            em.getTransaction().commit();
            return getById(workLog.getId());
        }
    }

    @Override
    public WorkLog getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT wl FROM WorkLog wl JOIN FETCH wl.user WHERE wl.id = :id",
                            WorkLog.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public WorkLog update(WorkLog workLog) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            WorkLog existing = em.find(WorkLog.class, workLog.getId());
            if (existing == null) {
                em.getTransaction().commit();
                return null;
            }

            attachUser(em, workLog);
            WorkLog updated = em.merge(workLog);
            em.getTransaction().commit();
            return getById(updated.getId());
        }
    }

    @Override
    public WorkLog delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            WorkLog workLog = em.find(WorkLog.class, id);
            if (workLog != null) {
                em.remove(workLog);
            }
            em.getTransaction().commit();
            return workLog;
        }
    }

    @Override
    public Set<WorkLog> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery(
                    "SELECT wl FROM WorkLog wl JOIN FETCH wl.user",
                    WorkLog.class
            ).getResultList());
        }
    }

    public List<WorkLog> getByUserId(Long userId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT wl FROM WorkLog wl JOIN FETCH wl.user WHERE wl.user.id = :userId ORDER BY wl.startTime",
                            WorkLog.class)
                    .setParameter("userId", userId)
                    .getResultList();
        }
    }

    private void attachUser(EntityManager em, WorkLog workLog) {
        User user = workLog.getUser();
        if (user == null || user.getId() == null) {
            throw new IllegalArgumentException("WorkLog must reference a user");
        }

        User managedUser = em.find(User.class, user.getId());
        if (managedUser == null) {
            throw new IllegalArgumentException("User with id " + user.getId() + " does not exist");
        }

        workLog.setUser(managedUser);
    }
}
