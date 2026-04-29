package app.dao;

import app.entities.Product;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

import java.util.HashSet;
import java.util.Set;

public class ProductDAO implements IDAO<Product> {
    private final EntityManagerFactory emf;

    public ProductDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public Product create(Product product) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(product);
            System.out.println(product);
            em.getTransaction().commit();
            System.out.println(product);
            return getByIdWithProductInRequests(product.getId());
        }
    }

    @Override
    public Product getById(Long id) {
        return getByIdWithProductInRequests(id);
    }

    @Override
    public Product update(Product product) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product updatedProduct = em.merge(product);
            em.getTransaction().commit();
            return getByIdWithProductInRequests(updatedProduct.getId());
        }
    }

    @Override
    public Product delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Product product = em.find(Product.class, id);
            if (product != null) {
                em.remove(product);
            }
            em.getTransaction().commit();
            return product;
        }
    }

    @Override
    public Set<Product> getAll() {
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<Product> query = em.createQuery(
                    "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productInRequests",
                    Product.class
            );
            return new HashSet<>(query.getResultList());
        }
    }

    public Product getByIdWithProductInRequests(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            TypedQuery<Product> query = em.createQuery(
                    "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.productInRequests WHERE p.id = :id",
                    Product.class
            );
            query.setParameter("id", id);
            return query.getSingleResult();
        } catch (NoResultException ex) {
            return null;
        }
    }
}
