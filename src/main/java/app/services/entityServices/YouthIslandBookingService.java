package app.services.entityServices;

import app.dao.UserDAO;
import app.dao.YouthIslandBookingDAO;
import app.entities.User;
import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingItem;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManagerFactory;

import java.time.LocalDate;
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
        if (booking == null || booking.getId() == null) {
            throw new ApiException(400, "Youth Island booking id is required");
        }
        validate(booking);
        YouthIslandBooking updated = bookingDAO.update(booking);
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

    private void validate(YouthIslandBooking booking) {
        if (booking == null) throw new ApiException(400, "Youth Island booking payload is required");
        if (booking.getOrderedBy() == null || booking.getOrderedBy().getId() == null) {
            throw new ApiException(400, "Ordering user is required");
        }
        if (isBlank(booking.getCustomerName())) throw new ApiException(400, "customerName is required");
        if (booking.getEventDate() == null) throw new ApiException(400, "eventDate is required");
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
            if (item.getServingTime() == null) throw new ApiException(400, "servingTime is required for every catering line");
            if (isBlank(item.getProductNumber())) throw new ApiException(400, "productNumber is required for every catering line");
            if (isBlank(item.getProductName())) throw new ApiException(400, "productName is required for every catering line");
            if (isBlank(item.getMenu())) throw new ApiException(400, "menu is required for every catering line");
            if (isBlank(item.getRoom())) throw new ApiException(400, "room is required for every catering line");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
