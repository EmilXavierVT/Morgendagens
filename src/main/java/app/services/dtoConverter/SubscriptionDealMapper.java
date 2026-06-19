package app.services.dtoConverter;

import app.dto.SubscriptionDealDTO;
import app.entities.SubscriptionDeal;
import app.entities.User;

public class SubscriptionDealMapper {
    public SubscriptionDealDTO toDto(SubscriptionDeal subscriptionDeal) {
        if (subscriptionDeal == null) return null;

        SubscriptionDealDTO dto = new SubscriptionDealDTO();
        dto.setId(subscriptionDeal.getId());
        dto.setUserId(subscriptionDeal.getUser() != null ? subscriptionDeal.getUser().getId() : null);
        dto.setVisitsPerMonth(subscriptionDeal.getVisitsPerMonth());
        return dto;
    }

    public SubscriptionDeal fromDto(SubscriptionDealDTO dto) {
        if (dto == null) return null;

        SubscriptionDeal subscriptionDeal = new SubscriptionDeal();
        subscriptionDeal.setId(dto.getId());
        subscriptionDeal.setVisitsPerMonth(dto.getVisitsPerMonth());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            subscriptionDeal.setUser(user);
        }

        return subscriptionDeal;
    }
}
