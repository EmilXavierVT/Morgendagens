package app.services.dtoConverter;

import app.dto.YouthIslandBookingDTO;
import app.dto.YouthIslandBookingItemDTO;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingItem;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class YouthIslandBookingMapper {
    public YouthIslandBookingDTO toDto(YouthIslandBooking booking) {
        if (booking == null) return null;

        YouthIslandBookingDTO dto = new YouthIslandBookingDTO();
        dto.setId(booking.getId());
        dto.setCustomerName(booking.getCustomerName());
        dto.setEventDate(booking.getEventDate());
        dto.setStartDateTime(booking.getStartDateTime());
        dto.setEndDateTime(booking.getEndDateTime());
        dto.setAllergies(booking.getAllergies());
        dto.setLocation(booking.getLocation());
        dto.setSpectraReservationNumber(booking.getSpectraReservationNumber());
        dto.setNotes(booking.getNotes());
        dto.setSnackTrayCount(booking.getSnackTrayCount());
        dto.setOrderDate(booking.getOrderDate());
        dto.setCreatedAt(booking.getCreatedAt());
        dto.setUpdatedAt(booking.getUpdatedAt());

        User orderedBy = booking.getOrderedBy();
        if (orderedBy != null) {
            dto.setOrderedByUserId(orderedBy.getId());
            dto.setOrderedByEmail(orderedBy.getEmail());
            dto.setOrderedByName(buildName(orderedBy));
        }

        List<YouthIslandBookingItemDTO> itemDtos = new ArrayList<>();
        int totalGuests = 0;
        for (YouthIslandBookingItem item : booking.getItems()) {
            itemDtos.add(new YouthIslandBookingItemDTO(
                    item.getId(),
                    item.getGuestCount(),
                    item.getServingTime(),
                    item.getProductNumber(),
                    item.getProductName(),
                    item.getMenu(),
                    item.getRoom()
            ));
            totalGuests += item.getGuestCount();
        }
        dto.setItems(itemDtos);
        dto.setTotalGuests(totalGuests);
        return dto;
    }

    public YouthIslandBooking fromDto(YouthIslandBookingDTO dto) {
        if (dto == null) return null;

        LocalDate eventDate = dto.getStartDateTime() != null
                ? dto.getStartDateTime().toLocalDate()
                : dto.getEventDate();

        YouthIslandBooking booking = YouthIslandBooking.builder()
                .id(dto.getId())
                .customerName(trimToNull(dto.getCustomerName()))
                .eventDate(eventDate)
                .startDateTime(dto.getStartDateTime())
                .endDateTime(dto.getEndDateTime())
                .allergies(trimToNull(dto.getAllergies()))
                .location(trimToNull(dto.getLocation()))
                .spectraReservationNumber(trimToNull(dto.getSpectraReservationNumber()))
                .notes(trimToNull(dto.getNotes()))
                .snackTrayCount(dto.getSnackTrayCount())
                .orderDate(dto.getOrderDate())
                .createdAt(dto.getCreatedAt())
                .updatedAt(dto.getUpdatedAt())
                .build();

        List<YouthIslandBookingItem> items = new ArrayList<>();
        if (dto.getItems() != null) {
            for (YouthIslandBookingItemDTO itemDto : dto.getItems()) {
                if (itemDto == null) continue;
                items.add(YouthIslandBookingItem.builder()
                        .id(itemDto.getId())
                        .guestCount(itemDto.getGuestCount())
                        .servingTime(itemDto.getServingTime())
                        .productNumber(trimToNull(itemDto.getProductNumber()))
                        .productName(trimToNull(itemDto.getProductName()))
                        .menu(trimToNull(itemDto.getMenu()))
                        .room(trimToNull(itemDto.getRoom()))
                        .build());
            }
        }
        booking.setItems(items);
        return booking;
    }

    private String buildName(User user) {
        String name = String.join(" ",
                user.getFirstName() == null ? "" : user.getFirstName().trim(),
                user.getLastName() == null ? "" : user.getLastName().trim()).trim();
        return name.isBlank() ? user.getEmail() : name;
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
