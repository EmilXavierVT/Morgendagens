package app.services.entityServices;

import app.dao.ProductDAO;
import app.entities.Product;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class ProductService implements CrudService<Product> {
    private final ProductDAO productDAO;

    public ProductService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productDAO = new ProductDAO(emf);
    }

    public Product create(Product product) {
        return productDAO.create(product);
    }

    public Product getById(Long id) {
        return productDAO.getById(id);
    }

    public Product update(Product product) {
        return productDAO.update(product);
    }

    public Product delete(Long id) {
        return productDAO.delete(id);
    }

    public Set<Product> getAll() {
        return productDAO.getAll();
    }
}
