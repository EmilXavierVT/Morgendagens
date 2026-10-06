package app.services.entityServices;

import app.dao.CleaningAppointmentDAO;
import app.dao.UserDAO;
import app.entities.CleaningAppointment;
import app.entities.User;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Set;

public class CleaningAppointmentService implements CrudService<CleaningAppointment> {
    private final CleaningAppointmentDAO cleaningAppointmentDAO;
    private final UserDAO userDAO;

    public CleaningAppointmentService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.cleaningAppointmentDAO = new CleaningAppointmentDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    @Override
    public CleaningAppointment create(CleaningAppointment appointment) {
        validateAndAttachUsers(appointment);
        return cleaningAppointmentDAO.create(appointment);
    }

    @Override
    public CleaningAppointment getById(Long id) {
        return cleaningAppointmentDAO.getById(id);
    }

    @Override
    public CleaningAppointment update(CleaningAppointment appointment) {
        return update(appointment, false);
    }

    public CleaningAppointment update(CleaningAppointment appointment, boolean applyTaskEditsToFutureAssignments) {
        validateAndAttachUsers(appointment);
        return cleaningAppointmentDAO.update(appointment, applyTaskEditsToFutureAssignments);
    }

    @Override
    public CleaningAppointment delete(Long id) {
        return cleaningAppointmentDAO.delete(id);
    }

    @Override
    public Set<CleaningAppointment> getAll() {
        return cleaningAppointmentDAO.getAll();
    }

    public List<CleaningAppointment> getByCleaningStaffId(Long cleaningStaffId) {
        return cleaningAppointmentDAO.getByCleaningStaffId(cleaningStaffId);
    }

    public List<CleaningAppointment> getVisibleToCleaningStaff(Long cleaningStaffId) {
        return cleaningAppointmentDAO.getVisibleToCleaningStaff(cleaningStaffId);
    }

    public List<CleaningAppointment> getByCleaningClientId(Long cleaningClientId) {
        return cleaningAppointmentDAO.getByCleaningClientId(cleaningClientId);
    }

    private void validateAndAttachUsers(CleaningAppointment appointment) {
        if (appointment == null) {
            throw new ApiException(400, "CleaningAppointment payload is required");
        }
        if (appointment.getAppointmentTime() == null) {
            throw new ApiException(400, "appointmentTime is required");
        }
        if (appointment.getDurationMinutes() <= 0) {
            throw new ApiException(400, "durationMinutes must be greater than 0");
        }
        boolean hasLocalCleaningClient = appointment.getCleaningClient() != null && appointment.getCleaningClient().getId() != null;
        boolean hasEconomicCustomer = appointment.getProject() != null
                && appointment.getProject().getEconomicCustomer() != null
                && appointment.getProject().getEconomicCustomer().getCustomerNumber() != null;

        if (!hasLocalCleaningClient && !hasEconomicCustomer) {
            throw new ApiException(400, "cleaningClientId or economicCustomerNumber is required");
        }

        if (hasLocalCleaningClient) {
            User client = userDAO.getByIdWithRoles(appointment.getCleaningClient().getId());
            if (client == null) {
                throw new ApiException(404, "Cleaning client not found");
            }
            appointment.setCleaningClient(client);
        } else {
            appointment.setCleaningClient(null);
        }

        if (appointment.getCleaningStaff() == null || appointment.getCleaningStaff().getId() == null) {
            appointment.setCleaningStaff(null);
            return;
        }

        User staff = userDAO.getByIdWithRoles(appointment.getCleaningStaff().getId());
        if (staff == null) {
            throw new ApiException(404, "Cleaning staff user not found");
        }
        if (staff.getRolesAsStrings().stream().noneMatch(role -> (
                role.equalsIgnoreCase("CLEANING_STAFF")
                        || role.equalsIgnoreCase("CLEANING_MANAGER")
        ))) {
            throw new ApiException(400, "User must have CLEANING_STAFF or CLEANING_MANAGER role");
        }

        appointment.setCleaningStaff(staff);
    }
}
