package app.services.entityApiServices;

import app.dto.MessageDTO;
import app.entities.Message;
import app.entities.User;
import app.services.entityServices.UserService;
import jakarta.persistence.EntityManagerFactory;

public class MessageMapper {
    private final UserService userService;

    public MessageMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userService = new UserService(emf);
    }

    public MessageDTO toDto(Message message) {
        if (message == null) return null;
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setThread(message.getThread());
        dto.setContext(message.getContext());
        dto.setDate(message.getDate());

        User user = message.getUser();
        dto.setUserId(user != null ? user.getId() : null);
        return dto;
    }

    public Message fromDto(MessageDTO dto) {
        if (dto == null) return null;
        Message message = new Message();
        message.setId(dto.getId());
        message.setThread(dto.getThread());
        message.setContext(dto.getContext());
        message.setDate(dto.getDate());

        if (dto.getUserId() != null) {
            User user = userService.getById(dto.getUserId());
            message.setUser(user);
        }
        return message;
    }
}
