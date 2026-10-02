package app.services.dtoConverter;

import app.dto.ProductInRequestDTO;
import app.dto.ProductDTO;
import app.entities.Product;
import app.entities.ProductInRequest;
import app.entities.Request;
import app.services.economic.EconomicProductCatalogService;
import app.services.entityServices.ProductService;
import app.services.entityServices.RequestService;
import jakarta.persistence.EntityManagerFactory;

import java.math.BigDecimal;

public class ProductInRequestMapper {
    private final RequestService requestService;
    private final ProductService productService;
    private final EconomicProductCatalogService productCatalogService;

    public ProductInRequestMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.requestService = new RequestService(emf);
        this.productService = new ProductService(emf);
        this.productCatalogService = new EconomicProductCatalogService();
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
        dto.setProductNumber(pir.getProductNumber() != null
                ? pir.getProductNumber()
                : prod != null ? prod.getEconomicProductNumber() : null);
        dto.setProductName(pir.getProductName() != null
                ? pir.getProductName()
                : prod != null ? prod.getName() : null);
        dto.setProductDescription(pir.getProductDescription() != null
                ? pir.getProductDescription()
                : prod != null ? prod.getDescription() : null);
        dto.setUnitPrice(pir.getUnitPrice() != null
                ? pir.getUnitPrice()
                : prod != null ? BigDecimal.valueOf(prod.getPrice()) : null);

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
        if (dto.getProductNumber() != null && !dto.getProductNumber().isBlank()) {
            ProductDTO product = productCatalogService.getByProductNumber(dto.getProductNumber());
            pir.setProductNumber(product.getEconomicProductNumber());
            pir.setProductName(product.getName());
            pir.setProductDescription(product.getDescription());
            pir.setUnitPrice(BigDecimal.valueOf(product.getPrice()));
        } else if (dto.getProductId() != null) {
            Product prod = productService.getById(dto.getProductId());
            pir.setProduct(prod);
        }
        return pir;
    }
}
