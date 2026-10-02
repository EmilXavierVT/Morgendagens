package app.services.routeSecurity.routes;

import app.dto.UserDTO;
import app.dto.YouthIslandBookingDTO;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.exceptions.ApiException;
import app.services.dtoConverter.YouthIslandBookingMapper;
import app.services.entityServices.UserService;
import app.services.entityServices.YouthIslandBookingService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

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
        List<YouthIslandBookingDTO> bookings = new ArrayList<>();

        if (hasRole(authenticatedUser, "ADMIN")) {
            bookingService.getAll().forEach(booking -> bookings.add(bookingMapper.toDto(booking)));
        } else {
            User currentUser = requireCurrentUser(authenticatedUser);
            bookingService.getByOrderedByUserId(currentUser.getId())
                    .forEach(booking -> bookings.add(bookingMapper.toDto(booking)));
        }

        ctx.json(bookings);
    }

    public void getById(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        YouthIslandBooking booking = bookingService.getById(id);
        if (booking == null) throw new ApiException(404, "Youth Island booking not found");

        ensureVisibleTo(booking, authenticatedUser);
        ctx.json(bookingMapper.toDto(booking));
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

        YouthIslandBooking created = bookingService.create(booking);
        ctx.status(201).json(bookingMapper.toDto(created));
    }

    public void update(Context ctx) {
        UserDTO authenticatedUser = requireAuthenticatedUser(ctx);
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        YouthIslandBooking existing = bookingService.getById(id);
        if (existing == null) throw new ApiException(404, "Youth Island booking not found");
        ensureVisibleTo(existing, authenticatedUser);

        YouthIslandBookingDTO dto = ctx.bodyValidator(YouthIslandBookingDTO.class).get();
        YouthIslandBooking booking = bookingMapper.fromDto(dto);
        booking.setId(id);
        booking.setOrderedBy(existing.getOrderedBy());
        booking.setCreatedAt(existing.getCreatedAt());
        booking.getItems().forEach(item -> item.setId(null));

        YouthIslandBooking updated = bookingService.update(booking);
        ctx.json(bookingMapper.toDto(updated));
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

    private void ensureVisibleTo(YouthIslandBooking booking, UserDTO authenticatedUser) {
        if (hasRole(authenticatedUser, "ADMIN")) return;
        User currentUser = requireCurrentUser(authenticatedUser);
        if (!booking.getOrderedBy().getId().equals(currentUser.getId())) {
            throw new ApiException(403, "You can only access your own Youth Island bookings");
        }
    }

    private boolean hasRole(UserDTO user, String roleName) {
        return user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
