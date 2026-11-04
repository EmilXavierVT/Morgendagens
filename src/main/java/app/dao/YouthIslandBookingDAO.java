package app.dao;

import app.entities.YouthIslandBooking;
import app.entities.YouthIslandBookingEdit;
import app.entities.YouthIslandBookingItem;
import app.entities.User;
import app.exceptions.ApiException;
import app.services.entityServices.YouthIslandBookingAudit;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
        return update(booking, editor, null);
    }

    public YouthIslandBooking update(YouthIslandBooking booking, User editor, Long expectedVersion) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            try {
                YouthIslandBooking managed = em.find(YouthIslandBooking.class, booking.getId(), LockModeType.PESSIMISTIC_WRITE);
                if (managed == null) {
                    em.getTransaction().rollback();
                    return null;
                }

                long version = em.createQuery("SELECT COUNT(edit) FROM YouthIslandBookingEdit edit WHERE edit.booking.id = :id", Long.class)
                        .setParameter("id", booking.getId()).getSingleResult() + 1;
                if (expectedVersion != null && expectedVersion != version) {
                    throw new ApiException(409, "Booking was changed by another person. Reload it before saving your changes.");
                }
                String before = YouthIslandBookingAudit.snapshot(managed);

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
                updateItems(managed, booking.getItems());
                managed.setUpdatedAt(LocalDateTime.now());
                em.flush();

                String editorName = String.join(" ", editor.getFirstName() == null ? "" : editor.getFirstName().trim(),
                        editor.getLastName() == null ? "" : editor.getLastName().trim()).trim();

                YouthIslandBookingEdit edit = YouthIslandBookingEdit.builder()
                        .booking(managed)
                        .editedBy(em.getReference(User.class, editor.getId()))
                        .editedByName(editorName.isBlank() ? editor.getEmail() : editorName)
                        .editedByEmail(editor.getEmail())
                        .revision(version + 1)
                        .beforeSnapshot(before)
                        .afterSnapshot(YouthIslandBookingAudit.snapshot(managed))
                        .build();
                em.persist(edit);

                em.getTransaction().commit();
                return getById(managed.getId());
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) em.getTransaction().rollback();
                throw e;
            }
        }
    }

    private void updateItems(YouthIslandBooking managed, List<YouthIslandBookingItem> incoming) {
        Map<Long, YouthIslandBookingItem> existing = new LinkedHashMap<>();
        managed.getItems().forEach(item -> existing.put(item.getId(), item));
        Set<Long> used = new HashSet<>();
        List<YouthIslandBookingItem> items = new ArrayList<>();
        boolean legacyPayload = incoming.stream().allMatch(item -> item.getId() == null);
        for (YouthIslandBookingItem source : incoming) {
            YouthIslandBookingItem target = source.getId() == null ? null : existing.get(source.getId());
            if (source.getId() != null && (target == null || used.contains(source.getId()))) {
                throw new ApiException(400, "Serving line id does not belong to this booking or was supplied more than once");
            }
            if (target == null && legacyPayload) {
                target = existing.values().stream().filter(item -> !used.contains(item.getId()) && sameItem(item, source)).findFirst().orElse(null);
            }
            if (target == null) target = source;
            if (target.getId() != null) used.add(target.getId());
            target.setGuestCount(source.getGuestCount());
            target.setServingTime(source.getServingTime());
            target.setServingDateTime(source.getServingDateTime());
            target.setProductNumber(source.getProductNumber());
            target.setProductName(source.getProductName());
            target.setUnitPriceOverride(source.getUnitPriceOverride());
            target.setMenu(source.getMenu());
            target.setRoom(source.getRoom());
            items.add(target);
        }
        managed.setItems(items);
    }

    private boolean sameItem(YouthIslandBookingItem a, YouthIslandBookingItem b) {
        return a.getGuestCount() == b.getGuestCount()
                && Objects.equals(a.getServingDateTime(), b.getServingDateTime())
                && Objects.equals(a.getProductNumber(), b.getProductNumber())
                && Objects.equals(a.getProductName(), b.getProductName())
                && Objects.equals(a.getMenu(), b.getMenu()) && Objects.equals(a.getRoom(), b.getRoom())
                && Objects.equals(a.getUnitPriceOverride() == null ? null : a.getUnitPriceOverride().stripTrailingZeros(),
                                  b.getUnitPriceOverride() == null ? null : b.getUnitPriceOverride().stripTrailingZeros());
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
