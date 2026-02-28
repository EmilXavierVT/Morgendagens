package app.services.entityServices;

import app.dao.TenantDAO;
import app.entities.Tenant;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class TenantService implements CrudService<Tenant> {
    private final TenantDAO tenantDAO;

    public TenantService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.tenantDAO = new TenantDAO(emf);
    }

    public Tenant create(Tenant tenant) {
        return tenantDAO.create(tenant);
    }

    public Tenant getById(Long id) {
        return tenantDAO.getById(id);
    }

    public Tenant update(Tenant tenant) {
        return tenantDAO.update(tenant);
    }

    public Tenant delete(Long id) {
        return tenantDAO.delete(id);
    }

    public Set<Tenant> getAll() {
        return tenantDAO.getAll();
    }
}
