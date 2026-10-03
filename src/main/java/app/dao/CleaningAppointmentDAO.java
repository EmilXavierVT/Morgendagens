package app.dao;

import app.entities.CleaningAssignmentTask;
import app.entities.CleaningAppointment;
import app.entities.CleaningProject;
import app.entities.EconomicCustomer;
import app.entities.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
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
                                    "LEFT JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project project " +
                                    "LEFT JOIN FETCH project.economicCustomer " +
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
        return update(appointment, false);
    }

    public CleaningAppointment update(CleaningAppointment appointment, boolean applyTaskEditsToFutureAssignments) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            CleaningAppointment existing = em.find(CleaningAppointment.class, appointment.getId());
            if (existing == null) {
                em.getTransaction().commit();
                return null;
            }

            attachRelations(em, appointment);
            CleaningAppointment updated = applyUpdate(existing, appointment);
            if (applyTaskEditsToFutureAssignments) {
                applyTaskTemplateToFutureAssignments(em, updated);
            }
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
                            "LEFT JOIN FETCH ca.cleaningClient " +
                            "LEFT JOIN FETCH ca.cleaningStaff " +
                            "LEFT JOIN FETCH ca.project project " +
                            "LEFT JOIN FETCH project.economicCustomer " +
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
                                    "LEFT JOIN FETCH ca.cleaningClient " +
                                    "JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project project " +
                                    "LEFT JOIN FETCH project.economicCustomer " +
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
                                    "LEFT JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project project " +
                                    "LEFT JOIN FETCH project.economicCustomer " +
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
                                    "LEFT JOIN FETCH ca.cleaningClient " +
                                    "LEFT JOIN FETCH ca.cleaningStaff " +
                                    "LEFT JOIN FETCH ca.project project " +
                                    "LEFT JOIN FETCH project.economicCustomer " +
                                    "LEFT JOIN FETCH ca.tasks " +
                                    "WHERE ca.cleaningClient.id = :cleaningClientId ORDER BY ca.appointmentTime",
                            CleaningAppointment.class)
                    .setParameter("cleaningClientId", cleaningClientId)
                    .getResultList();
        }
    }

    private void attachRelations(EntityManager em, CleaningAppointment appointment) {
        User client = appointment.getCleaningClient();
        User managedClient = null;
        if (client != null && client.getId() != null) {
            managedClient = em.find(User.class, client.getId());
            if (managedClient == null) {
                throw new IllegalArgumentException("User with id " + client.getId() + " does not exist");
            }
        }

        if (managedClient == null && !hasEconomicCustomer(appointment.getProject())) {
            throw new IllegalArgumentException("CleaningAppointment must reference a cleaning client or e-conomic customer");
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
            if (managedClient != null && (project.getCleaningClient() == null || !project.getCleaningClient().getId().equals(managedClient.getId()))) {
                throw new IllegalArgumentException("Cleaning project must belong to the selected cleaning client");
            }
            return project;
        }

        if (hasEconomicCustomer(incomingProject)) {
            EconomicCustomer customer = resolveEconomicCustomer(em, incomingProject.getEconomicCustomer());
            return resolveEconomicProject(em, incomingProject, customer);
        }

        if (managedClient == null) {
            throw new IllegalArgumentException("Cleaning project requires a cleaning client or e-conomic customer");
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

    private boolean hasEconomicCustomer(CleaningProject incomingProject) {
        return incomingProject != null
                && incomingProject.getEconomicCustomer() != null
                && incomingProject.getEconomicCustomer().getCustomerNumber() != null;
    }

    private EconomicCustomer resolveEconomicCustomer(EntityManager em, EconomicCustomer incomingCustomer) {
        EconomicCustomer customer = em.createQuery(
                        "SELECT customer FROM EconomicCustomer customer WHERE customer.customerNumber = :customerNumber",
                        EconomicCustomer.class)
                .setParameter("customerNumber", incomingCustomer.getCustomerNumber())
                .getResultStream()
                .findFirst()
                .orElse(null);

        if (customer == null) {
            throw new IllegalArgumentException("e-conomic customer " + incomingCustomer.getCustomerNumber() + " does not exist locally");
        }

        return customer;
    }

    private CleaningProject resolveEconomicProject(EntityManager em, CleaningProject incomingProject, EconomicCustomer customer) {
        List<CleaningProject> existingProjects = em.createQuery(
                        "SELECT cp FROM CleaningProject cp WHERE cp.economicCustomer.customerNumber = :customerNumber ORDER BY cp.id",
                        CleaningProject.class)
                .setParameter("customerNumber", customer.getCustomerNumber())
                .setMaxResults(1)
                .getResultList();
        if (!existingProjects.isEmpty()) {
            CleaningProject project = existingProjects.get(0);
            if (incomingProject.getName() != null && !incomingProject.getName().isBlank()) {
                project.setName(incomingProject.getName().trim());
            }
            return project;
        }

        CleaningProject project = new CleaningProject();
        project.setEconomicCustomer(customer);
        project.setName(resolveEconomicProjectName(incomingProject, customer));
        project.setActive(true);
        em.persist(project);
        return project;
    }

    private String resolveEconomicProjectName(CleaningProject incomingProject, EconomicCustomer customer) {
        if (incomingProject != null && incomingProject.getName() != null && !incomingProject.getName().isBlank()) {
            return incomingProject.getName().trim();
        }

        String customerName = customer.getName() == null || customer.getName().isBlank()
                ? "Cleaning customer " + customer.getCustomerNumber()
                : customer.getName().trim();
        return customerName + " cleaning project";
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

    private void applyTaskTemplateToFutureAssignments(EntityManager em, CleaningAppointment source) {
        if (source.getProject() == null || source.getProject().getId() == null || source.getAppointmentTime() == null) {
            return;
        }

        List<CleaningAppointment> futureAppointments = em.createQuery(
                        "SELECT DISTINCT ca FROM CleaningAppointment ca " +
                                "LEFT JOIN FETCH ca.tasks " +
                                "WHERE ca.project.id = :projectId " +
                                "AND ca.id <> :sourceId " +
                                "AND ca.appointmentTime > :sourceTime " +
                                "ORDER BY ca.appointmentTime",
                        CleaningAppointment.class)
                .setParameter("projectId", source.getProject().getId())
                .setParameter("sourceId", source.getId())
                .setParameter("sourceTime", source.getAppointmentTime())
                .getResultList();

        for (CleaningAppointment futureAppointment : futureAppointments) {
            replaceTasksFromTemplate(futureAppointment, source.getTasks());
        }
    }

    private void replaceTasksFromTemplate(CleaningAppointment appointment, List<CleaningAssignmentTask> templateTasks) {
        Map<String, Boolean> completedByTitle = appointment.getTasks().stream()
                .filter(task -> task.getTitle() != null)
                .collect(Collectors.toMap(
                        task -> normalizeTaskTitle(task.getTitle()),
                        CleaningAssignmentTask::isCompleted,
                        (first, ignored) -> first
                ));

        List<CleaningAssignmentTask> nextTasks = templateTasks.stream()
                .filter(task -> task.getTitle() != null && !task.getTitle().isBlank())
                .sorted(Comparator.comparingInt(CleaningAssignmentTask::getSortOrder))
                .map(task -> {
                    CleaningAssignmentTask nextTask = new CleaningAssignmentTask();
                    nextTask.setAppointment(appointment);
                    nextTask.setTitle(task.getTitle().trim());
                    nextTask.setSortOrder(task.getSortOrder());
                    nextTask.setCompleted(completedByTitle.getOrDefault(normalizeTaskTitle(task.getTitle()), false));
                    return nextTask;
                })
                .filter(task -> !Objects.equals(task.getTitle(), ""))
                .toList();

        appointment.getTasks().clear();
        appointment.getTasks().addAll(nextTasks);
    }

    private String normalizeTaskTitle(String title) {
        return title == null ? "" : title.trim().toLowerCase(Locale.ROOT);
    }
}
