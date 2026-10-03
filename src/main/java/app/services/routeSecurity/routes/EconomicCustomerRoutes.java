package app.services.routeSecurity.routes;

import app.dto.EconomicCustomerResponse;
import app.dto.UserDTO;
import app.entities.EconomicCustomer;
import io.javalin.http.Context;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.text.Normalizer;
import java.util.List;

public class EconomicCustomerRoutes {
    private static final int CLEANING_CUSTOMER_GROUP_NUMBER = 5;

    private final EntityManagerFactory emf;

    public EconomicCustomerRoutes(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    public void getAll(Context ctx) {
        UserDTO authenticatedUser = ctx.attribute("user");
        boolean includeAdminDetails = hasRole(authenticatedUser, "ADMIN");
        try (EntityManager em = emf.createEntityManager()) {
            List<EconomicCustomerResponse> customers = em.createQuery("""
                            SELECT customer
                            FROM EconomicCustomer customer
                            WHERE customer.economicAvailable = true
                            ORDER BY customer.name ASC
                            """, EconomicCustomer.class)
                    .getResultList()
                    .stream()
                    .filter(customer -> shouldIncludeCustomer(authenticatedUser, customer))
                    .map(customer -> toDto(customer, includeAdminDetails))
                    .toList();
            ctx.json(customers);
        }
    }

    private boolean shouldIncludeCustomer(UserDTO authenticatedUser, EconomicCustomer customer) {
        if (hasRole(authenticatedUser, "ADMIN")) {
            return true;
        }

        return isCleaningCustomer(customer);
    }

    private boolean hasRole(UserDTO user, String roleName) {
        return user != null
                && user.getRoles() != null
                && user.getRoles().stream().anyMatch(role -> role.equalsIgnoreCase(roleName));
    }

    private boolean isCleaningCustomer(EconomicCustomer customer) {
        if (customer.getCustomerGroupNumber() != null
                && customer.getCustomerGroupNumber() == CLEANING_CUSTOMER_GROUP_NUMBER) {
            return true;
        }

        String groupName = normalize(customer.getCustomerGroupName());
        return groupName.contains("cleaning") || groupName.contains("rengoring");
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase();
    }

    private EconomicCustomerResponse toDto(EconomicCustomer customer, boolean includeAdminDetails) {
        EconomicCustomerResponse dto = new EconomicCustomerResponse();
        dto.setCustomerNumber(customer.getCustomerNumber());
        dto.setName(customer.getName());
        if (includeAdminDetails) {
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
        }
        if (customer.getCustomerGroupNumber() != null || customer.getCustomerGroupName() != null) {
            EconomicCustomerResponse.CustomerGroup group = new EconomicCustomerResponse.CustomerGroup();
            group.setCustomerGroupNumber(customer.getCustomerGroupNumber());
            group.setName(customer.getCustomerGroupName());
            dto.setCustomerGroup(group);
        }
        return dto;
    }
}
