package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouthIslandInvoicePreviewDTO {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate invoiceDate;
    private Integer customerNumber;
    private String customerName;
    private int bookingCount;
    private int invoiceCount;
    private int lineCount;
    private int totalGuests;
    private BigDecimal totalNetAmount;
    private boolean alreadyInvoiced;
    private String status;
    private Integer draftInvoiceNumber;
    private Integer bookedInvoiceNumber;
    private String error;
    private List<YouthIslandInvoiceLineDTO> lines = new ArrayList<>();
    private List<YouthIslandInvoiceEventDTO> invoices = new ArrayList<>();
}
