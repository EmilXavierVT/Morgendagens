package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(
        name = "cleaning_invoice_records",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cleaning_invoice_month_customer_product",
                columnNames = {"invoice_month", "economic_customer_number", "product_number"}
        )
)
public class CleaningInvoiceRecord implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "invoice_month", nullable = false, length = 7)
    private String invoiceMonth;

    @Column(name = "economic_customer_number", nullable = false)
    private Integer economicCustomerNumber;

    @Column(nullable = false)
    private String economicCustomerName;

    @Column(name = "product_number", nullable = false, length = 25)
    private String productNumber;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal quantityHours;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitNetPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal netAmount;

    private int appointmentCount;
    private Integer draftInvoiceNumber;
    private Integer bookedInvoiceNumber;

    @Column(nullable = false)
    private boolean sentByEmail;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}
