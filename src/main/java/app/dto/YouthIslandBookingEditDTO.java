package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.Instant;
import java.util.List;
import com.fasterxml.jackson.databind.JsonNode;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouthIslandBookingEditDTO {
    private Long id;
    private Long editedByUserId;
    private String editedByName;
    private String editedByEmail;
    private LocalDateTime editedAt;
    private Instant recordedAt;
    private Long version;
    private boolean detailsAvailable;
    private List<YouthIslandBookingChangeDTO> changes;
    private JsonNode beforeSnapshot;
    private JsonNode afterSnapshot;
}
