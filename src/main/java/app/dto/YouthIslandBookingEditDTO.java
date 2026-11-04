package app.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class YouthIslandBookingEditDTO {
    private Long id;
    private Long editedByUserId;
    private String editedByName;
    private String editedByEmail;
    private LocalDateTime editedAt;
}
