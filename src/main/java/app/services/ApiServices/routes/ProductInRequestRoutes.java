package app.services.ApiServices.routes;

import app.dto.ProductInRequestDTO;
import app.entities.ProductInRequest;
import app.services.dtoConverter.ProductInRequestMapper;
import app.services.entityServices.ProductInRequestService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProductInRequestRoutes {
    private final ProductInRequestService productInRequestService;
    private final ProductInRequestMapper productInRequestMapper;

    public ProductInRequestRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.productInRequestService = new ProductInRequestService(emf);
        this.productInRequestMapper = new ProductInRequestMapper(emf);
    }

    public void getAll(Context ctx) {
        List<ProductInRequestDTO> dtos = new ArrayList<>();
        for (ProductInRequest pir : productInRequestService.getAll()) {
            dtos.add(productInRequestMapper.toDto(pir));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ProductInRequest pir = productInRequestService.getById(id);
        if (pir == null) {
            ctx.status(404).result("ProductInRequest not found");
            return;
        }
        ctx.json(productInRequestMapper.toDto(pir));
    }

    public void create(Context ctx) {
        ProductInRequestDTO dto = ctx.bodyValidator(ProductInRequestDTO.class).get();
        ProductInRequest pir = productInRequestMapper.fromDto(dto);
        ProductInRequest created = productInRequestService.create(pir);
        ctx.status(201).json(productInRequestMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ProductInRequestDTO dto = ctx.bodyValidator(ProductInRequestDTO.class).get();
        dto.setId(id);
        ProductInRequest pir = productInRequestMapper.fromDto(dto);
        ProductInRequest updated = productInRequestService.update(pir);
        if (updated == null) {
            ctx.status(404).result("ProductInRequest not found");
            return;
        }
        ctx.json(productInRequestMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        ProductInRequest deleted = productInRequestService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("ProductInRequest not found");
            return;
        }
        ctx.status(204);
    }
}
