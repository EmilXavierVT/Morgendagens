package app.utils;

import app.dao.*;
import app.entities.Product;
import app.entities.Request;
import app.entities.Tenant;
import app.entities.User;
import app.persistence.IEntity;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.Map;

public class Populate {
    private final EntityManagerFactory emf;

    private final UserDAO userDAO;
    private final ProductDAO productDAO;
    private final TenantDAO tenantDAO;
    private final RequestDAO requestDAO;
    private final MessageDAO messageDAO;


    public Populate(EntityManagerFactory emf){
        this.emf = emf;
        this.userDAO = new UserDAO(emf);
        this.productDAO = new ProductDAO(emf);
        this.tenantDAO = new TenantDAO(emf);
        this.requestDAO = new RequestDAO(emf);
        this.messageDAO = new MessageDAO(emf);
    }

    public Map<String, IEntity> populate(){

        Tenant tenant = new Tenant().builder().name("test").build();
        User user = new User().builder()
                .email("<EMAIL>")
                .password("<PASSWORD>")
                .role(1).firstName("emil")
                .lastName("johnson")
                .phoneNumber("272727272")
                .zipCode(2920)
                .tenant(tenant)
                .build();
        tenantDAO.create(tenant);
        userDAO.create(user);

        Request request = new Request().builder().type(1).productsInRequest(new ArrayList<>()).build();

        return Map.of("user",user,"tenant",tenant, "request",request);
    }

}
