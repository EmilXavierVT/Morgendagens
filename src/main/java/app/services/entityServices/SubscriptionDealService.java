package app.services.entityServices;

import app.dao.SubscriptionDealDAO;
import app.dao.UserDAO;
import app.entities.SubscriptionDeal;
import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Set;

public class SubscriptionDealService implements CrudService<SubscriptionDeal> {
    private final SubscriptionDealDAO subscriptionDealDAO;
    private final UserDAO userDAO;

    public SubscriptionDealService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.subscriptionDealDAO = new SubscriptionDealDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    @Override
    public SubscriptionDeal create(SubscriptionDeal subscriptionDeal) {
        validateAndAttachUser(subscriptionDeal);
        return subscriptionDealDAO.create(subscriptionDeal);
    }

    @Override
    public SubscriptionDeal getById(Long id) {
        return subscriptionDealDAO.getById(id);
    }

    @Override
    public SubscriptionDeal update(SubscriptionDeal subscriptionDeal) {
        validateAndAttachUser(subscriptionDeal);
        return subscriptionDealDAO.update(subscriptionDeal);
    }

    @Override
    public SubscriptionDeal delete(Long id) {
        return subscriptionDealDAO.delete(id);
    }

    @Override
    public Set<SubscriptionDeal> getAll() {
        return subscriptionDealDAO.getAll();
    }

    public List<SubscriptionDeal> getByUserId(Long userId) {
        return subscriptionDealDAO.getByUserId(userId);
    }

    private void validateAndAttachUser(SubscriptionDeal subscriptionDeal) {
        if (subscriptionDeal == null) {
            throw new ApiException(400, "SubscriptionDeal payload is required");
        }
        if (subscriptionDeal.getVisitsPerMonth() <= 0) {
            throw new ApiException(400, "visitsPerMonth must be greater than 0");
        }
        if (subscriptionDeal.getUser() == null || subscriptionDeal.getUser().getId() == null) {
            throw new ApiException(400, "userId is required");
        }

        User user = userDAO.getByIdWithRoles(subscriptionDeal.getUser().getId());
        if (user == null) {
            throw new ApiException(404, "User not found");
        }
        if (!hasRole(user, "SUBSCRIBER") || !hasRole(user, "CLEANING_CLIENT")) {
            throw new ApiException(400, "User must have both SUBSCRIBER and CLEANING_CLIENT roles");
        }

        subscriptionDeal.setUser(user);
    }

    private boolean hasRole(User user, String roleName) {
        return user.getRolesAsStrings().stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
    }
}
