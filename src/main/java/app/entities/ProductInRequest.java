package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.sql.Time;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "products_in_requests")
public class ProductInRequest implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    @ToString.Exclude
    private Request request;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "economic_product_number", length = 25)
    private String productNumber;

    @Column(name = "product_name", length = 300)
    private String productName;

    @Column(name = "product_description", length = 500)
    private String productDescription;

    @Column(name = "unit_price", precision = 12, scale = 2)
    private BigDecimal unitPrice;

    private Time time;

    private int amount;

}
