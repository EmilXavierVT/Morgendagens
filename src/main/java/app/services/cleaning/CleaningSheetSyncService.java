package app.services.cleaning;

import app.dto.CleaningSheetSyncResultDTO;
import app.entities.CleaningAppointment;
import app.entities.CleaningProject;
import app.entities.EconomicCustomer;
import app.exceptions.ApiException;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CleaningSheetSyncService {
    private static final int SCHEDULE_YEAR = 2026;
    private static final String PRIMARY_SHEET_ID = "1mSu2y2EooXwyOM07dhuQfZ_rhHxtF-mkmdcepTqaHaE";
    private static final String ARPITA_SHEET_ID = "1GvM5wNSWVOqvCVwkTBib0LY7W3xerLd2JPr4LDNmw6s";
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(?:[,.]\\d+)?)");
    private static final Map<String, Integer> CUSTOMER_NUMBERS = buildCustomerNumbers();
    private static final List<SheetSource> SHEET_SOURCES = List.of(
            new SheetSource(PRIMARY_SHEET_ID, List.of(
                    Month.JANUARY,
                    Month.FEBRUARY,
                    Month.MARCH,
                    Month.APRIL,
                    Month.MAY,
                    Month.JUNE,
                    Month.JULY,
                    Month.AUGUST,
                    Month.SEPTEMBER,
                    Month.OCTOBER,
                    Month.NOVEMBER,
                    Month.DECEMBER
            )),
            new SheetSource(ARPITA_SHEET_ID, List.of(
                    Month.MARCH,
                    Month.APRIL,
                    Month.MAY,
                    Month.JUNE,
                    Month.JULY,
                    Month.AUGUST,
                    Month.SEPTEMBER,
                    Month.OCTOBER,
                    Month.NOVEMBER,
                    Month.DECEMBER
            ))
    );

    private final EntityManagerFactory emf;
    private final HttpClient httpClient;

    public CleaningSheetSyncService(EntityManagerFactory emf) {
        if (emf == null) throw new IllegalArgumentException("EntityManagerFactory cannot be null");
        this.emf = emf;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public CleaningSheetSyncResultDTO sync() {
        SyncAccumulator accumulator = new SyncAccumulator();
        Map<AssignmentKey, SheetAssignment> assignments = new LinkedHashMap<>();

        try {
            for (SheetSource source : SHEET_SOURCES) {
                for (Month month : source.months()) {
                    List<List<String>> rows = fetchSheetRows(source.spreadsheetId(), month);
                    accumulator.sourceRows += rows.size();
                    for (SheetAssignment assignment : parseAssignments(month, rows, accumulator)) {
                        assignments.put(new AssignmentKey(assignment.customerNumber(), assignment.date()), assignment);
                    }
                }
            }
        } catch (IOException e) {
            throw new ApiException(502, "Could not fetch cleaning schedule sheets");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(502, "Cleaning schedule sheet sync was interrupted");
        }

        accumulator.parsedAssignments = assignments.size();
        upsertAssignments(assignments.values().stream()
                .sorted(Comparator.comparing(SheetAssignment::date).thenComparing(SheetAssignment::customerNumber))
                .toList(), accumulator);

        return new CleaningSheetSyncResultDTO(
                accumulator.sourceRows,
                accumulator.parsedAssignments,
                accumulator.createdAppointments,
                accumulator.updatedAppointments,
                accumulator.unchangedAppointments,
                accumulator.skippedAssignments
        );
    }

    private List<List<String>> fetchSheetRows(String spreadsheetId, Month month) throws IOException, InterruptedException {
        String sheetName = toSheetTitle(month);
        String encodedSheetName = URLEncoder.encode(sheetName, StandardCharsets.UTF_8);
        URI uri = URI.create("https://docs.google.com/spreadsheets/d/%s/gviz/tq?tqx=out:csv&sheet=%s"
                .formatted(spreadsheetId, encodedSheetName));

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        if (response.statusCode() != 200) {
            throw new IOException("Google Sheets CSV export returned " + response.statusCode());
        }

        return parseCsv(response.body());
    }

    private List<SheetAssignment> parseAssignments(Month month, List<List<String>> rows, SyncAccumulator accumulator) {
        List<SheetAssignment> assignments = new ArrayList<>();
        if (rows.isEmpty()) return assignments;

        List<String> headers = rows.get(0);
        for (int rowIndex = 1; rowIndex < rows.size(); rowIndex += 1) {
            List<String> row = rows.get(rowIndex);
            int day = parseDay(cell(row, 0));
            if (day <= 0 || day > month.length(false)) {
                continue;
            }

            LocalDate date = LocalDate.of(SCHEDULE_YEAR, month, day);
            for (int columnIndex = 1; columnIndex < headers.size(); columnIndex += 1) {
                String customerName = cell(headers, columnIndex);
                int durationMinutes = parseDurationMinutes(cell(row, columnIndex));
                if (customerName.isBlank() || durationMinutes <= 0) {
                    continue;
                }

                Integer customerNumber = CUSTOMER_NUMBERS.get(normalize(customerName));
                if (customerNumber == null) {
                    accumulator.skippedAssignments += 1;
                    continue;
                }

                assignments.add(new SheetAssignment(customerNumber, date, durationMinutes));
            }
        }

        return assignments;
    }

    private void upsertAssignments(List<SheetAssignment> assignments, SyncAccumulator accumulator) {
        try (EntityManager em = emf.createEntityManager()) {
            em.getTransaction().begin();

            try {
                for (SheetAssignment assignment : assignments) {
                    EconomicCustomer customer = findCustomer(em, assignment.customerNumber()).orElse(null);
                    if (customer == null) {
                        accumulator.skippedAssignments += 1;
                        continue;
                    }

                    CleaningProject project = findOrCreateProject(em, customer);
                    CleaningAppointment appointment = findAppointment(em, project, assignment.date()).orElse(null);
                    if (appointment == null) {
                        appointment = new CleaningAppointment();
                        appointment.setProject(project);
                        appointment.setAppointmentTime(assignment.date().atTime(9, 0));
                        appointment.setDurationMinutes(assignment.durationMinutes());
                        appointment.setVacation(false);
                        em.persist(appointment);
                        accumulator.createdAppointments += 1;
                        continue;
                    }

                    if (appointment.getDurationMinutes() != assignment.durationMinutes()) {
                        appointment.setDurationMinutes(assignment.durationMinutes());
                        accumulator.updatedAppointments += 1;
                    } else {
                        accumulator.unchangedAppointments += 1;
                    }
                }

                em.getTransaction().commit();
            } catch (RuntimeException e) {
                if (em.getTransaction().isActive()) {
                    em.getTransaction().rollback();
                }
                throw e;
            }
        }
    }

    private Optional<EconomicCustomer> findCustomer(EntityManager em, int customerNumber) {
        return em.createQuery(
                        "SELECT customer FROM EconomicCustomer customer " +
                                "WHERE customer.customerNumber = :customerNumber " +
                                "AND customer.economicAvailable = true",
                        EconomicCustomer.class)
                .setParameter("customerNumber", customerNumber)
                .getResultStream()
                .findFirst();
    }

    private CleaningProject findOrCreateProject(EntityManager em, EconomicCustomer customer) {
        List<CleaningProject> projects = em.createQuery(
                        "SELECT project FROM CleaningProject project " +
                                "WHERE project.economicCustomer.customerNumber = :customerNumber " +
                                "ORDER BY project.id",
                        CleaningProject.class)
                .setParameter("customerNumber", customer.getCustomerNumber())
                .setMaxResults(1)
                .getResultList();
        if (!projects.isEmpty()) {
            return projects.get(0);
        }

        CleaningProject project = new CleaningProject();
        project.setEconomicCustomer(customer);
        project.setName(projectName(customer));
        project.setActive(true);
        em.persist(project);
        return project;
    }

    private Optional<CleaningAppointment> findAppointment(EntityManager em, CleaningProject project, LocalDate date) {
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();

        return em.createQuery(
                        "SELECT appointment FROM CleaningAppointment appointment " +
                                "WHERE appointment.project.id = :projectId " +
                                "AND appointment.appointmentTime >= :start " +
                                "AND appointment.appointmentTime < :end " +
                                "AND appointment.vacation = false " +
                                "ORDER BY appointment.id",
                        CleaningAppointment.class)
                .setParameter("projectId", project.getId())
                .setParameter("start", start)
                .setParameter("end", end)
                .setMaxResults(1)
                .getResultStream()
                .findFirst();
    }

    private static List<List<String>> parseCsv(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean inQuotes = false;

        for (int index = 0; index < csv.length(); index += 1) {
            char current = csv.charAt(index);

            if (current == '"') {
                if (inQuotes && index + 1 < csv.length() && csv.charAt(index + 1) == '"') {
                    cell.append('"');
                    index += 1;
                } else {
                    inQuotes = !inQuotes;
                }
                continue;
            }

            if (current == ',' && !inQuotes) {
                row.add(cell.toString());
                cell.setLength(0);
                continue;
            }

            if ((current == '\n' || current == '\r') && !inQuotes) {
                if (current == '\r' && index + 1 < csv.length() && csv.charAt(index + 1) == '\n') {
                    index += 1;
                }
                row.add(cell.toString());
                rows.add(row);
                row = new ArrayList<>();
                cell.setLength(0);
                continue;
            }

            cell.append(current);
        }

        row.add(cell.toString());
        if (!row.isEmpty() && row.stream().anyMatch(value -> !value.isBlank())) {
            rows.add(row);
        }

        return rows;
    }

    private static String cell(List<String> row, int index) {
        if (row == null || index < 0 || index >= row.size()) return "";
        return row.get(index) == null ? "" : row.get(index).trim();
    }

    private static int parseDay(String value) {
        if (value == null || value.isBlank()) return 0;

        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int parseDurationMinutes(String value) {
        Matcher matcher = NUMBER_PATTERN.matcher(value == null ? "" : value);
        if (!matcher.find()) return 0;

        double hours = Double.parseDouble(matcher.group(1).replace(',', '.'));
        return (int) Math.round(hours * 60);
    }

    private static String projectName(EconomicCustomer customer) {
        String customerName = customer.getName() == null || customer.getName().isBlank()
                ? "Cleaning customer " + customer.getCustomerNumber()
                : customer.getName().trim();
        return customerName + " cleaning project";
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String toSheetTitle(Month month) {
        return month.name().substring(0, 1) + month.name().substring(1).toLowerCase(Locale.ROOT);
    }

    private static Map<String, Integer> buildCustomerNumbers() {
        Map<String, Integer> customerNumbers = new LinkedHashMap<>();
        customerNumbers.put("monica", 12);
        customerNumbers.put("pia", 13);
        customerNumbers.put("lise", 14);
        customerNumbers.put("maria", 15);
        customerNumbers.put("martin", 16);
        customerNumbers.put("rasmus", 17);
        customerNumbers.put("martin & carina", 18);
        customerNumbers.put("kirstine", 22);
        customerNumbers.put("morten", 24);
        customerNumbers.put("michel", 25);
        customerNumbers.put("hanne", 26);
        customerNumbers.put("mathias", 27);
        customerNumbers.put("sine", 28);
        return customerNumbers;
    }

    private record SheetSource(String spreadsheetId, List<Month> months) {}

    private record SheetAssignment(Integer customerNumber, LocalDate date, int durationMinutes) {}

    private record AssignmentKey(Integer customerNumber, LocalDate date) {}

    private static class SyncAccumulator {
        int sourceRows;
        int parsedAssignments;
        int createdAppointments;
        int updatedAppointments;
        int unchangedAppointments;
        int skippedAssignments;
    }
}
