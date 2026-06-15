package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "cleaning_appointments")
public class CleaningAppointment implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cleaning_client_id")
    @ToString.Exclude
    private User cleaningClient;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cleaning_staff_id")
    @ToString.Exclude
    private User cleaningStaff;

    private LocalDateTime appointmentTime;

    private int durationMinutes;

    private boolean vacation;
}
