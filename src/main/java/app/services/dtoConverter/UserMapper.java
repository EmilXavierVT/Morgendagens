package app.services.dtoConverter;

import app.dto.UserDTO;
import app.entities.Message;
import app.entities.Tenant;
import app.entities.User;
import app.services.entityServices.MessageService;
import app.services.entityServices.TenantService;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class UserMapper {
    private final TenantService tenantService;
    private final MessageService messageService;

    public UserMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.tenantService = new TenantService(emf);
        this.messageService = new MessageService(emf);
    }

    public UserDTO toDto(User user) {
        if (user == null) return null;
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setRole(user.getRole());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setZipCode(user.getZipCode());
        dto.setEmail(user.getEmail());
        dto.setPassword(user.getPassword());
        dto.setPhoneNumber(user.getPhoneNumber());

        Tenant tenant = user.getTenant();
        dto.setTenantId(tenant != null ? tenant.getId() : null);

        List<Long> messageIds = new ArrayList<>();
        if (user.getMessages() != null) {
            for (Message message : user.getMessages()) {
                if (message != null && message.getId() != null) {
                    messageIds.add(message.getId());
                }
            }
        }
        dto.setMessageIds(messageIds);
        return dto;
    }

    public User fromDto(UserDTO dto) {
        if (dto == null) return null;
        User user = new User();
        user.setId(dto.getId());
        user.setRole(dto.getRole());
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setZipCode(dto.getZipCode());
        user.setEmail(dto.getEmail());
        user.setPassword(dto.getPassword());
        user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getTenantId() != null) {
            Tenant tenant = tenantService.getById(dto.getTenantId());
            user.setTenant(tenant);
        }

        if (dto.getMessageIds() != null) {
            List<Message> messages = new ArrayList<>();
            for (Long messageId : dto.getMessageIds()) {
                if (messageId == null) continue;
                Message message = messageService.getById(messageId);
                if (message != null) {
                    message.setUser(user);
                    messages.add(message);
                }
            }
            user.setMessages(messages);
        }
        return user;
    }
}
