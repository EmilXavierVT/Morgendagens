package app.services.entityServices;

import app.dao.MessageDAO;
import app.entities.Message;
import jakarta.persistence.EntityManagerFactory;

import java.util.Set;

public class MessageService implements CrudService<Message> {
    private final MessageDAO messageDAO;

    public MessageService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.messageDAO = new MessageDAO(emf);
    }

    public Message create(Message message) {
        return messageDAO.create(message);
    }

    public Message getById(Long id) {
        return messageDAO.getById(id);
    }

    public Message update(Message message) {
        return messageDAO.update(message);
    }

    public Message delete(Long id) {
        return messageDAO.delete(id);
    }

    public Set<Message> getAll() {
        return messageDAO.getAll();
    }
}
