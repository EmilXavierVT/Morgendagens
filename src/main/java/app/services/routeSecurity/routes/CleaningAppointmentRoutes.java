package app.services.routeSecurity.routes;

import app.dto.CleaningAppointmentDTO;
import app.dto.UserDTO;
import app.entities.CleaningAppointment;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.dtoConverter.CleaningAppointmentMapper;
import app.services.entityServices.CleaningAppointmentService;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class CleaningAppointmentRoutes {
    private final CleaningAppointmentService cleaningAppointmentService;
    private final CleaningAppointmentMapper cleaningAppointmentMapper;
    private final UserService userService;

    public CleaningAppointmentRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.cleaningAppointmentService = new CleaningAppointmentService(emf);
        this.cleaningAppointmentMapper = new CleaningAppointmentMapper();
        this.userService = new UserService(emf);
    }

    public void getAll(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        List<CleaningAppointmentDTO> dtos = new ArrayList<>();

        if (isAdmin(authenticatedUser)) {
            for (CleaningAppointment appointment : cleaningAppointmentService.getAll()) {
                dtos.add(cleaningAppointmentMapper.toDto(appointment));
            }
            ctx.json(dtos);
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        for (CleaningAppointment appointment : cleaningAppointmentService.getByCleaningStaffId(currentUser.getId())) {
            dtos.add(cleaningAppointmentMapper.toDto(appointment));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        CleaningAppointment appointment = cleaningAppointmentService.getById(id);
        if (appointment == null) {
            ctx.status(404).result("CleaningAppointment not found");
            return;
        }

        if (!isAdmin(authenticatedUser)) {
            User currentUser = getCurrentUser(authenticatedUser);
            if (!appointment.getCleaningStaff().getId().equals(currentUser.getId())) {
                throw new ApiException(403, "Cleaning staff can only access their own appointments");
            }
        }

        ctx.json(cleaningAppointmentMapper.toDto(appointment));
    }

    public void create(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        CleaningAppointmentDTO dto = ctx.bodyValidator(CleaningAppointmentDTO.class).get();
        applyAuthenticatedUserRules(dto, authenticatedUser);
        CleaningAppointment appointment = cleaningAppointmentMapper.fromDto(dto);
        CleaningAppointment created = cleaningAppointmentService.create(appointment);
        ctx.status(201).json(cleaningAppointmentMapper.toDto(created));
    }

    private void applyAuthenticatedUserRules(CleaningAppointmentDTO dto, UserDTO authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(401, "Not authenticated");
        }

        if (isAdmin(authenticatedUser)) {
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        if (dto.getCleaningStaffId() == null) {
            dto.setCleaningStaffId(currentUser.getId());
            return;
        }

        if (!dto.getCleaningStaffId().equals(currentUser.getId())) {
            throw new ApiException(403, "Cleaning staff can only create appointments for themselves");
        }
    }

    private boolean isAdmin(UserDTO authenticatedUser) {
        return authenticatedUser != null && authenticatedUser.getRoles().stream()
                .anyMatch(role -> role.equalsIgnoreCase("ADMIN"));
    }

    private User getCurrentUser(UserDTO authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(401, "Not authenticated");
        }

        User currentUser = userService.getByEmail(authenticatedUser.getEmail());
        if (currentUser == null) {
            throw new ApiException(401, "Authenticated user no longer exists");
        }
        return currentUser;
    }
}
