package app.dao;

import app.entities.CleaningAppointment;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CleaningAppointmentDAO implements IDAO<CleaningAppointment> {
    private final EntityManagerFactory emf;

    public CleaningAppointmentDAO(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
    }

    @Override
    public CleaningAppointment create(CleaningAppointment appointment) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            attachUsers(em, appointment);
            em.persist(appointment);
            em.getTransaction().commit();
            return getById(appointment.getId());
        }
    }

    @Override
    public CleaningAppointment getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ca FROM CleaningAppointment ca JOIN FETCH ca.cleaningClient JOIN FETCH ca.cleaningStaff WHERE ca.id = :id",
                            CleaningAppointment.class)
                    .setParameter("id", id)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    @Override
    public CleaningAppointment update(CleaningAppointment appointment) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            CleaningAppointment existing = em.find(CleaningAppointment.class, appointment.getId());
            if (existing == null) {
                em.getTransaction().commit();
                return null;
            }

            attachUsers(em, appointment);
            CleaningAppointment updated = em.merge(appointment);
            em.getTransaction().commit();
            return getById(updated.getId());
        }
    }

    @Override
    public CleaningAppointment delete(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();
            CleaningAppointment appointment = em.find(CleaningAppointment.class, id);
            if (appointment != null) {
                em.remove(appointment);
            }
            em.getTransaction().commit();
            return appointment;
        }
    }

    @Override
    public Set<CleaningAppointment> getAll() {
        try (EntityManager em = emf.createEntityManager()) {
            return new HashSet<>(em.createQuery(
                    "SELECT ca FROM CleaningAppointment ca JOIN FETCH ca.cleaningClient JOIN FETCH ca.cleaningStaff ORDER BY ca.appointmentTime",
                    CleaningAppointment.class
            ).getResultList());
        }
    }

    public List<CleaningAppointment> getByCleaningStaffId(Long cleaningStaffId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ca FROM CleaningAppointment ca JOIN FETCH ca.cleaningClient JOIN FETCH ca.cleaningStaff WHERE ca.cleaningStaff.id = :cleaningStaffId ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningStaffId", cleaningStaffId)
                    .getResultList();
        }
    }

    public List<CleaningAppointment> getByCleaningClientId(Long cleaningClientId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT ca FROM CleaningAppointment ca JOIN FETCH ca.cleaningClient JOIN FETCH ca.cleaningStaff WHERE ca.cleaningClient.id = :cleaningClientId ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningClientId", cleaningClientId)
                    .getResultList();
        }
    }

    private void attachUsers(EntityManager em, CleaningAppointment appointment) {
        User client = appointment.getCleaningClient();
        if (client == null || client.getId() == null) {
            throw new IllegalArgumentException("CleaningAppointment must reference a cleaning client");
        }

        User staff = appointment.getCleaningStaff();
        if (staff == null || staff.getId() == null) {
            throw new IllegalArgumentException("CleaningAppointment must reference a cleaning staff user");
        }

        User managedClient = em.find(User.class, client.getId());
        if (managedClient == null) {
            throw new IllegalArgumentException("User with id " + client.getId() + " does not exist");
        }

        User managedStaff = em.find(User.class, staff.getId());
        if (managedStaff == null) {
            throw new IllegalArgumentException("User with id " + staff.getId() + " does not exist");
        }

        appointment.setCleaningClient(managedClient);
        appointment.setCleaningStaff(managedStaff);
    }
}
