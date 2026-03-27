package app.services.entityServices;

import app.dao.RequestDAO;
import app.entities.Request;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Set;

public class RequestService implements CrudService<Request> {
    private final RequestDAO requestDAO;

    public RequestService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.requestDAO = new RequestDAO(emf);
    }

    public Request create(Request request) {
        return requestDAO.create(request);
    }

    public Request getById(Long id) {
        return requestDAO.getById(id);
    }

    public Request update(Request request) {
        return requestDAO.update(request);
    }

    public Request delete(Long id) {
        return requestDAO.delete(id);
    }

    public Set<Request> getAll() {
        return requestDAO.getAll();
    }

    public List<Request> getByUserId(Long userId) {
        return requestDAO.getByUserId(userId);
    }
}
