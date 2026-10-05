package app.services.routeSecurity.routes;

import app.dto.CleaningInvoicePreviewDTO;
import app.dto.CleaningInvoiceRequestDTO;
import app.services.economic.CleaningInvoiceService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class CleaningInvoiceRoutes {
    private final CleaningInvoiceService cleaningInvoiceService;

    public CleaningInvoiceRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.cleaningInvoiceService = new CleaningInvoiceService(emf);
    }

    public void preview(Context ctx) {
        CleaningInvoiceRequestDTO request = ctx.bodyValidator(CleaningInvoiceRequestDTO.class).get();
        CleaningInvoicePreviewDTO preview = cleaningInvoiceService.preview(
                request.getInvoiceMonth(),
                request.getProductNumber()
        );
        ctx.json(preview);
    }

    public void create(Context ctx) {
        CleaningInvoiceRequestDTO request = ctx.bodyValidator(CleaningInvoiceRequestDTO.class).get();
        CleaningInvoicePreviewDTO result = cleaningInvoiceService.createInvoices(
                request.getInvoiceMonth(),
                request.getProductNumber(),
                request.isBookAndSend(),
                request.isForce()
        );
        ctx.json(result);
    }
}
