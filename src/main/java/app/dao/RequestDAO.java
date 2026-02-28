package app.dao;

import app.entities.Request;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;
import java.util.Set;

public class RequestDAO implements IDAO<Request> {
    private final EntityManagerFactory emf;

    public RequestDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public Request create(Request request) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(request);
            em.getTransaction().commit();
            return request;
        }
    }

    @Override
    public Request getById(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            return em.find(Request.class, id);
        }
    }

    @Override
    public Request update(Request request) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Request updated = em.merge(request);
            em.getTransaction().commit();
            return updated;

        }
    }

    @Override
    public Request delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Request request = em.find(Request.class, id);
            if (request != null) {
                em.remove(request);
            }
            em.getTransaction().commit();
            return request;
        }
    }

    @Override
    public Set<Request> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery("SELECT r FROM Request r", Request.class).getResultList());
        }
    }
}
