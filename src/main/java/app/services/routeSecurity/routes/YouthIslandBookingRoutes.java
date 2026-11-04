package app.services.routeSecurity.routes;

import app.dto.UserDTO;
import app.dto.YouthIslandBookingDTO;
import app.dto.YouthIslandBookingEditDTO;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingEdit;
import app.exceptions.ApiException;
import app.services.dtoConverter.YouthIslandBookingMapper;
import app.services.entityServices.UserService;
import app.services.entityServices.YouthIslandBookingService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class YouthIslandBookingRoutes {
    private final YouthIslandBookingService bookingService;
    private final YouthIslandBookingMapper bookingMapper;
    private final UserService userService;

    public YouthIslandBookingRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.bookingService = new YouthIslandBookingService(emf);
        this.bookingMapper = new YouthIslandBookingMapper();
        this.userService = new UserService(emf);
    }

    public void getAll(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        ensureReadableBy(authenticatedUser);
        List<YouthIslandBookingDTO> bookings = new ArrayList<>();

        bookingService.getAll().forEach(booking -> bookings.add(toDto(booking)));

        ctx.json(bookings);
    }

    public void getById(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        YouthIslandBooking booking = bookingService.getById(id);
        if (booking == null) throw new ApiException(404, "Youth Island booking not found");

        ensureReadableBy(authenticatedUser);
        ctx.json(toDto(booking));
    }

    public void create(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        User currentUser = requireCurrentUser(authenticatedUser);
        YouthIslandBookingDTO dto = ctx.bodyValidator(YouthIslandBookingDTO.class).get();
        YouthIslandBooking booking = bookingMapper.fromDto(dto);
        booking.setId(null);
        booking.setCreatedAt(null);
        booking.setOrderedBy(currentUser);
        booking.getItems().forEach(item -> item.setId(null));
        normalizeBookingDates(booking);

        YouthIslandBooking created = bookingService.create(booking);
        ctx.status(201).json(toDto(created));
    }

    public void update(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        YouthIslandBooking existing = bookingService.getById(id);
        if (existing == null) throw new ApiException(404, "Youth Island booking not found");
        ensureEditableBy(authenticatedUser);
        User editor = requireCurrentUser(authenticatedUser);

        YouthIslandBookingDTO dto = ctx.bodyValidator(YouthIslandBookingDTO.class).get();
        YouthIslandBooking booking = bookingMapper.fromDto(dto);
        booking.setId(id);
        booking.setOrderedBy(existing.getOrderedBy());
        booking.setCreatedAt(existing.getCreatedAt());
        booking.getItems().forEach(item -> item.setId(null));
        normalizeBookingDates(booking);

        YouthIslandBooking updated = bookingService.update(booking, editor);
        ctx.json(toDto(updated));
    }

    private void normalizeBookingDates(YouthIslandBooking booking) {
        LocalDateTime start = booking.getStartDateTime();
        LocalDateTime end = booking.getEndDateTime();

        if (start == null || end == null) {
            throw new ApiException(400, "Youth Island bookings must have a start and end datetime");
        }

        if (!end.isAfter(start)) {
            throw new ApiException(400, "Booking end datetime must be after the start datetime");
        }

        booking.setEventDate(start.toLocalDate());
    }

    private UserDTO requireAuthenticatedUser(Context ctx) {
        UserDTO user = ctx.attribute("user");
        if (user == null || user.getEmail() == null) throw new ApiException(401, "Not authenticated");
        return user;
    }

    private User requireCurrentUser(UserDTO authenticatedUser) {
        User user = userService.getByEmail(authenticatedUser.getEmail());
        if (user == null) throw new ApiException(401, "Authenticated user no longer exists");
        return user;
    }

    private void ensureReadableBy(UserDTO authenticatedUser) {
        if (hasRole(authenticatedUser, "ADMIN") || hasRole(authenticatedUser, "YOUTH_ISLAND")) return;
        throw new ApiException(403, "Youth Island access is required");
    }

    private void ensureEditableBy(UserDTO authenticatedUser) {
        ensureReadableBy(authenticatedUser);
    }

    private YouthIslandBookingDTO toDto(YouthIslandBooking booking) {
        YouthIslandBookingDTO dto = bookingMapper.toDto(booking);
        List<YouthIslandBookingEditDTO> editHistory = bookingService.getEditHistory(booking.getId()).stream()
                .map(this::toEditDto)
                .toList();
        dto.setEditHistory(editHistory);
        return dto;
    }

    private YouthIslandBookingEditDTO toEditDto(YouthIslandBookingEdit edit) {
        User editor = edit.getEditedBy();
        return new YouthIslandBookingEditDTO(
                edit.getId(),
                editor.getId(),
                buildName(editor),
                editor.getEmail(),
                edit.getEditedAt()
        );
    }

    private String buildName(User user) {
        String name = String.join(" ",
                user.getFirstName() == null ? "" : user.getFirstName().trim(),
                user.getLastName() == null ? "" : user.getLastName().trim()).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private boolean hasRole(UserDTO user, String roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
