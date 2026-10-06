import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.regex.*;

record ScheduleEntry(String customerName, int month, int day, String hoursText) {}

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

List<ScheduleEntry> entries = List.of(
    new ScheduleEntry("Monica", 1, 2, "2"),
    new ScheduleEntry("Lise", 1, 6, "2,5"),
    new ScheduleEntry("Martin", 1, 6, "2"),
    new ScheduleEntry("Martin & Carina", 1, 7, "2,5"),
    new ScheduleEntry("Pia", 1, 8, "3"),
    new ScheduleEntry("Rasmus", 1, 9, "4"),
    new ScheduleEntry("Maria", 1, 10, "2,5"),
    new ScheduleEntry("Lise", 1, 13, "2"),
    new ScheduleEntry("Martin", 1, 13, "2"),
    new ScheduleEntry("Monica", 1, 16, "2,5"),
    new ScheduleEntry("Rasmus", 1, 16, "2,5"),
    new ScheduleEntry("Kirstine", 1, 22, "2"),
    new ScheduleEntry("Pia", 1, 23, "3"),
    new ScheduleEntry("Martin & Carina", 1, 23, "2,5"),
    new ScheduleEntry("Lise", 1, 27, "3"),
    new ScheduleEntry("Martin", 1, 27, "2,5"),
    new ScheduleEntry("Monica", 1, 30, "2"),
    new ScheduleEntry("Rasmus", 1, 30, "3"),
    new ScheduleEntry("Lise", 2, 3, "2,5"),
    new ScheduleEntry("Martin & Carina", 2, 3, "2,5"),
    new ScheduleEntry("Kirstine", 2, 3, "2"),
    new ScheduleEntry("Pia", 2, 5, "3"),
    new ScheduleEntry("Rasmus", 2, 6, "3"),
    new ScheduleEntry("Maria", 2, 9, "5"),
    new ScheduleEntry("Rasmus", 2, 11, "3"),
    new ScheduleEntry("Lise", 2, 16, "2,5"),
    new ScheduleEntry("Martin", 2, 16, "2,5"),
    new ScheduleEntry("Pia", 2, 18, "3"),
    new ScheduleEntry("Martin & Carina", 2, 18, "2,5"),
    new ScheduleEntry("Monica", 2, 20, "2"),
    new ScheduleEntry("Rasmus", 2, 20, "3"),
    new ScheduleEntry("Lise", 2, 24, "2,5"),
    new ScheduleEntry("Martin", 2, 24, "2"),
    new ScheduleEntry("Lise", 3, 3, "2,5"),
    new ScheduleEntry("Martin", 3, 3, "2"),
    new ScheduleEntry("Pia", 3, 5, "3"),
    new ScheduleEntry("Maria", 3, 5, "2,5"),
    new ScheduleEntry("Kirstine", 3, 5, "2"),
    new ScheduleEntry("Monica", 3, 6, "2,5"),
    new ScheduleEntry("Rasmus", 3, 6, "3"),
    new ScheduleEntry("Lise", 3, 10, "2,5"),
    new ScheduleEntry("Martin", 3, 10, "2"),
    new ScheduleEntry("Kirstine", 3, 11, "2"),
    new ScheduleEntry("Rasmus", 3, 13, "3"),
    new ScheduleEntry("Lise", 3, 17, "2,5"),
    new ScheduleEntry("Martin", 3, 17, "2"),
    new ScheduleEntry("Kirstine", 3, 18, "2"),
    new ScheduleEntry("Morten", 3, 18, "3"),
    new ScheduleEntry("Pia", 3, 19, "3"),
    new ScheduleEntry("Maria", 3, 19, "2,5"),
    new ScheduleEntry("Monica", 3, 20, "2,5"),
    new ScheduleEntry("Rasmus", 3, 20, "3"),
    new ScheduleEntry("Lise", 3, 25, "2,5"),
    new ScheduleEntry("Martin", 3, 25, "2"),
    new ScheduleEntry("Martin & Carina", 3, 26, "3,5"),
    new ScheduleEntry("Rasmus", 3, 27, "3"),
    new ScheduleEntry("Pia", 3, 30, "3,5"),
    new ScheduleEntry("Kirstine", 3, 31, "2"),
    new ScheduleEntry("Michel", 3, 31, "3"),
    new ScheduleEntry("Martin", 4, 1, "2"),
    new ScheduleEntry("Pia", 4, 2, "3"),
    new ScheduleEntry("Maria", 4, 2, "2,5"),
    new ScheduleEntry("Lise", 4, 7, "2,5"),
    new ScheduleEntry("Martin", 4, 7, "2"),
    new ScheduleEntry("Rasmus", 4, 10, "3"),
    new ScheduleEntry("Lise", 4, 14, "2,5"),
    new ScheduleEntry("Martin", 4, 14, "2"),
    new ScheduleEntry("Martin & Carina", 4, 15, "2,5"),
    new ScheduleEntry("Kirstine", 4, 15, "2"),
    new ScheduleEntry("Pia", 4, 16, "3"),
    new ScheduleEntry("Maria", 4, 16, "2,5"),
    new ScheduleEntry("Monica", 4, 17, "2,5"),
    new ScheduleEntry("Rasmus", 4, 17, "3"),
    new ScheduleEntry("Lise", 4, 21, "2,5"),
    new ScheduleEntry("Martin", 4, 21, "2"),
    new ScheduleEntry("Rasmus", 4, 24, "2,5"),
    new ScheduleEntry("Lise", 4, 28, "2,5"),
    new ScheduleEntry("Martin", 4, 28, "2"),
    new ScheduleEntry("Martin & Carina", 4, 29, "2,5"),
    new ScheduleEntry("Kirstine", 4, 29, "2"),
    new ScheduleEntry("Monica", 5, 1, "2"),
    new ScheduleEntry("Rasmus", 5, 1, "3"),
    new ScheduleEntry("Lise", 5, 5, "2,5"),
    new ScheduleEntry("Martin", 5, 5, "2"),
    new ScheduleEntry("Rasmus", 5, 8, "2,5"),
    new ScheduleEntry("Lise", 5, 12, "2,5"),
    new ScheduleEntry("Martin", 5, 12, "2"),
    new ScheduleEntry("Maria", 5, 13, "3"),
    new ScheduleEntry("Martin & Carina", 5, 13, "2,5"),
    new ScheduleEntry("Kirstine", 5, 13, "2"),
    new ScheduleEntry("Monica", 5, 15, "2"),
    new ScheduleEntry("Rasmus", 5, 15, "3"),
    new ScheduleEntry("Lise", 5, 19, "2,5"),
    new ScheduleEntry("Martin", 5, 19, "2"),
    new ScheduleEntry("Pia", 5, 20, "3"),
    new ScheduleEntry("Rasmus", 5, 22, "3"),
    new ScheduleEntry("Lise", 5, 26, "2,5"),
    new ScheduleEntry("Martin", 5, 26, "2"),
    new ScheduleEntry("Martin & Carina", 5, 27, "2,5"),
    new ScheduleEntry("Kirstine", 5, 27, "2"),
    new ScheduleEntry("Pia", 5, 28, "3"),
    new ScheduleEntry("Maria", 5, 28, "3"),
    new ScheduleEntry("Monica", 5, 29, "3,5"),
    new ScheduleEntry("Rasmus", 5, 29, "3,5"),
    new ScheduleEntry("Lise", 6, 2, "2,5"),
    new ScheduleEntry("Martin", 6, 2, "2"),
    new ScheduleEntry("Rasmus", 6, 5, "3"),
    new ScheduleEntry("Lise", 6, 10, "2,5"),
    new ScheduleEntry("Martin", 6, 10, "2"),
    new ScheduleEntry("Pia", 6, 11, "3,5"),
    new ScheduleEntry("Maria", 6, 11, "3"),
    new ScheduleEntry("Martin & Carina", 6, 11, "2,5"),
    new ScheduleEntry("Kirstine", 6, 11, "2"),
    new ScheduleEntry("Monica", 6, 12, "2,5"),
    new ScheduleEntry("Rasmus", 6, 12, "3"),
    new ScheduleEntry("Lise", 6, 16, "2,5"),
    new ScheduleEntry("Martin", 6, 16, "2"),
    new ScheduleEntry("Rasmus", 6, 19, "3"),
    new ScheduleEntry("Lise", 6, 23, "2"),
    new ScheduleEntry("Martin", 6, 23, "2"),
    new ScheduleEntry("Martin & Carina", 6, 24, "2,5"),
    new ScheduleEntry("Kirstine", 6, 24, "2"),
    new ScheduleEntry("Pia", 6, 25, "3"),
    new ScheduleEntry("Maria", 6, 25, "3"),
    new ScheduleEntry("Monica", 6, 26, "2,5"),
    new ScheduleEntry("Rasmus", 6, 26, "3"),
    new ScheduleEntry("Maria", 7, 9, "2,5"),
    new ScheduleEntry("Rasmus", 7, 17, "4"),
    new ScheduleEntry("Pia", 7, 21, "3"),
    new ScheduleEntry("Martin & Carina", 7, 22, "2,5"),
    new ScheduleEntry("Lise", 7, 23, "2,5"),
    new ScheduleEntry("Maria", 7, 23, "2,5"),
    new ScheduleEntry("Kirstine", 7, 23, "2"),
    new ScheduleEntry("Rasmus", 7, 27, "2,5"),
    new ScheduleEntry("Lise", 8, 11, "2,5"),
    new ScheduleEntry("Martin", 8, 11, "3,5"),
    new ScheduleEntry("Rasmus", 8, 14, "3,5"),
    new ScheduleEntry("Lise", 8, 18, "2,5"),
    new ScheduleEntry("Martin", 8, 18, "2"),
    new ScheduleEntry("Martin & Carina", 8, 19, "2,5"),
    new ScheduleEntry("Kirstine", 8, 19, "2"),
    new ScheduleEntry("Pia", 8, 20, "3"),
    new ScheduleEntry("Maria", 8, 20, "3"),
    new ScheduleEntry("Monica", 8, 21, "3"),
    new ScheduleEntry("Rasmus", 8, 21, "3"),
    new ScheduleEntry("Lise", 8, 25, "2,5"),
    new ScheduleEntry("Martin", 8, 25, "2"),
    new ScheduleEntry("Rasmus", 8, 27, "2,5"),
    new ScheduleEntry("Lise", 9, 1, "2,5"),
    new ScheduleEntry("Martin", 9, 1, "2"),
    new ScheduleEntry("Martin & Carina", 9, 2, "2,5"),
    new ScheduleEntry("Kirstine", 9, 2, "2"),
    new ScheduleEntry("Pia", 9, 3, "3"),
    new ScheduleEntry("Maria", 9, 3, "3"),
    new ScheduleEntry("Monica", 9, 4, "2"),
    new ScheduleEntry("Rasmus", 9, 4, "2,5"),
    new ScheduleEntry("Lise", 9, 8, "2,5"),
    new ScheduleEntry("Martin", 9, 8, "2"),
    new ScheduleEntry("Lise", 9, 15, "2,5"),
    new ScheduleEntry("Martin", 9, 15, "2"),
    new ScheduleEntry("Martin & Carina", 9, 16, "2,5"),
    new ScheduleEntry("Kirstine", 9, 16, "2"),
    new ScheduleEntry("Pia", 9, 17, "3"),
    new ScheduleEntry("Maria", 9, 17, "3"),
    new ScheduleEntry("Monica", 9, 18, "2,5"),
    new ScheduleEntry("Rasmus", 9, 18, "3"),
    new ScheduleEntry("Lise", 9, 22, "2,5"),
    new ScheduleEntry("Martin", 9, 22, "2"),
    new ScheduleEntry("Rasmus", 9, 25, "3,5"),
    new ScheduleEntry("Lise", 9, 28, "2,5"),
    new ScheduleEntry("Martin", 9, 29, "2"),
    new ScheduleEntry("Kirstine", 9, 29, "2"),
    new ScheduleEntry("Martin & Carina", 9, 30, "2,5"),
    new ScheduleEntry("Pia", 10, 1, "3"),
    new ScheduleEntry("Maria", 10, 1, "3"),
    new ScheduleEntry("Monica", 10, 2, "2,5"),
    new ScheduleEntry("Rasmus", 10, 2, "3"),
    new ScheduleEntry("Morten", 4, 1, "3"),
    new ScheduleEntry("Hanne", 4, 5, "3,5"),
    new ScheduleEntry("Michel", 4, 14, "2"),
    new ScheduleEntry("Morten", 4, 16, "3"),
    new ScheduleEntry("Michel", 4, 27, "2"),
    new ScheduleEntry("Morten", 4, 30, "3"),
    new ScheduleEntry("Hanne", 5, 5, "5"),
    new ScheduleEntry("Michel", 5, 12, "2"),
    new ScheduleEntry("Hanne", 5, 12, "2,5"),
    new ScheduleEntry("Hanne", 5, 19, "2,5"),
    new ScheduleEntry("Morten", 5, 21, "3"),
    new ScheduleEntry("Michel", 5, 26, "2"),
    new ScheduleEntry("Hanne", 5, 26, "2"),
    new ScheduleEntry("Hanne", 6, 2, "2,5"),
    new ScheduleEntry("Michel", 6, 9, "2,5"),
    new ScheduleEntry("Hanne", 6, 9, "2,5"),
    new ScheduleEntry("Morten", 6, 11, "3"),
    new ScheduleEntry("Hanne", 6, 16, "2,5"),
    new ScheduleEntry("Michel", 6, 23, "2"),
    new ScheduleEntry("Hanne", 6, 23, "2,5"),
    new ScheduleEntry("Morten", 6, 25, "3"),
    new ScheduleEntry("Hanne", 6, 30, "2,5"),
    new ScheduleEntry("Michel", 7, 7, "2,5"),
    new ScheduleEntry("Hanne", 7, 7, "2,5"),
    new ScheduleEntry("Morten", 7, 9, "3"),
    new ScheduleEntry("Hanne", 7, 14, "2,5"),
    new ScheduleEntry("Michel", 8, 4, "2"),
    new ScheduleEntry("Hanne", 8, 4, "2,5"),
    new ScheduleEntry("Morten", 8, 6, "3"),
    new ScheduleEntry("Hanne", 8, 11, "2,5"),
    new ScheduleEntry("Michel", 8, 18, "2"),
    new ScheduleEntry("Hanne", 8, 18, "2,5"),
    new ScheduleEntry("Morten", 8, 20, "3"),
    new ScheduleEntry("Hanne", 8, 25, "2,5"),
    new ScheduleEntry("Mathias", 8, 27, "5"),
    new ScheduleEntry("Sine", 8, 29, "6"),
    new ScheduleEntry("Michel", 9, 1, "2"),
    new ScheduleEntry("Hanne", 9, 2, "2,5"),
    new ScheduleEntry("Mathias", 9, 3, "2"),
    new ScheduleEntry("Hanne", 9, 8, "2,5"),
    new ScheduleEntry("Mathias", 9, 10, "2"),
    new ScheduleEntry("Sine", 9, 10, "4"),
    new ScheduleEntry("Michel", 9, 15, "2"),
    new ScheduleEntry("Hanne", 9, 15, "2,5"),
    new ScheduleEntry("Morten", 9, 17, "3"),
    new ScheduleEntry("Mathias", 9, 17, "2"),
    new ScheduleEntry("Hanne", 9, 21, "2,5"),
    new ScheduleEntry("Mathias", 9, 23, "2"),
    new ScheduleEntry("Sine", 9, 23, "4"),
    new ScheduleEntry("Morten", 9, 30, "3"),
    new ScheduleEntry("Mathias", 10, 1, "2")
);

