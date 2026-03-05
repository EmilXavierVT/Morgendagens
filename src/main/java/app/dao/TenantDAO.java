package app.dao;

import app.entities.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.TypedQuery;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TenantDAO implements IDAO<Tenant> {
    private final EntityManagerFactory emf;

    public TenantDAO(EntityManagerFactory emf) {
        this.emf = emf;
    }

    @Override
    public Tenant create(Tenant tenant) {

        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            em.persist(tenant);
            em.getTransaction().commit();
            return tenant;
        }
    }

    @Override
    public Tenant getById(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<Tenant> query = em.createQuery(
                    "SELECT DISTINCT t FROM Tenant t LEFT JOIN FETCH t.users LEFT JOIN FETCH t.requests WHERE t.id = :id",
                    Tenant.class);
            query.setParameter("id", id);
            List<Tenant> resultList = query.getResultList();
            return resultList.isEmpty() ? null : resultList.get(0);
        }
    }

    @Override
    public Tenant update(Tenant tenant) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant updated = em.merge(tenant);
            em.getTransaction().commit();
            return updated;
        }
    }

    @Override
    public Tenant delete(Long id) {
        try(EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            Tenant tenant = em.find(Tenant.class, id);
            if (tenant != null) {
                em.remove(tenant);
            }
            em.getTransaction().commit();
            return tenant;
        }
    }

    @Override
    public Set<Tenant> getAll() {
        try(EntityManager em = emf.createEntityManager()) {
            TypedQuery<Tenant> query = em.createQuery(
                    "SELECT DISTINCT t FROM Tenant t LEFT JOIN FETCH t.users LEFT JOIN FETCH t.requests",
                    Tenant.class);
            return new HashSet<>(query.getResultList());
        }
    }
}
