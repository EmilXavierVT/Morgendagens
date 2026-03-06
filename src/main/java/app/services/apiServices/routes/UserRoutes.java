package app.services.apiServices.routes;

import app.dto.UserDTO;
import app.entities.User;
import app.services.dtoConverter.UserMapper;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserRoutes {
    private final UserService userService;
    private final UserMapper userMapper;

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
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        User user = userMapper.fromDto(dto);
        User created = userService.create(user);
        ctx.status(201).json(userMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        UserDTO dto = ctx.bodyValidator(UserDTO.class).get();
        dto.setId(id);
        User user = userMapper.fromDto(dto);
        User updated = userService.update(user);
        if (updated == null) {
            ctx.status(404).result("User not found");
            return;
        }
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
}
