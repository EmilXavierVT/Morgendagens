package app.utils;

import app.dao.*;
import app.entities.*;
import app.entities.IEntity;
import app.services.apiServices.WeatherService;
import jakarta.persistence.EntityManagerFactory;

import java.util.ArrayList;
import java.util.Map;
import java.sql.Time;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class Populate {
    private final EntityManagerFactory emf;

    private final ProductDAO productDAO;
    private final TenantDAO tenantDAO;
    private final MessageDAO messageDAO;


    public Populate(EntityManagerFactory emf){
        this.emf = emf;
        this.productDAO = new ProductDAO(emf);
        this.tenantDAO = new TenantDAO(emf);
        this.messageDAO = new MessageDAO(emf);
    }

    public Map<String, IEntity> populate(){

        Tenant tenant = new Tenant().builder().name("test").type("default").status(1).build();

        User user = new User().builder()
                .email("<EMAIL>")
                .password("<PASSWORD>")
                .role(1).firstName("emil")
                .lastName("johnson")
                .phoneNumber("272727272")
                .zipCode(2920)
                .tenant(tenant)
                .build();
        tenant.getUsers().add(user);

        Product productA = new Product().builder()
                .name("Coffee")
                .description("Fresh brewed coffee")
                .price(25.0)
                .type(1)
                .build();

        Product productB = new Product().builder()
                .name("Sandwich")
                .description("Ham and cheese sandwich")
                .price(45.0)
                .type(2)
                .build();

        Request request = new Request().builder()
                .tenant(tenant)
                .type(1)
                .status(1)
                .startDate(LocalDateTime.now())
                .endDate(LocalDateTime.now().plusDays(1))
                .location("Copenhagen")
                .productsInRequest(new ArrayList<>())
                .build();

        tenant.getRequests().add(request);

        ProductInRequest pirA = new ProductInRequest().builder()
                .request(request)
                .product(productA)
                .time(Time.valueOf(LocalTime.of(9, 0)))
                .build();
        ProductInRequest pirB = new ProductInRequest().builder()
                .request(request)
                .product(productB)
                .time(Time.valueOf(LocalTime.of(12, 30)))
                .build();

        request.getProductsInRequest().add(pirA);
        request.getProductsInRequest().add(pirB);

        productA.getProductInRequests().add(pirA);
        productB.getProductInRequests().add(pirB);


        productDAO.create(productA);
        productDAO.create(productB);
        tenantDAO.create(tenant);

        Message message = new Message().builder()
                .user(user)
                .thread(1)
                .context("Welcome message")
                .date(java.time.LocalDateTime.now())
                .build();

        user.getMessages().add(message);

        messageDAO.create(message);

        return Map.of(
                "user", user,
                "tenant", tenant,
                "request", request,
                "productA", productA,
                "productB", productB,
                "message", message
        );
    }

}
