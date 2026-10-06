package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CleaningSheetSyncResultDTO {
    private int sourceRows;
    private int parsedAssignments;
    private int createdAppointments;
    private int updatedAppointments;
    private int unchangedAppointments;
    private int skippedAssignments;
}
