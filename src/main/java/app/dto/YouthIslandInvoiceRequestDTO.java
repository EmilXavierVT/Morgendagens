package app.dto;

import lombok.Data;

@Data
public class YouthIslandInvoiceRequestDTO {
    private String startDate;
    private Integer customerNumber;
    private Boolean applyEventDiscount = true;
    private boolean bookAndSend = true;
    private boolean force = false;
}
