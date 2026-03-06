package app.services.apiServices;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;


public class ObjectMapperService {

    private static final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    public static ObjectMapper getMapper(){
        return objectMapper;
    }

}
