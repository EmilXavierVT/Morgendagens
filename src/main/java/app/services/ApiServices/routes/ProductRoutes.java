package app.services.ApiServices.routes;

import app.dto.ProductDTO;
import app.entities.Product;
import app.services.dtoConverter.ProductMapper;
import app.services.entityServices.ProductService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductRoutes {
    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productService = new ProductService(emf);
        this.productMapper = new ProductMapper(emf);
    }

    public void getAll(Context ctx) {
        List<ProductDTO> dtos = new ArrayList<>();
        for (Product product : productService.getAll()) {
            dtos.add(productMapper.toDto(product));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Product product = productService.getById(id);
        if (product == null) {
            ctx.status(404).result("Product not found");
            return;
        }
        ctx.json(productMapper.toDto(product));
    }

    public void create(Context ctx) {
        ProductDTO dto = ctx.bodyValidator(ProductDTO.class).get();
        Product product = productMapper.fromDto(dto);
        Product created = productService.create(product);
        ctx.status(201).json(productMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ProductDTO dto = ctx.bodyValidator(ProductDTO.class).get();
        dto.setId(id);
        Product product = productMapper.fromDto(dto);
        Product updated = productService.update(product);
        if (updated == null) {
            ctx.status(404).result("Product not found");
            return;
        }
        ctx.json(productMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Product deleted = productService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("Product not found");
            return;
        }
        ctx.status(204);
    }
}
