package app.dao;

import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingEdit;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class YouthIslandBookingDAO implements IDAO<YouthIslandBooking> {
    private final EntityManagerFactory emf;

    public YouthIslandBookingDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public YouthIslandBooking create(YouthIslandBooking booking) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            booking.setOrderedBy(em.getReference(app.entities.User.class, booking.getOrderedBy().getId()));
            booking.getItems().forEach(item -> item.setBooking(booking));
            em.persist(booking);
            em.getTransaction().commit();
            return getById(booking.getId());
        }
    }

    @Override
    public YouthIslandBooking getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT DISTINCT booking
                            FROM YouthIslandBooking booking
                            JOIN FETCH booking.orderedBy
                            LEFT JOIN FETCH booking.items
                            WHERE booking.id = :id
                            """, YouthIslandBooking.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public YouthIslandBooking update(YouthIslandBooking booking) {
        throw new IllegalArgumentException("An editor is required when updating a Youth Island booking");
    }

    public YouthIslandBooking update(YouthIslandBooking booking, User editor) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            YouthIslandBooking managed = em.find(YouthIslandBooking.class, booking.getId());
            if (managed == null) {
                em.getTransaction().rollback();
                return null;
            }

            managed.setCustomerName(booking.getCustomerName());
            managed.setEventDate(booking.getEventDate());
            managed.setStartDateTime(booking.getStartDateTime());
            managed.setEndDateTime(booking.getEndDateTime());
            managed.setAllergies(booking.getAllergies());
            managed.setLocation(booking.getLocation());
            managed.setSpectraReservationNumber(booking.getSpectraReservationNumber());
            managed.setNotes(booking.getNotes());
            managed.setSnackTrayCount(booking.getSnackTrayCount());
            managed.setOrderDate(booking.getOrderDate());
            booking.getItems().forEach(item -> item.setId(null));
            managed.setItems(booking.getItems());

            YouthIslandBookingEdit edit = YouthIslandBookingEdit.builder()
                    .booking(managed)
                    .editedBy(em.getReference(User.class, editor.getId()))
                    .build();
            em.persist(edit);

            em.getTransaction().commit();
            return getById(managed.getId());
        }
    }

    @Override
    public YouthIslandBooking delete(Long id) {
        throw new UnsupportedOperationException("Youth Island bookings are immutable");
    }

    @Override
    public Set<YouthIslandBooking> getAll() {
        return new LinkedHashSet<>(findBookings(null));
    }

    public List<YouthIslandBooking> getByOrderedByUserId(Long userId) {
        return findBookings(userId);
    }

    public List<YouthIslandBookingEdit> getEditHistory(Long bookingId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery("""
                            SELECT edit
                            FROM YouthIslandBookingEdit edit
                            JOIN FETCH edit.editedBy
                            WHERE edit.booking.id = :bookingId
                            ORDER BY edit.editedAt ASC, edit.id ASC
                            """, YouthIslandBookingEdit.class)
                    .setParameter("bookingId", bookingId)
                    .getResultList();
        }
    }

    private List<YouthIslandBooking> findBookings(Long userId) {
        try (EntityManager em = emf.createEntityManager()) {
            String ownershipClause = userId == null ? "" : "WHERE booking.orderedBy.id = :userId";
            var query = em.createQuery("""
                            SELECT DISTINCT booking
                            FROM YouthIslandBooking booking
                            JOIN FETCH booking.orderedBy
                            LEFT JOIN FETCH booking.items
                            %s
                            ORDER BY booking.eventDate DESC, booking.id DESC
                            """.formatted(ownershipClause), YouthIslandBooking.class);
            if (userId != null) query.setParameter("userId", userId);
            return query.getResultList();
        }
    }
}
