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
public class YouthIslandInvoiceEventDTO {
    private Long bookingId;
    private String eventName;
    private LocalDate eventDate;
    private String spectraReservationNumber;
    private int lineCount;
    private int totalGuests;
    private BigDecimal netAmount;
    private boolean alreadyInvoiced;
    private String status;
    private Integer draftInvoiceNumber;
    private Integer bookedInvoiceNumber;
    private String error;
    private List<YouthIslandInvoiceLineDTO> lines = new ArrayList<>();
}
