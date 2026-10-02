package app.services.routeSecurity.routes;

import app.dto.EconomicCustomerResponse;
import app.entities.EconomicCustomer;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.util.List;

public class EconomicCustomerRoutes {
    private final EntityManagerFactory emf;

    public EconomicCustomerRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public void getAll(Context ctx) {
        try (EntityManager em = emf.createEntityManager()) {
            List<EconomicCustomerResponse> customers = em.createQuery("""
                            SELECT customer
                            FROM EconomicCustomer customer
                            WHERE customer.economicAvailable = true
                            ORDER BY customer.name ASC
                            """, EconomicCustomer.class)
                    .getResultList()
                    .stream()
                    .map(this::toDto)
                    .toList();
            ctx.json(customers);
        }
    }

    private EconomicCustomerResponse toDto(EconomicCustomer customer) {
        EconomicCustomerResponse dto = new EconomicCustomerResponse();
        dto.setCustomerNumber(customer.getCustomerNumber());
        dto.setName(customer.getName());
        dto.setEmail(customer.getEmail());
        dto.setAddress(customer.getAddress());
        dto.setCity(customer.getCity());
        dto.setZip(customer.getZip());
        dto.setCountry(customer.getCountry());
        dto.setMobilePhone(customer.getMobilePhone());
        dto.setTelephoneAndFaxNumber(customer.getTelephoneAndFaxNumber());
        dto.setCorporateIdentificationNumber(customer.getCorporateIdentificationNumber());
        dto.setVatNumber(customer.getVatNumber());
        dto.setBalance(customer.getBalance());
        dto.setBarred(customer.getBarred());
        dto.setLastUpdated(customer.getLastUpdated());
        dto.setSelf(customer.getSelf());
        if (customer.getCustomerGroupNumber() != null || customer.getCustomerGroupName() != null) {
            EconomicCustomerResponse.CustomerGroup group = new EconomicCustomerResponse.CustomerGroup();
            group.setCustomerGroupNumber(customer.getCustomerGroupNumber());
            group.setName(customer.getCustomerGroupName());
            dto.setCustomerGroup(group);
        }
        return dto;
    }
}
