package app.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "youth_island_bookings")
public class YouthIslandBooking implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "ordered_by_user_id", nullable = false)
    @ToString.Exclude
    private User orderedBy;

    @Column(nullable = false)
    private String customerName;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(columnDefinition = "text")
    private String allergies;

    private String location;

    @Column(nullable = false, unique = true)
    private String spectraReservationNumber;

    @Column(columnDefinition = "text")
    private String notes;

    private int snackTrayCount;

    @Column(nullable = false)
    private LocalDate orderDate;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("servingTime ASC, id ASC")
    @Builder.Default
    @ToString.Exclude
    private List<YouthIslandBookingItem> items = new ArrayList<>();

    public void setItems(List<YouthIslandBookingItem> items) {
        this.items.clear();
        if (items == null) return;

        for (YouthIslandBookingItem item : items) {
            if (item == null) continue;
            item.setBooking(this);
            this.items.add(item);
        }
    }

    @PrePersist
    void initializeTimestamps() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = LocalDateTime.now();
    }
}
