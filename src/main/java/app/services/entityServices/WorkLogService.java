package app.services.entityServices;

import app.dao.UserDAO;
import app.dao.WorkLogDAO;
import app.entities.User;
import app.entities.WorkLog;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;
import java.util.Set;

public class WorkLogService implements CrudService<WorkLog> {
    private final WorkLogDAO workLogDAO;
    private final UserDAO userDAO;

    public WorkLogService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.workLogDAO = new WorkLogDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    @Override
    public WorkLog create(WorkLog workLog) {
        validateAndAttachEmployee(workLog);
        return workLogDAO.create(workLog);
    }

    @Override
    public WorkLog getById(Long id) {
        return workLogDAO.getById(id);
    }

    @Override
    public WorkLog update(WorkLog workLog) {
        validateAndAttachEmployee(workLog);
        return workLogDAO.update(workLog);
    }

    @Override
    public WorkLog delete(Long id) {
        return workLogDAO.delete(id);
    }

    @Override
    public Set<WorkLog> getAll() {
        return workLogDAO.getAll();
    }

    public List<WorkLog> getByUserId(Long userId) {
        return workLogDAO.getByUserId(userId);
    }

    private void validateAndAttachEmployee(WorkLog workLog) {
        if (workLog == null) {
            throw new ApiException(400, "WorkLog payload is required");
        }
        if (workLog.getStartTime() == null || workLog.getEndTime() == null) {
            throw new ApiException(400, "startTime and endTime are required");
        }
        if (!workLog.getEndTime().isAfter(workLog.getStartTime())) {
            throw new ApiException(400, "endTime must be after startTime");
        }
        if (workLog.getUser() == null || workLog.getUser().getId() == null) {
            throw new ApiException(400, "userId is required");
        }

        User employee = userDAO.getByIdWithRoles(workLog.getUser().getId());
        if (employee == null) {
            throw new ApiException(404, "User not found");
        }
        if (employee.getRolesAsStrings().stream().noneMatch(role -> role.equalsIgnoreCase("EMPLOYEE"))) {
            throw new ApiException(400, "User must have EMPLOYEE role");
        }

        workLog.setUser(employee);
    }
}
