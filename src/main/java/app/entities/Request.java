package app.entities;

import app.dto.WeatherDTO;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Builder
@Table(name = "requests")
public class Request implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id")
    @ToString.Exclude
    private Tenant tenant;
    private Long startDate;
    private Long endDate;
    private int status;
    private int type;
    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @Builder.Default
    private List<ProductInRequest> productsInRequest = new ArrayList<>();

    @Transient
//    @ToString.Exclude
    private WeatherDTO weatherDTO;


}
