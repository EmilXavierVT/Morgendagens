package app.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record YouthIslandBookingChangeDTO(
        String field, String label, Long itemId, String itemName, String type,
        JsonNode before, JsonNode after
) {}
