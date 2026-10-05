package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleaningInvoiceLineDTO {
    private Integer customerNumber;
    private String customerName;
    private int appointmentCount;
    private int durationMinutes;
    private BigDecimal quantityHours;
    private BigDecimal unitNetPrice;
    private BigDecimal netAmount;
    private boolean alreadyInvoiced;
    private String status;
    private Integer draftInvoiceNumber;
    private Integer bookedInvoiceNumber;
    private String error;
}
