package app.services.apiServices.routes;

import app.dto.MessageDTO;
import app.entities.Message;
import app.services.dtoConverter.MessageMapper;
import app.services.entityServices.MessageService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


public class MessageRoutes {
    private final MessageService messageService;
    private final MessageMapper messageMapper;
    private static final Logger logger = LoggerFactory.getLogger(MessageRoutes.class);
    private static final Logger debugLogger = LoggerFactory.getLogger("app");

    public MessageRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.messageService = new MessageService(emf);
        this.messageMapper = new MessageMapper(emf);
    }

    public void getAll(Context ctx) {
    logger.info("getAll");
        debugLogger.info("getAll");
        List<MessageDTO> dtos = new ArrayList<>();
        for (Message message : messageService.getAll()) {
            dtos.add(messageMapper.toDto(message));
        }
        ctx.json(dtos);
    }

    public void getById(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Message message = messageService.getById(id);
        if (message == null) {
            ctx.status(404).result("Message not found");
            return;
        }
        ctx.json(messageMapper.toDto(message));
    }

    public void create(Context ctx) {
        MessageDTO dto = ctx.bodyValidator(MessageDTO.class).get();
        Message message = messageMapper.fromDto(dto);
        Message created = messageService.create(message);
        ctx.status(201).json(messageMapper.toDto(created));
    }

    public void update(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        MessageDTO dto = ctx.bodyValidator(MessageDTO.class).get();
        dto.setId(id);
        Message message = messageMapper.fromDto(dto);
        Message updated = messageService.update(message);
        if (updated == null) {
            ctx.status(404).result("Message not found");
            return;
        }
        ctx.json(messageMapper.toDto(updated));
    }

    public void delete(Context ctx) {
        Long id = ctx.pathParamAsClass("id", Long.class).get();
        Message deleted = messageService.delete(id);
        if (deleted == null) {
            ctx.status(404).result("Message not found");
            return;
        }
        ctx.status(204);
    }
}
