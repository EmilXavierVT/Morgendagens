package app.services.dtoConverter;

import app.dto.ProductInRequestDTO;
import app.entities.Product;
import app.entities.ProductInRequest;
import app.entities.Request;
import app.services.entityServices.ProductService;
import app.services.entityServices.RequestService;
import jakarta.persistence.EntityManagerFactory;

public class ProductInRequestMapper {
    private final RequestService requestService;
    private final ProductService productService;

    public ProductInRequestMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.requestService = new RequestService(emf);
        this.productService = new ProductService(emf);
    }

    public ProductInRequestDTO toDto(ProductInRequest pir) {
        if (pir == null) return null;
        ProductInRequestDTO dto = new ProductInRequestDTO();
        dto.setId(pir.getId());
        dto.setTime(pir.getTime());
        dto.setAmount(pir.getAmount());

        Request req = pir.getRequest();
        dto.setRequestId(req != null ? req.getId() : null);

        Product prod = pir.getProduct();
        dto.setProductId(prod != null ? prod.getId() : null);

        return dto;
    }

    public ProductInRequest fromDto(ProductInRequestDTO dto) {
        if (dto == null) return null;
        ProductInRequest pir = new ProductInRequest();
        pir.setId(dto.getId());
        pir.setTime(dto.getTime());
        pir.setAmount(dto.getAmount());

        if (dto.getRequestId() != null) {
            Request req = requestService.getById(dto.getRequestId());
            pir.setRequest(req);
        }
        if (dto.getProductId() != null) {
            Product prod = productService.getById(dto.getProductId());
            pir.setProduct(prod);
        }
        return pir;
    }
}
