package app.services.routeSecurity.routes;

import app.dto.UserDTO;
import app.dto.WorkLogDTO;
import app.entities.WorkLog;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.dtoConverter.WorkLogMapper;
import app.services.entityServices.UserService;
import app.services.entityServices.WorkLogService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class WorkLogRoutes {
    private final WorkLogService workLogService;
    private final WorkLogMapper workLogMapper;
    private final UserService userService;

    public WorkLogRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.workLogService = new WorkLogService(emf);
        this.workLogMapper = new WorkLogMapper();
        this.userService = new UserService(emf);
    }

    public void getAll(Context ctx) {
        List<WorkLogDTO> dtos = new ArrayList<>();
        for (WorkLog workLog : workLogService.getAll()) {
            dtos.add(workLogMapper.toDto(workLog));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        WorkLog workLog = workLogService.getById(id);
        if (workLog == null) {
            ctx.status(404).result("WorkLog not found");
            return;
        }
        ctx.json(workLogMapper.toDto(workLog));
    }

    public void create(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        WorkLogDTO dto = ctx.bodyValidator(WorkLogDTO.class).get();
        applyAuthenticatedUserRules(dto, authenticatedUser);
        WorkLog workLog = workLogMapper.fromDto(dto);
        WorkLog created = workLogService.create(workLog);
        ctx.status(201).json(workLogMapper.toDto(created));
    }

    private void applyAuthenticatedUserRules(WorkLogDTO dto, UserDTO authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(401, "Not authenticated");
        }

        if (authenticatedUser.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase("ADMIN"))) {
            return;
        }

        User currentUser = userService.getByEmail(authenticatedUser.getEmail());
        if (currentUser == null) {
            throw new ApiException(401, "Authenticated user no longer exists");
        }

        if (dto.getUserId() == null) {
            dto.setUserId(currentUser.getId());
            return;
        }

        if (!dto.getUserId().equals(currentUser.getId())) {
            throw new ApiException(403, "Employees can only create worklogs for themselves");
        }
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        WorkLogDTO dto = ctx.bodyValidator(WorkLogDTO.class).get();
        dto.setId(id);
        WorkLog workLog = workLogMapper.fromDto(dto);
        WorkLog updated = workLogService.update(workLog);
        if (updated == null) {
            ctx.status(404).result("WorkLog not found");
            return;
        }
        ctx.json(workLogMapper.toDto(updated));
    }

    public void getByUserId(Context ctx) {
        Long userId = ctx.pathParamAsClass("userId", Long.class).get();
        List<WorkLogDTO> dtos = new ArrayList<>();
        for (WorkLog workLog : workLogService.getByUserId(userId)) {
            dtos.add(workLogMapper.toDto(workLog));
        }
        ctx.json(dtos);
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        WorkLog deleted = workLogService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("WorkLog not found");
            return;
        }
        ctx.status(204);
    }
}
