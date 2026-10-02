package app.dao;

import app.entities.CleaningAssignmentTask;
import app.entities.CleaningAppointment;
import app.entities.CleaningProject;
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
            attachRelations(em, appointment);
            em.persist(appointment);
            em.getTransaction().commit();
            return getById(appointment.getId());
        }
    }

    @Override
    public CleaningAppointment getById(Long id) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                                    "JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project " +
                                    "LEFT JOIN FETCH ca.tasks " +
                                    "WHERE ca.id = :id",
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

            attachRelations(em, appointment);
            CleaningAppointment updated = applyUpdate(existing, appointment);
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
                    "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                            "JOIN FETCH ca.cleaningClient " +
                            "LEFT JOIN FETCH ca.cleaningStaff " +
                            "LEFT JOIN FETCH ca.project " +
                            "LEFT JOIN FETCH ca.tasks " +
                            "ORDER BY ca.appointmentTime",
                    CleaningAppointment.class
            ).getResultList());
        }
    }

    public List<CleaningAppointment> getByCleaningStaffId(Long cleaningStaffId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                                    "JOIN FETCH ca.cleaningClient " +
                                    "JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project " +
                                    "LEFT JOIN FETCH ca.tasks " +
                                    "WHERE ca.cleaningStaff.id = :cleaningStaffId ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningStaffId", cleaningStaffId)
                    .getResultList();
        }
    }

    public List<CleaningAppointment> getVisibleToCleaningStaff(Long cleaningStaffId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                                    "JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project " +
                                    "LEFT JOIN FETCH ca.tasks " +
                                    "WHERE ca.cleaningStaff.id = :cleaningStaffId OR ca.cleaningStaff IS NULL ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningStaffId", cleaningStaffId)
                    .getResultList();
        }
    }

    public List<CleaningAppointment> getByCleaningClientId(Long cleaningClientId) {
        try (EntityManager em = emf.createEntityManager()) {
            return em.createQuery(
                            "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                                    "JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project " +
                                    "LEFT JOIN FETCH ca.tasks " +
                                    "WHERE ca.cleaningClient.id = :cleaningClientId ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningClientId", cleaningClientId)
                    .getResultList();
        }
    }

    private void attachRelations(EntityManager em, CleaningAppointment appointment) {
        User client = appointment.getCleaningClient();
        if (client == null || client.getId() == null) {
            throw new IllegalArgumentException("CleaningAppointment must reference a cleaning client");
        }

        User managedClient = em.find(User.class, client.getId());
        if (managedClient == null) {
            throw new IllegalArgumentException("User with id " + client.getId() + " does not exist");
        }

        appointment.setCleaningClient(managedClient);
        appointment.setProject(resolveProject(em, appointment.getProject(), managedClient));

        User staff = appointment.getCleaningStaff();
        if (staff == null || staff.getId() == null) {
            appointment.setCleaningStaff(null);
        } else {
            User managedStaff = em.find(User.class, staff.getId());
            if (managedStaff == null) {
                throw new IllegalArgumentException("User with id " + staff.getId() + " does not exist");
            }
            appointment.setCleaningStaff(managedStaff);
        }

        attachTasks(appointment);
    }

    private CleaningProject resolveProject(EntityManager em, CleaningProject incomingProject, User managedClient) {
        if (incomingProject != null && incomingProject.getId() != null) {
            CleaningProject project = em.find(CleaningProject.class, incomingProject.getId());
            if (project == null) {
                throw new IllegalArgumentException("Cleaning project with id " + incomingProject.getId() + " does not exist");
            }
            if (project.getCleaningClient() == null || !project.getCleaningClient().getId().equals(managedClient.getId())) {
                throw new IllegalArgumentException("Cleaning project must belong to the selected cleaning client");
            }
            return project;
        }

        List<CleaningProject> existingProjects = em.createQuery(
                        "SELECT cp FROM CleaningProject cp WHERE cp.cleaningClient.id = :clientId ORDER BY cp.id",
                        CleaningProject.class)
                .setParameter("clientId", managedClient.getId())
                .setMaxResults(1)
                .getResultList();
        if (!existingProjects.isEmpty()) {
            CleaningProject project = existingProjects.get(0);
            if (incomingProject != null && incomingProject.getName() != null && !incomingProject.getName().isBlank()) {
                project.setName(incomingProject.getName().trim());
            }
            return project;
        }

        CleaningProject project = new CleaningProject();
        project.setCleaningClient(managedClient);
        project.setName(resolveProjectName(incomingProject, managedClient));
        project.setActive(true);
        em.persist(project);
        return project;
    }

    private String resolveProjectName(CleaningProject incomingProject, User client) {
        if (incomingProject != null && incomingProject.getName() != null && !incomingProject.getName().isBlank()) {
            return incomingProject.getName().trim();
        }

        String clientName = String.join(" ",
                List.of(
                        client.getFirstName() == null ? "" : client.getFirstName().trim(),
                        client.getLastName() == null ? "" : client.getLastName().trim()
                )).trim();
        if (clientName.isBlank()) {
            clientName = client.getEmail() == null ? "Cleaning customer" : client.getEmail();
        }

        return clientName + " cleaning project";
    }

    private void attachTasks(CleaningAppointment appointment) {
        if (appointment.getTasks() == null) return;

        for (int index = 0; index < appointment.getTasks().size(); index += 1) {
            CleaningAssignmentTask task = appointment.getTasks().get(index);
            task.setAppointment(appointment);
            if (task.getSortOrder() <= 0) {
                task.setSortOrder(index + 1);
            }
        }
    }

    private CleaningAppointment applyUpdate(CleaningAppointment existing, CleaningAppointment incoming) {
        existing.setCleaningClient(incoming.getCleaningClient());
        existing.setCleaningStaff(incoming.getCleaningStaff());
        existing.setProject(incoming.getProject());
        existing.setAppointmentTime(incoming.getAppointmentTime());
        existing.setCancellationTime(incoming.getCancellationTime());
        existing.setDurationMinutes(incoming.getDurationMinutes());
        existing.setVacation(incoming.isVacation());
        existing.getTasks().clear();
        if (incoming.getTasks() != null) {
            for (CleaningAssignmentTask task : incoming.getTasks()) {
                task.setId(null);
                task.setAppointment(existing);
                existing.getTasks().add(task);
            }
        }
        return existing;
    }
}
