package task06;

import task03.PassLoader;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * SIMATS Java Programming Assignment 5 - Task 06
 * Enterprise Migration Pipeline: ETL Validation, Batch Loading & Audit Rejection Reporting
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class PassMigration {

    private static final String DB_URL = "jdbc:h2:mem:migration_db;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void setupDatabase(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("DROP TABLE IF EXISTS bus_pass");
            st.execute("CREATE TABLE bus_pass (" +
                    "pass_id VARCHAR(20) PRIMARY KEY, " +
                    "reg_no VARCHAR(20) NOT NULL, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "route_no INT NOT NULL, " +
                    "amount DOUBLE NOT NULL)");
        }
    }

    /**
     * Production Migration Workflow:
     * Reads CSV file, strictly validates each field, batches clean rows to JDBC,
     * and streams corrupted/malicious records into an audit rejection report.
     */
    public static MigrationStats migrateData(Connection con, Path inputCsv, Path rejectReport) throws IOException, SQLException {
        long totalLines = 0;
        long validCount = 0;
        long rejectedCount = 0;

        List<String[]> batch = new ArrayList<>();

        try (BufferedReader br = Files.newBufferedReader(inputCsv);
             BufferedWriter rejects = Files.newBufferedWriter(rejectReport,
                     StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

            rejects.write("LineNumber\tErrorCode\tErrorMessage\tRawContent");
            rejects.newLine();

            String line;
            while ((line = br.readLine()) != null) {
                totalLines++;
                if (line.trim().isEmpty()) {
                    rejects.write(totalLines + "\tERR_EMPTY_LINE\tEncountered blank line\t(empty)");
                    rejects.newLine();
                    rejectedCount++;
                    continue;
                }

                try {
                    String[] fields = parseAndValidate(line);
                    // Extract 5 database fields: passId, regNo, name, route, amount
                    batch.add(new String[]{fields[0], fields[1], fields[2], fields[3], fields[6]});
                    validCount++;

                    if (batch.size() == 1000) {
                        PassLoader.loadAll(con, batch);
                        batch.clear();
                    }
                } catch (ValidationException ve) {
                    rejects.write(totalLines + "\t" + ve.getErrorCode() + "\t" + ve.getMessage() + "\t" + line);
                    rejects.newLine();
                    rejectedCount++;
                }
            }

            // Flush remainder
            if (!batch.isEmpty()) {
                PassLoader.loadAll(con, batch);
                batch.clear();
            }
        }

        return new MigrationStats(totalLines, validCount, rejectedCount);
    }

    private static String[] parseAndValidate(String line) throws ValidationException {
        String[] fields = line.split(",", -1);
        if (fields.length != 7) {
            throw new ValidationException("ERR_COL_COUNT", "Invalid column count: expected 7, got " + fields.length);
        }

        String passId = fields[0].trim();
        String regNo = fields[1].trim();
        String name = fields[2].trim();
        String routeStr = fields[3].trim();
        String boardingStop = fields[4].trim();
        String validityDateStr = fields[5].trim();
        String amountStr = fields[6].trim();

        if (passId.isEmpty()) throw new ValidationException("ERR_EMPTY_PASSID", "Pass ID cannot be empty");
        if (regNo.isEmpty()) throw new ValidationException("ERR_EMPTY_REGNO", "Registration Number cannot be empty");
        if (name.isEmpty()) throw new ValidationException("ERR_EMPTY_NAME", "Student Name cannot be empty");

        int route;
        try {
            route = Integer.parseInt(routeStr);
            if (route <= 0 || route > 100) {
                throw new ValidationException("ERR_ROUTE_RANGE", "Route number out of operational range (1-100): " + route);
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("ERR_INVALID_ROUTE", "Route is not a valid integer: " + routeStr);
        }

        try {
            LocalDate.parse(validityDateStr);
        } catch (DateTimeParseException e) {
            throw new ValidationException("ERR_INVALID_DATE", "Date format must be ISO YYYY-MM-DD: " + validityDateStr);
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount < 0) {
                throw new ValidationException("ERR_NEGATIVE_AMOUNT", "Amount cannot be negative: " + amount);
            }
        } catch (NumberFormatException e) {
            throw new ValidationException("ERR_INVALID_AMOUNT", "Amount is not a valid decimal: " + amountStr);
        }

        return fields;
    }

    public static class ValidationException extends Exception {
        private final String errorCode;
        public ValidationException(String errorCode, String message) {
            super(message);
            this.errorCode = errorCode;
        }
        public String getErrorCode() { return errorCode; }
    }

    public record MigrationStats(long total, long valid, long rejected) {}

    public static void main(String[] args) throws Exception {
        System.out.println("================================================================================");
        System.out.println("SIMATS JAVA PROGRAMMING ASSIGNMENT 5 - TASK 06: DATA MIGRATION & REJECTION AUDIT");
        System.out.println("Student: Bala Naga Jaswanth | Reg No: 192424300 | Dept: AI & DS");
        System.out.println("================================================================================\n");

        Path tempInput = Files.createTempFile("legacy_bus_passes_", ".csv");
        Path tempRejects = Files.createTempFile("migration_rejections_", ".tsv");

        try (BufferedWriter bw = Files.newBufferedWriter(tempInput)) {
            // Write batch of clean and corrupted rows
            bw.write("BP10001,192424300,Bala Naga Jaswanth,15,Tambaram,2026-12-31,4500.00\n");
            bw.write("BP10002,192424301,Arun Kumar,12,Poonamallee,2026-12-31,4000.00\n");
            bw.write("BP10003,192424302,Divya Lakshmi,7,Guindy,2026-12-31,3800.00\n");
            bw.write("BP10004,192424303,Damaged Date,15,Tambaram,31-12-2026,4500.00\n"); // Bad date format
            bw.write("BP10005,192424304,Missing Fields,12,Poonamallee\n");                     // Bad col count
            bw.write("\n");                                                                    // Blank line
            bw.write("BP10006,192424305,Negative Fee,15,Tambaram,2026-12-31,-500.00\n");       // Bad fee
            bw.write("BP10007,192424306,Invalid Route,999,Central,2026-12-31,4200.00\n");      // Route out of range
            bw.write("BP10008,192424307,Kavitha Raman,10,Avadi,2026-12-31,4100.00\n");
            bw.write("BP10009,192424308,Suresh Babu,18,Porur,2026-12-31,3900.00\n");
        }

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            setupDatabase(con);

            System.out.println("[MIGRATION EXECUTION] Starting Enterprise Ingestion Pipeline...");
            MigrationStats stats = migrateData(con, tempInput, tempRejects);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("MIGRATION AUDIT SUMMARY STATISTICS");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-30s : %d%n", "Total Raw Input Lines", stats.total());
            System.out.printf("%-30s : %d (%.1f%%)%n", "Successfully Ingested Records", stats.valid(), (stats.valid() * 100.0 / stats.total()));
            System.out.printf("%-30s : %d (%.1f%%)%n", "Rejected Damaged Records", stats.rejected(), (stats.rejected() * 100.0 / stats.total()));

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("AUDIT TRAIL: REJECTION REPORT (TSV)");
            System.out.println("--------------------------------------------------------------------------------");
            List<String> rejectLines = Files.readAllLines(tempRejects);
            for (String r : rejectLines) {
                System.out.println("  ! " + r);
            }

            // Verify database contents
            try (Statement st = con.createStatement();
                 ResultSet rs = st.executeQuery("SELECT pass_id, reg_no, name, route_no, amount FROM bus_pass ORDER BY pass_id")) {
                System.out.println("\n--------------------------------------------------------------------------------");
                System.out.println("PERSISTED RELATIONAL RECORDS IN DATABASE (bus_pass)");
                System.out.println("--------------------------------------------------------------------------------");
                System.out.printf("%-10s | %-12s | %-22s | %-8s | %-10s%n",
                        "Pass ID", "Register No", "Student Name", "Route", "Amount");
                System.out.println("--------------------------------------------------------------------------------");
                while (rs.next()) {
                    System.out.printf("%-10s | %-12s | %-22s | %-8d | INR %-8.2f%n",
                            rs.getString("pass_id"),
                            rs.getString("reg_no"),
                            rs.getString("name"),
                            rs.getInt("route_no"),
                            rs.getDouble("amount"));
                }
            }

            System.out.println("\n================================================================================");
            System.out.println("[CONCLUSION] Enterprise migration successfully isolates corrupted rows.");
            System.out.println("[CONCLUSION] Rejection audit enables zero-data-loss remediation while preserving DB integrity.");
            System.out.println("================================================================================");
        } finally {
            Files.deleteIfExists(tempInput);
            Files.deleteIfExists(tempRejects);
        }
    }
}
