package app.services.dtoConverter;

import app.dto.RequestDTO;
import app.entities.ProductInRequest;
import app.entities.Request;
import app.entities.Tenant;
import app.services.entityServices.ProductInRequestService;
import app.services.entityServices.TenantService;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.List;

public class RequestMapper {
    private final TenantService tenantService;
    private final ProductInRequestService pirService;

    public RequestMapper(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.tenantService = new TenantService(emf);
        this.pirService = new ProductInRequestService(emf);
    }

    public RequestDTO toDto(Request request) {
        if (request == null) return null;
        RequestDTO dto = new RequestDTO();
        dto.setId(request.getId());
        dto.setStartDate(request.getStartDate());
        dto.setEndDate(request.getEndDate());
        dto.setLocation(request.getLocation());
        dto.setStatus(request.getStatus());
        dto.setType(request.getType());
        dto.setAllergies(request.getAllergies());
        dto.setWeatherDTO(request.getWeatherDTO());

        Tenant tenant = request.getTenant();
        dto.setTenantId(tenant != null ? tenant.getId() : null);

        List<Long> ids = new ArrayList<>();
        if (request.getProductsInRequest() != null) {
            for (ProductInRequest pir : request.getProductsInRequest()) {
                if (pir != null && pir.getId() != null) {
                    ids.add(pir.getId());
                }
            }
        }
        dto.setProductInRequestIds(ids);
        return dto;
    }

    public Request fromDto(RequestDTO dto) {
        if (dto == null) return null;
        Request request = new Request();
        request.setId(dto.getId());
        request.setStartDate(dto.getStartDate());
        request.setEndDate(dto.getEndDate());
        request.setLocation(dto.getLocation());
        request.setStatus(dto.getStatus());
        request.setType(dto.getType());
        request.setAllergies(dto.getAllergies());
        request.setWeatherDTO(dto.getWeatherDTO());

        if (dto.getTenantId() != null) {
            Tenant tenant = tenantService.getById(dto.getTenantId());
            request.setTenant(tenant);
        }

        if (dto.getProductInRequestIds() != null) {
            List<ProductInRequest> list = new ArrayList<>();
            for (Long id : dto.getProductInRequestIds()) {
                if (id == null) continue;
                ProductInRequest pir = pirService.getById(id);
                if (pir != null) {
                    pir.setRequest(request);
                    list.add(pir);
                }
            }
            request.setProductsInRequest(list);
        }
        return request;
    }
}
