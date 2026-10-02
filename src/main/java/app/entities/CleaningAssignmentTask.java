package app.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "cleaning_assignment_tasks")
public class CleaningAssignmentTask implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "cleaning_appointment_id")
    @ToString.Exclude
    private CleaningAppointment appointment;

    @Column(nullable = false)
    private String title;

    private boolean completed;

    private int sortOrder;
}
