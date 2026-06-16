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
        if (hasRole(authenticatedUser, "CLEANING_CLIENT")) {
            for (CleaningAppointment appointment : cleaningAppointmentService.getByCleaningClientId(currentUser.getId())) {
                dtos.add(cleaningAppointmentMapper.toDto(appointment));
            }
            ctx.json(dtos);
            return;
        }

        for (CleaningAppointment appointment : cleaningAppointmentService.getVisibleToCleaningStaff(currentUser.getId())) {
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

        ensureOwnership(appointment, authenticatedUser, "You can only access your own appointments");

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

    public void update(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        CleaningAppointment existing = cleaningAppointmentService.getById(id);
        if (existing == null) {
            ctx.status(404).result("CleaningAppointment not found");
            return;
        }

        ensureOwnership(existing, authenticatedUser, "Cleaning staff can only update their own appointments");

        CleaningAppointmentDTO dto = ctx.bodyValidator(CleaningAppointmentDTO.class).get();
        dto.setId(id);
        applyAuthenticatedUserRules(dto, authenticatedUser);
        CleaningAppointment appointment = cleaningAppointmentMapper.fromDto(dto);
        CleaningAppointment updated = cleaningAppointmentService.update(appointment);
        ctx.json(cleaningAppointmentMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        CleaningAppointment existing = cleaningAppointmentService.getById(id);
        if (existing == null) {
            ctx.status(404).result("CleaningAppointment not found");
            return;
        }

        ensureOwnership(existing, authenticatedUser, "Cleaning staff can only delete their own appointments");
        cleaningAppointmentService.delete(id);
        ctx.status(204);
    }

    private void applyAuthenticatedUserRules(CleaningAppointmentDTO dto, UserDTO authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(401, "Not authenticated");
        }

        if (isAdmin(authenticatedUser)) {
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);

        if (hasRole(authenticatedUser, "CLEANING_CLIENT")) {
            if (dto.getCleaningClientId() == null) {
                dto.setCleaningClientId(currentUser.getId());
            } else if (!dto.getCleaningClientId().equals(currentUser.getId())) {
                throw new ApiException(403, "Cleaning clients can only create appointments for themselves");
            }

            return;
        }

        if (dto.getCleaningStaffId() == null) {
            dto.setCleaningStaffId(currentUser.getId());
            return;
        }

        if (!dto.getCleaningStaffId().equals(currentUser.getId())) {
            throw new ApiException(403, "Cleaning staff can only create appointments for themselves");
        }
    }

    private boolean isAdmin(UserDTO authenticatedUser) {
        return hasRole(authenticatedUser, "ADMIN");
    }

    private void ensureOwnership(CleaningAppointment appointment, UserDTO authenticatedUser, String message) {
        if (isAdmin(authenticatedUser)) {
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        boolean ownsAsClient = hasRole(authenticatedUser, "CLEANING_CLIENT")
                && appointment.getCleaningClient().getId().equals(currentUser.getId());
        boolean ownsAsStaff = hasRole(authenticatedUser, "CLEANING_STAFF")
                && (appointment.getCleaningStaff() == null
                || appointment.getCleaningStaff().getId().equals(currentUser.getId()));

        if (!ownsAsClient && !ownsAsStaff) {
            throw new ApiException(403, message);
        }
    }

    private boolean hasRole(UserDTO authenticatedUser, String roleName) {
        return authenticatedUser != null && authenticatedUser.getRoles().stream()
                .anyMatch(role -> role.equalsIgnoreCase(roleName));
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
