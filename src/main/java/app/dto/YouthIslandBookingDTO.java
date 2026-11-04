package app.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonIgnoreProperties(ignoreUnknown = true)
public class YouthIslandBookingDTO {
    private Long id;
    private Long version;
    private Long orderedByUserId;
    private String orderedByName;
    private String orderedByEmail;
    private String customerName;
    private LocalDate eventDate;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private String allergies;
    private String location;
    private String spectraReservationNumber;
    private String notes;
    private int snackTrayCount;
    private LocalDate orderDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private int totalGuests;
    private List<YouthIslandBookingItemDTO> items = new ArrayList<>();
    private List<YouthIslandBookingEditDTO> editHistory = new ArrayList<>();
}
