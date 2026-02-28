package app.dao;

import app.entities.Message;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;
import java.util.Set;

public class MessageDAO implements IDAO<Message> {

    private final EntityManagerFactory emf;

    public MessageDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public Message create(Message message) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(message);
            em.getTransaction().commit();
            return message;
        }
    }

    @Override
    public Message getById(Long id) {

        try(EntityManager em = emf.createEntityManager()) {
            return em.find(Message.class, id);
        }
    }

    @Override
    public Message update(Message message) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Message updated = em.merge(message);
            em.getTransaction().commit();
            return updated;
        }
    }

    @Override
    public Message delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Message message = em.find(Message.class, id);
            if (message != null) {
                em.remove(message);
            }
            em.getTransaction().commit();
            return message;
        }
    }

    @Override
    public Set<Message> getAll() {
        try(EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery("SELECT m FROM Message m", Message.class).getResultList());
        }
    }
}
