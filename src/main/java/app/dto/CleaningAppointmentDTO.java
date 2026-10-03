package app.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class CleaningAppointmentDTO {
    private Long id;
    private Long cleaningClientId;
    private Long cleaningStaffId;
    private Integer economicCustomerNumber;
    private String economicCustomerName;
    private Long projectId;
    private String projectName;
    private LocalDateTime appointmentTime;
    private LocalDateTime cancellationTime;
    private int durationMinutes;
    private boolean vacation;
    private boolean applyTaskEditsToFutureAssignments;
    private List<CleaningAssignmentTaskDTO> tasks = new ArrayList<>();
}
