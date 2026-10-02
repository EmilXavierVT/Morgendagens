package app.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

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
    @Column(length = 300)
    private String name;

    @Column(length = 500)
    private String description;
    private double price;
    private int type;

    @Column(name = "economic_product_number", unique = true, length = 25)
    private String economicProductNumber;

    @Column(name = "economic_product_group_name", length = 50)
    private String economicProductGroupName;

    @Column(name = "economic_product_group_number")
    private Integer economicProductGroupNumber;

    @Column(name = "economic_barred")
    private Boolean economicBarred;

    @Column(name = "economic_available")
    @Builder.Default
    private boolean economicAvailable = false;

    @Column(name = "economic_last_updated")
    private OffsetDateTime economicLastUpdated;

    @Column(name = "economic_self", length = 1024)
    private String economicSelf;

    @OneToMany(mappedBy = "product", cascade = CascadeType.REMOVE, orphanRemoval = true)
    @JsonIgnore
    @ToString.Exclude
    @Builder.Default
    private java.util.List<ProductInRequest> productInRequests = new java.util.ArrayList<>();
}
