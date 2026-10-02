package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Entity
@Table(name = "economic_customers")
public class EconomicCustomer implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "economic_customer_number", unique = true, nullable = false)
    private Integer customerNumber;

    private String name;
    private String email;
    private String address;
    private String city;
    private String zip;
    private String country;
    private String mobilePhone;
    private String telephoneAndFaxNumber;
    private String corporateIdentificationNumber;
    private String vatNumber;
    private BigDecimal balance;

    @Column(name = "economic_barred")
    private Boolean barred;

    @Column(name = "economic_available")
    @Builder.Default
    private boolean economicAvailable = true;

    @Column(name = "economic_last_updated")
    private OffsetDateTime lastUpdated;

    private Integer customerGroupNumber;
    private String customerGroupName;

    @Column(name = "economic_self", length = 1024)
    private String self;
}
