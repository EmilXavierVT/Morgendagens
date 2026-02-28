package app.entities;

import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "products")
public class Product implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private double price;
    private int type;

    @OneToMany(mappedBy = "product")
    @ToString.Exclude
    @Builder.Default
    private java.util.List<ProductInRequest> productInRequests = new java.util.ArrayList<>();
}
