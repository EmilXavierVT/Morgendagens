package app.services.entityApiServices;

import app.dto.TenantDTO;
import app.entities.Request;
import app.entities.Tenant;
import app.entities.User;
import app.services.entityServices.RequestService;
import app.services.entityServices.UserService;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class TenantMapper {
    private final UserService userService;
    private final RequestService requestService;

    public TenantMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.userService = new UserService(emf);
        this.requestService = new RequestService(emf);
    }

    public TenantDTO toDto(Tenant tenant) {
        if (tenant == null) return null;
        TenantDTO dto = new TenantDTO();
        dto.setId(tenant.getId());
        dto.setName(tenant.getName());
        dto.setType(tenant.getType());
        dto.setStatus(tenant.getStatus());

        List<Long> userIds = new ArrayList<>();
        if (tenant.getUsers() != null) {
            for (User u : tenant.getUsers()) {
                if (u != null && u.getId() != null) {
                    userIds.add(u.getId());
                }
            }
        }
        dto.setUserIds(userIds);

        List<Long> requestIds = new ArrayList<>();
        if (tenant.getRequests() != null) {
            for (Request r : tenant.getRequests()) {
                if (r != null && r.getId() != null) {
                    requestIds.add(r.getId());
                }
            }
        }
        dto.setRequestIds(requestIds);
        return dto;
    }

    public Tenant fromDto(TenantDTO dto) {
        if (dto == null) return null;
        Tenant tenant = new Tenant();
        tenant.setId(dto.getId());
        tenant.setName(dto.getName());
        tenant.setType(dto.getType());
        tenant.setStatus(dto.getStatus());

        if (dto.getUserIds() != null) {
            List<User> users = new ArrayList<>();
            for (Long id : dto.getUserIds()) {
                if (id == null) continue;
                User u = userService.getById(id);
                if (u != null) {
                    u.setTenant(tenant);
                    users.add(u);
                }
            }
            tenant.setUsers(users);
        }

        if (dto.getRequestIds() != null) {
            List<Request> list = new ArrayList<>();
            for (Long id : dto.getRequestIds()) {
                if (id == null) continue;
                Request r = requestService.getById(id);
                if (r != null) {
                    r.setTenant(tenant);
                    list.add(r);
                }
            }
            tenant.setRequests(list);
        }
        return tenant;
    }
}
