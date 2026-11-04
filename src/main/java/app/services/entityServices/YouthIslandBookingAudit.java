package app.services.entityServices;

import app.dto.YouthIslandBookingChangeDTO;
import app.entities.YouthIslandBooking;
import app.services.dtoConverter.YouthIslandBookingMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class YouthIslandBookingAudit {
    private static final ObjectMapper JSON = new ObjectMapper().registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final Map<String, String> BOOKING_FIELDS = fields(
            "customerName", "Kunde eller arrangement", "startDateTime", "Start", "endDateTime", "Slut",
            "allergies", "Allergener", "location", "Lokale eller område", "spectraReservationNumber", "Spectra reservationsnummer",
            "notes", "Andet", "snackTrayCount", "Snackfade", "orderDate", "Bestillingsdato");
    private static final Map<String, String> ITEM_FIELDS = fields(
            "guestCount", "Antal", "servingDateTime", "Dato og tid", "productNumber", "Produktnummer",
            "productName", "Produktnavn", "unitPriceOverride", "Pris pr. person", "menu", "Menu", "room", "Lokale");

    private YouthIslandBookingAudit() {}

    public static String snapshot(YouthIslandBooking booking) {
        try {
            return JSON.writeValueAsString(new YouthIslandBookingMapper().toDto(booking));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not record booking version", e);
        }
    }

    public static JsonNode read(String snapshot) {
        if (snapshot == null) return null;
        try {
            return JSON.readTree(snapshot);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Could not read booking version", e);
        }
    }

    public static List<YouthIslandBookingChangeDTO> changes(JsonNode before, JsonNode after) {
        if (before == null || after == null) return List.of();
        List<YouthIslandBookingChangeDTO> changes = new ArrayList<>();
        addFieldChanges(changes, BOOKING_FIELDS, before, after, null, null);
        Map<Long, JsonNode> oldItems = itemsById(before);
        Map<Long, JsonNode> newItems = itemsById(after);
        var ids = new LinkedHashSet<>(oldItems.keySet());
        ids.addAll(newItems.keySet());
        for (Long id : ids) {
            JsonNode oldItem = oldItems.get(id);
            JsonNode newItem = newItems.get(id);
            String name = (newItem != null ? newItem : oldItem).path("productName").asText("Servering");
            if (oldItem == null || newItem == null) {
                changes.add(new YouthIslandBookingChangeDTO("item", oldItem == null ? "Servering tilføjet" : "Servering fjernet",
                        id, name, oldItem == null ? "ADDED" : "REMOVED", oldItem, newItem));
            } else {
                addFieldChanges(changes, ITEM_FIELDS, oldItem, newItem, id, name);
            }
        }
        return changes;
    }

    private static void addFieldChanges(List<YouthIslandBookingChangeDTO> changes, Map<String, String> fields,
                                       JsonNode before, JsonNode after, Long itemId, String itemName) {
        fields.forEach((field, label) -> {
            JsonNode oldValue = before.path(field);
            JsonNode newValue = after.path(field);
            if (!oldValue.equals(newValue)) changes.add(new YouthIslandBookingChangeDTO(
                    field, label, itemId, itemName, "UPDATED", oldValue, newValue));
        });
    }

    private static Map<Long, JsonNode> itemsById(JsonNode snapshot) {
        Map<Long, JsonNode> items = new LinkedHashMap<>();
        snapshot.path("items").forEach(item -> items.put(item.path("id").asLong(), item));
        return items;
    }

    private static Map<String, String> fields(String... pairs) {
        Map<String, String> fields = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) fields.put(pairs[i], pairs[i + 1]);
        return fields;
    }
}
