package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "youth_island_booking_items")
public class YouthIslandBookingItem implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    private YouthIslandBooking booking;

    @Column(nullable = false)
    private int guestCount;

    @Column(nullable = false)
    private LocalTime servingTime;

    @Column(name = "economic_product_number", length = 25)
    private String productNumber;

    @Column(name = "product_name", length = 300)
    private String productName;

    @Column(name = "unit_price_override", precision = 12, scale = 2)
    private BigDecimal unitPriceOverride;

    @Column(columnDefinition = "text")
    private String menu;

    @Column
    private String room;
}
