package app.dao;

import app.entities.ProductInRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.HashSet;
import java.util.Set;

public class ProductInRequestDAO implements IDAO<ProductInRequest> {
    private final EntityManagerFactory emf;

    public ProductInRequestDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public ProductInRequest create(ProductInRequest productInRequest) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(productInRequest);
            em.getTransaction().commit();
            return productInRequest;
        }
    }

    @Override
    public ProductInRequest getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.find(ProductInRequest.class, id);
        }
    }

    @Override
    public ProductInRequest update(ProductInRequest productInRequest) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ProductInRequest updated = em.merge(productInRequest);
            em.getTransaction().commit();
            return updated;
        }
    }

    @Override
    public ProductInRequest delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            ProductInRequest productInRequest = em.find(ProductInRequest.class, id);
            if (productInRequest != null) {
                em.remove(productInRequest);
            }
            em.getTransaction().commit();
            return productInRequest;
        }
    }

    @Override
    public Set<ProductInRequest> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery("SELECT p FROM ProductInRequest p", ProductInRequest.class)
                    .getResultList());
        }
    }
}
