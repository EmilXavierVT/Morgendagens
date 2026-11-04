package app.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserDTOTest {
    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void passwords_are_accepted_as_input_but_never_serialized_or_logged() throws Exception {
        UserDTO user = mapper.readValue("{\"email\":\"test@example.com\",\"password\":\"private-password\"}", UserDTO.class);
        assertEquals("private-password", user.getPassword());
        assertFalse(mapper.readTree(mapper.writeValueAsString(user)).has("password"));
        assertFalse(user.toString().contains("private-password"));

        UserUpdateDTO update = mapper.readValue("{\"password\":\"private-password\"}", UserUpdateDTO.class);
        assertEquals("private-password", update.getPassword());
        assertFalse(mapper.readTree(mapper.writeValueAsString(update)).has("password"));
        assertFalse(update.toString().contains("private-password"));
    }
}
