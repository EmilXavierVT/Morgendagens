package app.services.apiServices.routes;

import app.dto.TenantDTO;
import app.entities.Tenant;
import app.services.dtoConverter.TenantMapper;
import app.services.entityServices.TenantService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class TenantRoutes {
    private final TenantService tenantService;
    private final TenantMapper tenantMapper;

    public TenantRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.tenantService = new TenantService(emf);
        this.tenantMapper = new TenantMapper(emf);
    }

    public void getAll(Context ctx){
        List<TenantDTO> dtos = new ArrayList<>();
        for (Tenant tenant : tenantService.getAll()) {
            dtos.add(tenantMapper.toDto(tenant));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx){
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Tenant tenant = tenantService.getById(id);
        if (tenant == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.json(tenantMapper.toDto(tenant));
    }

    public void delete(Context ctx){
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Tenant deleted = tenantService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.status(204);
    }

    public void update(Context ctx){
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        TenantDTO tenantdto = ctx.bodyValidator(TenantDTO.class).get();
        tenantdto.setId(id);
        Tenant tenant = tenantMapper.fromDto(tenantdto);
        Tenant updated = tenantService.update(tenant);
        if (updated == null) {
            ctx.status(404).result("Tenant not found");
            return;
        }
        ctx.json(tenantMapper.toDto(updated));
    }

    public void create(Context ctx){
        TenantDTO tenantdto = ctx.bodyValidator(TenantDTO.class).get();
        Tenant tenant = tenantMapper.fromDto(tenantdto);
        Tenant created = tenantService.create(tenant);
        ctx.status(201).json(tenantMapper.toDto(created));

    }
}
