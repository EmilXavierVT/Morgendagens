package app.dao;

import app.entities.ProductInRequest;
import app.entities.Request;
import app.entities.Tenant;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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

            attachTenant(em, request);
            attachProductInRequests(em, request);

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

            attachTenant(em, request);
            attachProductInRequests(em, request);

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

    private void attachTenant(EntityManager em, Request request) {
        Tenant tenant = request.getTenant();
        if (tenant != null && tenant.getId() != null) {
            Tenant managedTenant = em.find(Tenant.class, tenant.getId());
            if (managedTenant == null) {
                throw new IllegalArgumentException("Tenant with id " + tenant.getId() + " does not exist");
            }
            request.setTenant(managedTenant);
        }
    }

    private void attachProductInRequests(EntityManager em, Request request) {
        if (request.getProductsInRequest() == null) return;

        List<ProductInRequest> attached = new ArrayList<>();
        for (ProductInRequest pir : request.getProductsInRequest()) {
            if (pir == null) continue;

            if (pir.getId() != null) {
                ProductInRequest managedPir = em.find(ProductInRequest.class, pir.getId());
                if (managedPir == null) {
                    throw new IllegalArgumentException("ProductInRequest with id " + pir.getId() + " does not exist");
                }
                managedPir.setRequest(request);
                attached.add(managedPir);
            } else {
                pir.setRequest(request);
                attached.add(pir);
            }
        }
        request.setProductsInRequest(attached);
    }
}
