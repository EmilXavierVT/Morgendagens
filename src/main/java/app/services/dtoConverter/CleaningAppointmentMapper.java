package app.services.dtoConverter;

import app.dto.CleaningAppointmentDTO;
import app.entities.CleaningAppointment;
import app.entities.User;

public class CleaningAppointmentMapper {
    public CleaningAppointmentDTO toDto(CleaningAppointment appointment) {
        if (appointment == null) return null;

        CleaningAppointmentDTO dto = new CleaningAppointmentDTO();
        dto.setId(appointment.getId());
        dto.setCleaningClientId(appointment.getCleaningClient() != null ? appointment.getCleaningClient().getId() : null);
        dto.setCleaningStaffId(appointment.getCleaningStaff() != null ? appointment.getCleaningStaff().getId() : null);
        dto.setAppointmentTime(appointment.getAppointmentTime());
        dto.setDurationMinutes(appointment.getDurationMinutes());
        return dto;
    }

    public CleaningAppointment fromDto(CleaningAppointmentDTO dto) {
        if (dto == null) return null;

        CleaningAppointment appointment = new CleaningAppointment();
        appointment.setId(dto.getId());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setDurationMinutes(dto.getDurationMinutes());

        if (dto.getCleaningClientId() != null) {
            User client = new User();
            client.setId(dto.getCleaningClientId());
            appointment.setCleaningClient(client);
        }

        if (dto.getCleaningStaffId() != null) {
            User staff = new User();
            staff.setId(dto.getCleaningStaffId());
            appointment.setCleaningStaff(staff);
        }

        return appointment;
    }
}
