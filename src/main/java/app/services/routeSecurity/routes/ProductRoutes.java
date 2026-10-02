package app.services.routeSecurity.routes;

import app.exceptions.ApiException;
import app.dto.ProductDTO;
import app.entities.Product;
import app.services.dtoConverter.ProductMapper;
import app.services.entityServices.ProductService;
import app.services.economic.EconomicProductCatalogService;
import app.services.economic.EconomicMirrorSyncService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductRoutes {
    private final ProductService productService;
    private final ProductMapper productMapper;
    private final EconomicProductCatalogService productCatalogService;
    private final EconomicMirrorSyncService mirrorSyncService;

    public ProductRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productService = new ProductService(emf);
        this.productMapper = new ProductMapper(emf);
        this.productCatalogService = new EconomicProductCatalogService();
        this.mirrorSyncService = new EconomicMirrorSyncService(emf);
    }

    public void getAll(Context ctx) {
        List<ProductDTO> products = new ArrayList<>();
        for (Product product : productService.getAvailableEconomicProducts()) {
            products.add(productMapper.toDto(product));
        }
        ctx.json(products);
    }

    public void getById(Context ctx) {
        String productNumber = ctx.pathParam("id");
        Product product = productService.getByEconomicProductNumber(productNumber);
        if (product == null || !product.isEconomicAvailable()) {
            throw new ApiException(404, "Product not found in local e-conomic mirror");
        }
        ctx.json(productMapper.toDto(product));
    }

    public void create(Context ctx) {
        throw new ApiException(409, "Products are managed in e-conomic");
    }

    public void update(Context ctx) {
        String productNumber = ctx.pathParam("id");
        ProductDTO update = ctx.bodyValidator(ProductDTO.class).get();
        ProductDTO updated = productCatalogService.updateProduct(productNumber, update);
        mirrorSyncService.upsertProduct(updated);
        ctx.json(updated);
    }

    public void delete(Context ctx) {
        throw new ApiException(409, "Products are managed in e-conomic");
    }
}
