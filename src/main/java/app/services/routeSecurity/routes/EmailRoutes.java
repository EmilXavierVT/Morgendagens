package app.services.routeSecurity.routes;

import app.dto.EmailRequestDTO;
import app.dto.EmailResponseDTO;
import app.services.email.EmailService;
import io.javalin.http.Context;

public class EmailRoutes {
    private final EmailService emailService;

    public EmailRoutes() {
        this.emailService = new EmailService();
    }

    public void send(Context ctx) {
        EmailRequestDTO dto = ctx.bodyValidator(EmailRequestDTO.class).get();
        emailService.send(dto);
        ctx.status(202).json(new EmailResponseDTO("Email accepted for delivery"));
    }
}
