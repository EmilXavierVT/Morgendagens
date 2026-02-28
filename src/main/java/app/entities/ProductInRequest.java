package app.entities;

import jakarta.persistence.*;
import lombok.*;

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

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private Time time;


}
