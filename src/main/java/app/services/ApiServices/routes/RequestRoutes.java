package app.services.ApiServices.routes;

import app.dto.RequestDTO;
import app.entities.Request;
import app.services.dtoConverter.RequestMapper;
import app.services.entityServices.RequestService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class RequestRoutes {
    private final RequestService requestService;
    private final RequestMapper requestMapper;

    public RequestRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.requestService = new RequestService(emf);
        this.requestMapper = new RequestMapper(emf);
    }

    public void getAll(Context ctx) {
        List<RequestDTO> dtos = new ArrayList<>();
        for (Request request : requestService.getAll()) {
            dtos.add(requestMapper.toDto(request));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Request request = requestService.getById(id);
        if (request == null) {
            ctx.status(404).result("Request not found");
            return;
        }
        ctx.json(requestMapper.toDto(request));
    }

    public void create(Context ctx) {
        RequestDTO dto = ctx.bodyValidator(RequestDTO.class).get();
        Request request = requestMapper.fromDto(dto);
        Request created = requestService.create(request);
        ctx.status(201).json(requestMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        RequestDTO dto = ctx.bodyValidator(RequestDTO.class).get();
        dto.setId(id);
        Request request = requestMapper.fromDto(dto);
        Request updated = requestService.update(request);
        if (updated == null) {
            ctx.status(404).result("Request not found");
            return;
        }
        ctx.json(requestMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Request deleted = requestService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("Request not found");
            return;
        }
        ctx.status(204);
    }
}
