package app.dto;

import lombok.Data;

@Data
public class CleaningInvoiceRequestDTO {
    private String invoiceMonth;
    private String productNumber;
    private boolean bookAndSend;
    private boolean force;
}
