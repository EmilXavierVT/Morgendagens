package app.services.entityServices;

import app.dao.UserDAO;
import app.dao.YouthIslandBookingDAO;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingItem;
import app.entities.YouthIslandBookingEdit;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class YouthIslandBookingService implements CrudService<YouthIslandBooking> {
    private final YouthIslandBookingDAO bookingDAO;
    private final UserDAO userDAO;

    public YouthIslandBookingService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.bookingDAO = new YouthIslandBookingDAO(emf);
        this.userDAO = new UserDAO(emf);
    }

    @Override
    public YouthIslandBooking create(YouthIslandBooking booking) {
        validate(booking);

        User orderedBy = userDAO.getByIdWithRoles(booking.getOrderedBy().getId());
        if (orderedBy == null) throw new ApiException(404, "Ordering user not found");
        booking.setOrderedBy(orderedBy);

        if (booking.getOrderDate() == null) booking.setOrderDate(LocalDate.now());
        return bookingDAO.create(booking);
    }

    @Override
    public YouthIslandBooking getById(Long id) {
        return bookingDAO.getById(id);
    }

    @Override
    public YouthIslandBooking update(YouthIslandBooking booking) {
        throw new ApiException(400, "The user editing the Youth Island booking is required");
    }

    public YouthIslandBooking update(YouthIslandBooking booking, User editor) {
        return update(booking, editor, null);
    }

    public YouthIslandBooking update(YouthIslandBooking booking, User editor, Long expectedVersion) {
        if (booking == null || booking.getId() == null) {
            throw new ApiException(400, "Youth Island booking id is required");
        }
        if (editor == null || editor.getId() == null) {
            throw new ApiException(400, "The user editing the Youth Island booking is required");
        }
        validate(booking);
        YouthIslandBooking updated = bookingDAO.update(booking, editor, expectedVersion);
        if (updated == null) throw new ApiException(404, "Youth Island booking not found");
        return updated;
    }

    @Override
    public YouthIslandBooking delete(Long id) {
        throw new ApiException(405, "Youth Island bookings cannot be deleted");
    }

    @Override
    public Set<YouthIslandBooking> getAll() {
        return bookingDAO.getAll();
    }

    public List<YouthIslandBooking> getByOrderedByUserId(Long userId) {
        return bookingDAO.getByOrderedByUserId(userId);
    }

    public List<YouthIslandBookingEdit> getEditHistory(Long bookingId) {
        return bookingDAO.getEditHistory(bookingId);
    }

    private void validate(YouthIslandBooking booking) {
        if (booking == null) throw new ApiException(400, "Youth Island booking payload is required");
        if (booking.getOrderedBy() == null || booking.getOrderedBy().getId() == null) {
            throw new ApiException(400, "Ordering user is required");
        }
        if (isBlank(booking.getCustomerName())) throw new ApiException(400, "customerName is required");
        if (booking.getEventDate() == null) throw new ApiException(400, "eventDate is required");
        LocalDateTime start = booking.getStartDateTime();
        LocalDateTime end = booking.getEndDateTime();
        if (start == null || end == null) {
            throw new ApiException(400, "startDateTime and endDateTime are required");
        }
        if (!end.isAfter(start)) {
            throw new ApiException(400, "endDateTime must be after startDateTime");
        }
        if (isBlank(booking.getSpectraReservationNumber())) {
            throw new ApiException(400, "spectraReservationNumber is required");
        }
        if (booking.getSnackTrayCount() < 0) {
            throw new ApiException(400, "snackTrayCount cannot be negative");
        }
        if (booking.getItems() == null || booking.getItems().isEmpty()) {
            throw new ApiException(400, "At least one catering line is required");
        }

        for (YouthIslandBookingItem item : booking.getItems()) {
            if (item.getGuestCount() <= 0) throw new ApiException(400, "Each catering line needs at least one guest");
            LocalDateTime servingDateTime = item.getServingDateTime();
            if (servingDateTime == null) throw new ApiException(400, "servingDateTime is required for every catering line");
            item.setServingDateTime(servingDateTime);
            item.setServingTime(servingDateTime.toLocalTime());
            if (isBlank(item.getProductNumber())) throw new ApiException(400, "productNumber is required for every catering line");
            if (isBlank(item.getProductName())) throw new ApiException(400, "productName is required for every catering line");
            boolean diverseProduct = item.isSpecialServing();
            if (diverseProduct && item.getUnitPriceOverride() == null) {
                throw new ApiException(400, "unitPriceOverride is required for DIVERSE catering lines");
            }
            if (!diverseProduct && item.getUnitPriceOverride() != null) {
                throw new ApiException(400, "unitPriceOverride is only allowed for DIVERSE catering lines");
            }
            if (item.getUnitPriceOverride() != null && item.getUnitPriceOverride().signum() < 0) {
                throw new ApiException(400, "unitPriceOverride cannot be negative");
            }
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
