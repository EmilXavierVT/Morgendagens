package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleaningInvoicePreviewDTO {
    private String invoiceMonth;
    private String productNumber;
    private String productName;
    private BigDecimal unitNetPrice;
    private int totalCustomers;
    private int totalAppointments;
    private BigDecimal totalHours;
    private BigDecimal totalNetAmount;
    private List<CleaningInvoiceLineDTO> lines = new ArrayList<>();
}
