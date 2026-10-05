package app.services.routeSecurity.routes;

import app.dto.YouthIslandInvoicePreviewDTO;
import app.dto.YouthIslandInvoiceRequestDTO;
import app.services.economic.YouthIslandInvoiceService;
import io.javalin.http.Context;
import jakarta.persistence.EntityManagerFactory;

public class YouthIslandInvoiceRoutes {
    private final YouthIslandInvoiceService youthIslandInvoiceService;

    public YouthIslandInvoiceRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.youthIslandInvoiceService = new YouthIslandInvoiceService(emf);
    }

    public void preview(Context ctx) {
        YouthIslandInvoiceRequestDTO request = ctx.bodyValidator(YouthIslandInvoiceRequestDTO.class).get();
        YouthIslandInvoicePreviewDTO preview = youthIslandInvoiceService.preview(
                request.getStartDate(),
                request.getCustomerNumber(),
                request.getApplyEventDiscount() == null || request.getApplyEventDiscount()
        );
        ctx.json(preview);
    }

    public void create(Context ctx) {
        YouthIslandInvoiceRequestDTO request = ctx.bodyValidator(YouthIslandInvoiceRequestDTO.class).get();
        YouthIslandInvoicePreviewDTO result = youthIslandInvoiceService.createInvoice(
                request.getStartDate(),
                request.getCustomerNumber(),
                request.getApplyEventDiscount() == null || request.getApplyEventDiscount(),
                request.isBookAndSend(),
                request.isForce()
        );
        ctx.json(result);
    }
}
