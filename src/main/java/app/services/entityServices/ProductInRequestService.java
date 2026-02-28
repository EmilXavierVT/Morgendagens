package app.services.entityServices;

import app.dao.ProductInRequestDAO;
import app.entities.ProductInRequest;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class ProductInRequestService implements CrudService<ProductInRequest> {
    private final ProductInRequestDAO productInRequestDAO;

    public ProductInRequestService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productInRequestDAO = new ProductInRequestDAO(emf);
    }

    public ProductInRequest create(ProductInRequest productInRequest) {
        return productInRequestDAO.create(productInRequest);
    }

    public ProductInRequest getById(Long id) {
        return productInRequestDAO.getById(id);
    }

    public ProductInRequest update(ProductInRequest productInRequest) {
        return productInRequestDAO.update(productInRequest);
    }

    public ProductInRequest delete(Long id) {
        return productInRequestDAO.delete(id);
    }

    public Set<ProductInRequest> getAll() {
        return productInRequestDAO.getAll();
    }
}
