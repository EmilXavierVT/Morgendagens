package app.services.routeSecurity.routes;

import app.dto.UserDTO;
import app.dto.UserUpdateDTO;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.dtoConverter.UserMapper;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class UserRoutes {
    private final UserService userService;
    private final UserMapper userMapper;
    private static final Logger logger = LoggerFactory.getLogger(UserRoutes.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app.services.apiServices.routes");

    public UserRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userService = new UserService(emf);
        this.userMapper = new UserMapper(emf);
    }

    public void getAll(Context ctx) {
        List<UserDTO> dtos = new ArrayList<>();
        for (User user : userService.getAll()) {
            dtos.add(userMapper.toDto(user));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.getById(id);
        if (user == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.json(userMapper.toDto(user));
    }

    public void create(Context ctx) {
        logger.info("Creating user");
        debugLogger.info("Creating user");
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        User user = userMapper.fromDto(dto);
        User created = userService.create(user);
        ctx.status(201).json(userMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        UserDTO actor = ctx.attribute("user");
        if (actor == null) throw new ApiException(401, "Not authenticated");
        User existing = userService.getById(id);
        if (existing == null) throw new ApiException(404, "User not found");

        UserUpdateDTO dto = ctx.bodyValidator(UserUpdateDTO.class).get();
        boolean admin = actor.getRoles().contains("ADMIN");
        if (!admin) {
            if (!Objects.equals(actor.getEmail(), existing.getEmail())) {
                throw new ApiException(403, "Only administrators can edit other users");
            }
            if ((dto.getRoles() != null && !dto.getRoles().equals(existing.getRolesAsStrings()))
                    || (dto.getTenantId() != null && !dto.getTenantId().equals(existing.getTenant().getId()))) {
                throw new ApiException(403, "Only administrators can change roles or tenant");
            }
            if (dto.getPassword() != null && !dto.getPassword().isBlank()) {
                throw new ApiException(403, "Use change-password with your current password");
            }
        }
        User updated = userService.updateDetails(id, dto);
        ctx.json(userMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User deleted = userService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("User not found");
            return;
        }
        ctx.status(204);
    }

    public void setAdmin(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setAdmin(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setEmployee(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setEmployee(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningStaff(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setCleaningStaff(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningManager(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setCleaningManager(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setCleaningClient(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setCleaningClient(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setSubscriber(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setSubscriber(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setFlex(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setFlex(id);
        ctx.json(userMapper.toDto(user));
    }

    public void setYouthIsland(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        User user = userService.setYouthIsland(id);
        ctx.json(userMapper.toDto(user));
    }
}
