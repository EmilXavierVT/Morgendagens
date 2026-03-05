package app.services.dtoConverter;

import app.dto.ProductDTO;
import app.entities.Product;
import app.entities.ProductInRequest;
import app.services.entityServices.ProductInRequestService;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductMapper {
    private final ProductInRequestService productInRequestService;

    public ProductMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productInRequestService = new ProductInRequestService(emf);
    }

    public ProductDTO toDto(Product product) {
        if (product == null) return null;
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setType(product.getType());

        List<Long> ids = new ArrayList<>();
        if (product.getProductInRequests() != null) {
            for (ProductInRequest pir : product.getProductInRequests()) {
                if (pir != null && pir.getId() != null) {
                    ids.add(pir.getId());
                }
            }
        }
        dto.setProductInRequestIds(ids);
        return dto;
    }

    public Product fromDto(ProductDTO dto) {
        if (dto == null) return null;
        Product product = new Product();
        product.setId(dto.getId());
        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setType(dto.getType());

        if (dto.getProductInRequestIds() != null) {
            List<ProductInRequest> list = new ArrayList<>();
            for (Long id : dto.getProductInRequestIds()) {
                if (id == null) continue;
                ProductInRequest pir = productInRequestService.getById(id);
                if (pir != null) {
                    pir.setProduct(product);
                    list.add(pir);
                }
            }
            product.setProductInRequests(list);
        }
        return product;
    }
}
