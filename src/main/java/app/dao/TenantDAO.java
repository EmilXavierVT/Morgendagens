package app.dao;

import app.entities.Request;
import app.entities.Tenant;
import app.entities.User;
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
            Set<User> managedUsers = new HashSet<>();
            if (tenant.getUsers() != null) {
                for (User user : tenant.getUsers()) {
                    if (user == null) continue;
                    User managedUser = user;
                    if (user.getId() != null) {
                        managedUser = em.find(User.class, user.getId());
                        if (managedUser == null) {
                            throw new IllegalArgumentException("User with id " + user.getId() + " does not exist");
                        }
                    }
                    managedUser.setTenant(tenant);
                    managedUsers.add(managedUser);
                }
            }
            tenant.setUsers(managedUsers);
            Set<Request> managedRequests = new HashSet<>();
            if (tenant.getRequests() != null) {
                for (Request request : tenant.getRequests()) {
                    if (request == null) continue;
                    Request managedRequest = request;
                    if (request.getId() != null) {
                        managedRequest = em.find(Request.class, request.getId());
                        if (managedRequest == null) {
                            throw new IllegalArgumentException("Request with id " + request.getId() + " does not exist");
                        }
                    }
                    managedRequest.setTenant(tenant);
                    managedRequests.add(managedRequest);
                }
            }
            tenant.setRequests(managedRequests);
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
