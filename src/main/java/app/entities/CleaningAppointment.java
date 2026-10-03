package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;
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

    @ManyToOne(optional = true)
    @JoinColumn(name = "cleaning_client_id")
    @ToString.Exclude
    private User cleaningClient;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cleaning_staff_id")
    @ToString.Exclude
    private User cleaningStaff;

    @ManyToOne(optional = true)
    @JoinColumn(name = "project_id")
    @ToString.Exclude
    private CleaningProject project;

    @OneToMany(mappedBy = "appointment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @ToString.Exclude
    @Builder.Default
    private List<CleaningAssignmentTask> tasks = new ArrayList<>();

    private LocalDateTime appointmentTime;

    private LocalDateTime cancellationTime;

    private int durationMinutes;

    private boolean vacation;
}
