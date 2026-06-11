package app.services.dtoConverter;

import app.dto.WorkLogDTO;
import app.entities.User;
import app.entities.WorkLog;

public class WorkLogMapper {
    public WorkLogDTO toDto(WorkLog workLog) {
        if (workLog == null) return null;

        WorkLogDTO dto = new WorkLogDTO();
        dto.setId(workLog.getId());
        dto.setStartTime(workLog.getStartTime());
        dto.setEndTime(workLog.getEndTime());
        dto.setUserId(workLog.getUser() != null ? workLog.getUser().getId() : null);
        return dto;
    }

    public WorkLog fromDto(WorkLogDTO dto) {
        if (dto == null) return null;

        WorkLog workLog = new WorkLog();
        workLog.setId(dto.getId());
        workLog.setStartTime(dto.getStartTime());
        workLog.setEndTime(dto.getEndTime());

        if (dto.getUserId() != null) {
            User user = new User();
            user.setId(dto.getUserId());
            workLog.setUser(user);
        }

        return workLog;
    }
}
