package task04;

import task03.PassLoader;
import java.sql.*;
import java.util.*;

/**
 * SIMATS Java Programming Assignment 5 - Task 04
 * ACID Transaction Integrity: All-or-Nothing Ingestion and Forensic Rollback Verification
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class TransactionRollbackDemo {

    private static final String DB_URL = "jdbc:h2:mem:tx_test_db;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void setupSchema(Connection con) throws SQLException {
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

    public static int getRowCount(Connection con) throws SQLException {
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM bus_pass")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("================================================================================");
        System.out.println("SIMATS JAVA PROGRAMMING ASSIGNMENT 5 - TASK 04: TRANSACTION ROLLBACK FORENSICS");
        System.out.println("Student: Bala Naga Jaswanth | Reg No: 192424300 | Dept: AI & DS");
        System.out.println("================================================================================\n");

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {

            // SCENARIO 1: ATOMIC ROLLBACK UNDER TRANSACTION MANAGEMENT
            System.out.println("--------------------------------------------------------------------------------");
            System.out.println("SCENARIO 1: BATCH INGESTION WITH MIDWAY CORRUPTION (TRANSACTION ENABLED)");
            System.out.println("--------------------------------------------------------------------------------");
            setupSchema(con);

            int totalRows = 5000;
            int corruptIndex = 2500;
            System.out.printf("Generating %d records with intentional data corruption at row #%d...%n", totalRows, corruptIndex);

            List<String[]> passesWithCorruption = new ArrayList<>(totalRows);
            for (int i = 1; i <= totalRows; i++) {
                if (i == corruptIndex) {
                    // Poison pill: Non-numeric amount to trigger NumberFormatException midway
                    passesWithCorruption.add(new String[]{
                            "BP" + String.format("%06d", i),
                            "192424" + String.format("%03d", (i % 1000)),
                            "PoisonPill_Student",
                            "12",
                            "CRITICAL_CORRUPT_NOT_A_NUMBER"
                    });
                } else {
                    passesWithCorruption.add(new String[]{
                            "BP" + String.format("%06d", i),
                            "192424" + String.format("%03d", (i % 1000)),
                            "Student_" + i,
                            "15",
                            "4500.00"
                    });
                }
            }

            System.out.println("Initial Database Row Count: " + getRowCount(con));
            System.out.println("Invoking PassLoader.loadAll() within single managed transaction...");

            boolean caughtExpectedException = false;
            try {
                PassLoader.loadAll(con, passesWithCorruption);
            } catch (Exception ex) {
                caughtExpectedException = true;
                System.out.println(">> Intercepted Expected Exception: " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                System.out.println(">> Catch block activated: Invoked con.rollback() successfully.");
            }

            int finalCount = getRowCount(con);
            System.out.println("\n[POST-FAILURE AUDIT] SELECT COUNT(*) FROM bus_pass:");
            System.out.println("   Rows in Database: " + finalCount);

            if (caughtExpectedException && finalCount == 0) {
                System.out.println("   [STATUS: PASSED] Strict ACID Atomicity Proven!");
                System.out.printf("   Zero partial records persisted out of the %d successfully staged rows.%n", (corruptIndex - 1));
            } else {
                System.out.println("   [STATUS: FAILED] Inconsistent database state detected.");
            }

            // SCENARIO 2: CONTROL COMPARISON - WITHOUT TRANSACTION (AUTO-COMMIT ON)
            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("SCENARIO 2: VULNERABILITY COMPARISON (UNPROTECTED AUTO-COMMIT MODE)");
            System.out.println("--------------------------------------------------------------------------------");
            setupSchema(con);
            con.setAutoCommit(true);

            String insertSql = "INSERT INTO bus_pass VALUES (?, ?, ?, ?, ?)";
            int stagedRows = 0;
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                for (String[] r : passesWithCorruption) {
                    ps.setString(1, r[0]);
                    ps.setString(2, r[1]);
                    ps.setString(3, r[2]);
                    ps.setInt(4, Integer.parseInt(r[3]));
                    ps.setDouble(5, Double.parseDouble(r[4])); // Throws exception at row 2500
                    ps.executeUpdate();
                    stagedRows++;
                }
            } catch (NumberFormatException nfe) {
                System.out.println(">> Intercepted unhandled parsing exception at row #" + (stagedRows + 1));
            }

            int orphanedRows = getRowCount(con);
            System.out.println("\n[POST-FAILURE AUDIT] Auto-Commit Database Row Count:");
            System.out.printf("   Orphaned Stale Rows Persisted: %d (Critical Integrity Failure!)%n", orphanedRows);
            System.out.println("   Without transaction rollback, database is left in a corrupted, half-imported state.");

            System.out.println("\n================================================================================");
            System.out.println("[CONCLUSION] setAutoCommit(false) + commit() + rollback() is mandatory for ETL.");
            System.out.println("================================================================================");
        }
    }
}
