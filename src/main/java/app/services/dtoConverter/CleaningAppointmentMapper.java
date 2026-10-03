package app.services.dtoConverter;

import app.dto.CleaningAssignmentTaskDTO;
import app.dto.CleaningAppointmentDTO;
import app.entities.CleaningAssignmentTask;
import app.entities.CleaningAppointment;
import app.entities.CleaningProject;
import app.entities.EconomicCustomer;
import app.entities.User;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CleaningAppointmentMapper {
    public CleaningAppointmentDTO toDto(CleaningAppointment appointment) {
        if (appointment == null) return null;

        CleaningAppointmentDTO dto = new CleaningAppointmentDTO();
        dto.setId(appointment.getId());
        dto.setCleaningClientId(appointment.getCleaningClient() != null ? appointment.getCleaningClient().getId() : null);
        dto.setCleaningStaffId(appointment.getCleaningStaff() != null ? appointment.getCleaningStaff().getId() : null);
        dto.setProjectId(appointment.getProject() != null ? appointment.getProject().getId() : null);
        dto.setProjectName(appointment.getProject() != null ? appointment.getProject().getName() : null);
        dto.setEconomicCustomerNumber(appointment.getProject() != null && appointment.getProject().getEconomicCustomer() != null
                ? appointment.getProject().getEconomicCustomer().getCustomerNumber()
                : null);
        dto.setEconomicCustomerName(appointment.getProject() != null && appointment.getProject().getEconomicCustomer() != null
                ? appointment.getProject().getEconomicCustomer().getName()
                : null);
        dto.setAppointmentTime(appointment.getAppointmentTime());
        dto.setCancellationTime(appointment.getCancellationTime());
        dto.setDurationMinutes(appointment.getDurationMinutes());
        dto.setVacation(appointment.isVacation());
        dto.setTasks(toTaskDtos(appointment.getTasks()));
        return dto;
    }

    public CleaningAppointment fromDto(CleaningAppointmentDTO dto) {
        if (dto == null) return null;

        CleaningAppointment appointment = new CleaningAppointment();
        appointment.setId(dto.getId());
        appointment.setAppointmentTime(dto.getAppointmentTime());
        appointment.setCancellationTime(dto.getCancellationTime());
        appointment.setDurationMinutes(dto.getDurationMinutes());
        appointment.setVacation(dto.isVacation());

        if (dto.getProjectId() != null || (dto.getProjectName() != null && !dto.getProjectName().isBlank())) {
            CleaningProject project = new CleaningProject();
            project.setId(dto.getProjectId());
            project.setName(dto.getProjectName());
            if (dto.getEconomicCustomerNumber() != null) {
                EconomicCustomer customer = new EconomicCustomer();
                customer.setCustomerNumber(dto.getEconomicCustomerNumber());
                customer.setName(dto.getEconomicCustomerName());
                project.setEconomicCustomer(customer);
            }
            appointment.setProject(project);
        } else if (dto.getEconomicCustomerNumber() != null) {
            CleaningProject project = new CleaningProject();
            EconomicCustomer customer = new EconomicCustomer();
            customer.setCustomerNumber(dto.getEconomicCustomerNumber());
            customer.setName(dto.getEconomicCustomerName());
            project.setEconomicCustomer(customer);
            project.setName(dto.getEconomicCustomerName());
            appointment.setProject(project);
        }

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

        appointment.setTasks(fromTaskDtos(dto.getTasks(), appointment));

        return appointment;
    }

    private List<CleaningAssignmentTaskDTO> toTaskDtos(List<CleaningAssignmentTask> tasks) {
        if (tasks == null || tasks.isEmpty()) return new ArrayList<>();

        return tasks.stream()
                .sorted(Comparator.comparingInt(CleaningAssignmentTask::getSortOrder))
                .map(task -> new CleaningAssignmentTaskDTO(
                        task.getId(),
                        task.getTitle(),
                        task.isCompleted(),
                        task.getSortOrder()
                ))
                .toList();
    }

    private List<CleaningAssignmentTask> fromTaskDtos(List<CleaningAssignmentTaskDTO> dtos, CleaningAppointment appointment) {
        List<CleaningAssignmentTask> tasks = new ArrayList<>();
        if (dtos == null) return tasks;

        for (int index = 0; index < dtos.size(); index += 1) {
            CleaningAssignmentTaskDTO dto = dtos.get(index);
            if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
                continue;
            }

            CleaningAssignmentTask task = new CleaningAssignmentTask();
            task.setId(dto.getId());
            task.setTitle(dto.getTitle().trim());
            task.setCompleted(dto.isCompleted());
            task.setSortOrder(dto.getSortOrder() > 0 ? dto.getSortOrder() : index + 1);
            task.setAppointment(appointment);
            tasks.add(task);
        }

        return tasks;
    }
}
