package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouthIslandInvoiceLineDTO {
    private Long bookingId;
    private LocalDate eventDate;
    private LocalTime servingTime;
    private String spectraReservationNumber;
    private String productNumber;
    private String productName;
    private String room;
    private int guestCount;
    private BigDecimal unitNetPrice;
    private BigDecimal discountPercentage;
    private BigDecimal netAmount;
}
