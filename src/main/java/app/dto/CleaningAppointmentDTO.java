package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CleaningAppointmentDTO {
    private Long id;
    private Long cleaningClientId;
    private Long cleaningStaffId;
    private LocalDateTime appointmentTime;
    private LocalDateTime cancellationTime;
    private int durationMinutes;
    private boolean vacation;
}
