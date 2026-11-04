package app.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.time.LocalDateTime;
import java.time.Instant;
import java.time.ZoneId;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "youth_island_booking_edits", indexes = @Index(name = "idx_youth_booking_edits_booking", columnList = "booking_id,id"))
public class YouthIslandBookingEdit implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    @ToString.Exclude
    private YouthIslandBooking booking;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "edited_by_user_id", nullable = false)
    @ToString.Exclude
    private User editedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime editedAt;

    @Column(name = "recorded_at", updatable = false)
    private Instant recordedAt;

    @Column(updatable = false)
    private Long revision;

    @Column(name = "edited_by_name", length = 512, updatable = false)
    private String editedByName;

    @Column(name = "edited_by_email", updatable = false)
    private String editedByEmail;

    @Column(name = "before_snapshot", columnDefinition = "text", updatable = false)
    private String beforeSnapshot;

    @Column(name = "after_snapshot", columnDefinition = "text", updatable = false)
    private String afterSnapshot;

    @PrePersist
    void initializeTimestamp() {
        if (recordedAt == null) recordedAt = Instant.now();
        if (editedAt == null) editedAt = LocalDateTime.ofInstant(recordedAt, ZoneId.of("Europe/Copenhagen"));
    }
}
