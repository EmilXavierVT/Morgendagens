package app.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@Table(name = "cleaning_projects")
public class CleaningProject implements IEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = true)
    @JoinColumn(name = "cleaning_client_id")
    @ToString.Exclude
    private User cleaningClient;

    @ManyToOne(optional = true)
    @JoinColumn(name = "economic_customer_number", referencedColumnName = "economic_customer_number")
    @ToString.Exclude
    private EconomicCustomer economicCustomer;

    @Column(nullable = false)
    private String name;

    @Column(length = 1000)
    private String notes;

    private boolean active;

    @OneToMany(mappedBy = "project")
    @ToString.Exclude
    @Builder.Default
    private List<CleaningAppointment> assignments = new ArrayList<>();
}