Pattern numberPattern = Pattern.compile("(\\d+(?:[,.]\\d+)?)");

String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
}

int durationMinutes(String hoursText) {
    Matcher matcher = numberPattern.matcher(hoursText == null ? "" : hoursText);
    if (!matcher.find()) return 0;
    double hours = Double.parseDouble(matcher.group(1).replace(',', '.'));
    return (int) Math.round(hours * 60);
}

var props = new Properties();
try (var in = java.nio.file.Files.newInputStream(java.nio.file.Path.of("src/main/resources/config.properties"))) {
    props.load(in);
}

String host = props.getProperty("DB_HOST", "localhost");
String port = props.getProperty("DB_PORT", "5432");
String db = props.getProperty("DB_NAME");
String user = props.getProperty("DB_USERNAME");
String pass = props.getProperty("DB_PASSWORD");
String url = "jdbc:postgresql://" + host + ":" + port + "/" + db;

int created = 0;
int updated = 0;
int unchanged = 0;
int skipped = 0;

String[] defaultTasks = {
    "Kitchen surfaces",
    "Bathroom reset",
    "Floors",
    "Final walkthrough"
};

try (var conn = DriverManager.getConnection(url, user, pass)) {
    conn.setAutoCommit(false);
    try (var statement = conn.createStatement()) {
        statement.executeUpdate("ALTER TABLE cleaning_appointments ALTER COLUMN cleaning_client_id DROP NOT NULL");
    }

    for (ScheduleEntry entry : entries) {
        Integer customerNumber = customerNumbers.get(normalize(entry.customerName()));
        int minutes = durationMinutes(entry.hoursText());
        if (customerNumber == null || minutes <= 0) {
            skipped += 1;
            continue;
        }

        String customerName;
        try (var customerQuery = conn.prepareStatement("""
                SELECT name FROM economic_customers
                WHERE economic_customer_number = ? AND economic_available = true
                """)) {
            customerQuery.setInt(1, customerNumber);
            try (var rs = customerQuery.executeQuery()) {
                if (!rs.next()) {
                    skipped += 1;
                    continue;
                }
                customerName = rs.getString(1);
            }
        }

        long projectId;
        try (var projectQuery = conn.prepareStatement("""
                SELECT id FROM cleaning_projects
                WHERE economic_customer_number = ?
                ORDER BY id
                LIMIT 1
                """)) {
            projectQuery.setInt(1, customerNumber);
            try (var rs = projectQuery.executeQuery()) {
                if (rs.next()) {
                    projectId = rs.getLong(1);
                } else {
                    try (var insertProject = conn.prepareStatement("""
                            INSERT INTO cleaning_projects (active, name, notes, cleaning_client_id, economic_customer_number)
                            VALUES (true, ?, null, null, ?)
                            RETURNING id
                            """)) {
                        insertProject.setString(1, customerName + " cleaning project");
                        insertProject.setInt(2, customerNumber);
                        try (var inserted = insertProject.executeQuery()) {
                            inserted.next();
                            projectId = inserted.getLong(1);
                        }
                    }
                }
            }
        }

        LocalDateTime appointmentTime = LocalDate.of(2026, entry.month(), entry.day()).atTime(9, 0);
        Long appointmentId = null;
        Integer existingMinutes = null;

        try (var appointmentQuery = conn.prepareStatement("""
                SELECT id, durationminutes FROM cleaning_appointments
                WHERE project_id = ?
                  AND appointmenttime::date = ?
                  AND vacation = false
                ORDER BY id
                LIMIT 1
                """)) {
            appointmentQuery.setLong(1, projectId);
            appointmentQuery.setDate(2, java.sql.Date.valueOf(appointmentTime.toLocalDate()));
            try (var rs = appointmentQuery.executeQuery()) {
                if (rs.next()) {
                    appointmentId = rs.getLong(1);
                    existingMinutes = rs.getInt(2);
                }
            }
        }

        if (appointmentId == null) {
            try (var insertAppointment = conn.prepareStatement("""
                    INSERT INTO cleaning_appointments
                        (appointmenttime, cancellationtime, durationminutes, vacation, cleaning_client_id, cleaning_staff_id, project_id)
                    VALUES (?, null, ?, false, null, null, ?)
                    RETURNING id
                    """)) {
                insertAppointment.setTimestamp(1, Timestamp.valueOf(appointmentTime));
                insertAppointment.setInt(2, minutes);
                insertAppointment.setLong(3, projectId);
                try (var inserted = insertAppointment.executeQuery()) {
                    inserted.next();
                    appointmentId = inserted.getLong(1);
                }
            }

            try (var insertTask = conn.prepareStatement("""
                    INSERT INTO cleaning_assignment_tasks (completed, sortorder, title, cleaning_appointment_id)
                    VALUES (false, ?, ?, ?)
                    """)) {
                for (int i = 0; i < defaultTasks.length; i += 1) {
                    insertTask.setInt(1, i + 1);
                    insertTask.setString(2, defaultTasks[i]);
                    insertTask.setLong(3, appointmentId);
                    insertTask.addBatch();
                }
                insertTask.executeBatch();
            }
            created += 1;
        } else if (existingMinutes != null && existingMinutes != minutes) {
            try (var updateAppointment = conn.prepareStatement("""
                    UPDATE cleaning_appointments
                    SET appointmenttime = ?, durationminutes = ?
                    WHERE id = ?
                    """)) {
                updateAppointment.setTimestamp(1, Timestamp.valueOf(appointmentTime));
                updateAppointment.setInt(2, minutes);
                updateAppointment.setLong(3, appointmentId);
                updateAppointment.executeUpdate();
            }
            updated += 1;
        } else {
            unchanged += 1;
        }
    }

    conn.commit();
}

System.out.printf("Cleaning calendar import complete: created=%d updated=%d unchanged=%d skipped=%d%n", created, updated, unchanged, skipped);
