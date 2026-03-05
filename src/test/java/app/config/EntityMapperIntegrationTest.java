package app.config;

import app.dto.*;
import app.entities.*;
import app.services.ApiServices.ObjectMapperService;
import app.services.dtoConverter.*;
import app.services.entityServices.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;


import jakarta.persistence.EntityManagerFactory;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

@Testcontainers
class EntityMapperIntegrationTest {
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    private static EntityManagerFactory emf;
    private static UserService userService;
    private static TenantService tenantService;
    private static ProductService productService;
    private static RequestService requestService;
    private static MessageService messageService;
    private static ProductInRequestService pirService;

    private static UserMapper userMapper;
    private static TenantMapper tenantMapper;
    private static ProductMapper productMapper;
    private static RequestMapper requestMapper;
    private static MessageMapper messageMapper;
    private static ProductInRequestMapper pirMapper;

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
      
        Properties props = HibernateBaseProperties.createBase();
        props.put("hibernate.connection.url", postgres.getJdbcUrl());
        props.put("hibernate.connection.username", postgres.getUsername());
        props.put("hibernate.connection.password", postgres.getPassword());
        props.put("hibernate.hbm2ddl.auto", "create-drop");
        emf = HibernateEmfBuilder.build(props);

        userService = new UserService(emf);
        tenantService = new TenantService(emf);
        productService = new ProductService(emf);
        requestService = new RequestService(emf);
        messageService = new MessageService(emf);
        pirService = new ProductInRequestService(emf);

        userMapper = new UserMapper(emf);
        tenantMapper = new TenantMapper(emf);
        productMapper = new ProductMapper(emf);
        requestMapper = new RequestMapper(emf);
        messageMapper = new MessageMapper(emf);
        pirMapper = new ProductInRequestMapper(emf);

        objectMapper = ObjectMapperService.getMapper();
    }

    @AfterAll
    static void teardown() {
        if (emf != null) {
            emf.close();
        }
    }

    @Test
    void unit_toDto_fromDto_roundtrip() {
        // Create fresh tenant and user to exercise both sides of mapper
        Tenant tenant = Tenant.builder().name("t1").type("x").status(1).build();
        tenantService.create(tenant);

        User user = User.builder()
                .email("a@b.com")
                .password("pw")
                .role(2)
                .firstName("first")
                .lastName("last")
                .phoneNumber("123")
                .zipCode(9999)
                .tenant(tenant)
                .build();
        userService.create(user);

        // dto -> entity -> dto conversion
        UserDTO dto = userMapper.toDto(user);
        assertEquals(user.getEmail(), dto.getEmail());
        assertEquals(user.getTenant().getId(), dto.getTenantId());

        User reconstructed = userMapper.fromDto(dto);
        assertEquals(user.getEmail(), reconstructed.getEmail());
        // NOTE: fromDto fetches tenant using service; because we persisted earlier, should not be null
        assertNotNull(reconstructed.getTenant());
        assertEquals(tenant.getId(), reconstructed.getTenant().getId());
    }

    @Test
    void persistence_json_to_entity() throws Exception {
        // prepare referenced data first
        Tenant tenant = Tenant.builder().name("persist").type("y").status(2).build();
        tenantService.create(tenant);
        Product product = Product.builder().name("prod").description("d").price(5.0).type(1).build();
        productService.create(product);

        // build JSON for a Request that refers to the tenant and product in request
        String json = "{" +
                "\"tenantId\":" + tenant.getId() + "," +
                "\"startDate\":123456789," +
                "\"endDate\":987654321," +
                "\"status\":3," +
                "\"type\":4" +
                // productInRequestIds will be empty initially
                "}";

        RequestDTO reqDto = objectMapper.readValue(json, RequestDTO.class);
        Request reqEntity = requestMapper.fromDto(reqDto);
        // at this point reqEntity has tenant set but not persisted yet
        requestService.create(reqEntity);
        assertNotNull(reqEntity.getId());
        assertEquals(tenant.getId(), reqEntity.getTenant().getId());
        assertEquals(123456789L, reqEntity.getStartDate());
    }
}
