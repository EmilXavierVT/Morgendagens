package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(
        name = "youth_island_invoice_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_youth_island_invoice_booking_customer",
                columnNames = {"booking_id", "economic_customer_number"}
        )
)
public class YouthIslandInvoiceRecord implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "invoice_date")
    private LocalDate invoiceDate;

    @Column(name = "booking_id")
    private Long bookingId;

    private String eventName;

    @Column(name = "economic_customer_number", nullable = false)
    private Integer economicCustomerNumber;

    @Column(nullable = false)
    private String economicCustomerName;

    private int bookingCount;
    private int lineCount;
    private int totalGuests;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    private Integer draftInvoiceNumber;
    private Integer bookedInvoiceNumber;

    @Column(nullable = false)
    private boolean sentByEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
