package app.services.routeSecurity.routes;

import app.dto.SubscriptionDealDTO;
import app.dto.UserDTO;
import app.entities.SubscriptionDeal;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.dtoConverter.SubscriptionDealMapper;
import app.services.entityServices.SubscriptionDealService;
import app.services.entityServices.UserService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class SubscriptionDealRoutes {
    private final SubscriptionDealService subscriptionDealService;
    private final SubscriptionDealMapper subscriptionDealMapper;
    private final UserService userService;

    public SubscriptionDealRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.subscriptionDealService = new SubscriptionDealService(emf);
        this.subscriptionDealMapper = new SubscriptionDealMapper();
        this.userService = new UserService(emf);
    }

    public void getAll(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        List<SubscriptionDealDTO> dtos = new ArrayList<>();

        if (hasFullAccess(authenticatedUser)) {
            for (SubscriptionDeal subscriptionDeal : subscriptionDealService.getAll()) {
                dtos.add(subscriptionDealMapper.toDto(subscriptionDeal));
            }
            ctx.json(dtos);
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        for (SubscriptionDeal subscriptionDeal : subscriptionDealService.getByUserId(currentUser.getId())) {
            dtos.add(subscriptionDealMapper.toDto(subscriptionDeal));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        SubscriptionDeal subscriptionDeal = subscriptionDealService.getById(id);
        if (subscriptionDeal == null) {
            ctx.status(404).result("SubscriptionDeal not found");
            return;
        }

        ensureOwnership(subscriptionDeal, authenticatedUser);
        ctx.json(subscriptionDealMapper.toDto(subscriptionDeal));
    }

    public void create(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        SubscriptionDealDTO dto = ctx.bodyValidator(SubscriptionDealDTO.class).get();
        applyAuthenticatedUserRules(dto, authenticatedUser);
        SubscriptionDeal created = subscriptionDealService.create(subscriptionDealMapper.fromDto(dto));
        ctx.status(201).json(subscriptionDealMapper.toDto(created));
    }

    public void update(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        SubscriptionDeal existing = subscriptionDealService.getById(id);
        if (existing == null) {
            ctx.status(404).result("SubscriptionDeal not found");
            return;
        }

        ensureOwnership(existing, authenticatedUser);

        SubscriptionDealDTO dto = ctx.bodyValidator(SubscriptionDealDTO.class).get();
        dto.setId(id);
        applyAuthenticatedUserRules(dto, authenticatedUser);
        SubscriptionDeal updated = subscriptionDealService.update(subscriptionDealMapper.fromDto(dto));
        ctx.json(subscriptionDealMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        SubscriptionDeal existing = subscriptionDealService.getById(id);
        if (existing == null) {
            ctx.status(404).result("SubscriptionDeal not found");
            return;
        }

        ensureOwnership(existing, authenticatedUser);
        subscriptionDealService.delete(id);
        ctx.status(204);
    }

    private void applyAuthenticatedUserRules(SubscriptionDealDTO dto, UserDTO authenticatedUser) {
        if (authenticatedUser == null) {
            throw new ApiException(401, "Not authenticated");
        }

        if (hasFullAccess(authenticatedUser)) {
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        if (dto.getUserId() == null) {
            dto.setUserId(currentUser.getId());
            return;
        }

        if (!dto.getUserId().equals(currentUser.getId())) {
            throw new ApiException(403, "Subscribers can only manage their own subscription deals");
        }
    }

    private void ensureOwnership(SubscriptionDeal subscriptionDeal, UserDTO authenticatedUser) {
        if (hasFullAccess(authenticatedUser)) {
            return;
        }

        User currentUser = getCurrentUser(authenticatedUser);
        if (!subscriptionDeal.getUser().getId().equals(currentUser.getId())) {
            throw new ApiException(403, "Subscribers can only manage their own subscription deals");
        }
    }

    private boolean hasFullAccess(UserDTO authenticatedUser) {
        return hasRole(authenticatedUser, "ADMIN") || hasRole(authenticatedUser, "CLEANING_STAFF");
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
