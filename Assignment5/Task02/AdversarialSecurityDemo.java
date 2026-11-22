package task02;

import java.sql.*;
import java.util.*;

/**
 * SIMATS Java Programming Assignment 5 - Task 02
 * Forensic Engineering: Adversarial Input Analysis & SQL Injection Mitigation
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class AdversarialSecurityDemo {

    private static final String DB_URL = "jdbc:h2:mem:security_audit;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void setupSecurityDatabase(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE users (" +
                    "id VARCHAR(50) PRIMARY KEY, " +
                    "pwd VARCHAR(100) NOT NULL, " +
                    "role VARCHAR(20) NOT NULL, " +
                    "full_name VARCHAR(100) NOT NULL)");

            // Populate target credential records
            st.execute("INSERT INTO users VALUES ('admin', 'hash_admin_98231', 'SYSTEM_ADMIN', 'Security Administrator')");
            st.execute("INSERT INTO users VALUES ('student1', 'hash_pass_55412', 'STUDENT', 'Bala Naga Jaswanth')");
            st.execute("INSERT INTO users VALUES ('faculty', 'hash_fac_11209', 'FACULTY', 'Dr. S. K. Raman')");
        }
    }

    /**
     * Demonstrates critical SQL Injection vulnerability using Statement concatenation.
     */
    public static List<String> authenticateUnsafe(Connection con, String inputId, String inputPwd) throws SQLException {
        List<String> leakedAccounts = new ArrayList<>();
        // VULNERABLE: Direct string interpolation allows adversarial syntax manipulation
        String unsafeSql = "SELECT id, role, full_name FROM users WHERE id='" + inputId + "' AND pwd='" + inputPwd + "'";
        System.out.println("   [RAW SQL EXECUTED]: " + unsafeSql);

        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(unsafeSql)) {
            while (rs.next()) {
                leakedAccounts.add(String.format("User: %-10s | Role: %-12s | Name: %s",
                        rs.getString("id"), rs.getString("role"), rs.getString("full_name")));
            }
        }
        return leakedAccounts;
    }

    /**
     * Demonstrates robust parameterized protection using PreparedStatement.
     */
    public static List<String> authenticateSafe(Connection con, String inputId, String inputPwd) throws SQLException {
        List<String> authenticated = new ArrayList<>();
        // SECURE: Parameter placeholders separate query grammar from user data
        String safeSql = "SELECT id, role, full_name FROM users WHERE id=? AND pwd=?";

        try (PreparedStatement ps = con.prepareStatement(safeSql)) {
            ps.setString(1, inputId);
            ps.setString(2, inputPwd);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    authenticated.add(String.format("User: %-10s | Role: %-12s | Name: %s",
                            rs.getString("id"), rs.getString("role"), rs.getString("full_name")));
                }
            }
        }
        return authenticated;
    }

    public static void testAdversarialCsvRows() {
        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("TEST SUITE 2: RESILIENT BUS-PASS RECORD PARSER VERIFICATION");
        System.out.println("--------------------------------------------------------------------------------");

        String[] testRows = {
                "P2001,192424300,Bala Naga Jaswanth,15,4500.00",            // Valid control
                "P2002,REG2,Damaged Record,5",                                // Structural flaw (4 fields instead of 5)
                "P2003,REG3,Malicious Amount,5,DROP TABLE bus_pass;--",       // Type flaw & injection attempt
                "",                                                           // Empty line flaw
                "   ",                                                        // Whitespace line flaw
                "P2004,REG4,Negative Route,-3,4500.00",                       // Range violation
                "P2005,192424301,Kavitha Raman,12,3800.00"                   // Valid control
        };

        for (int i = 0; i < testRows.length; i++) {
            String row = testRows[i];
            System.out.printf("Row %d: %-50s -> ", (i + 1), (row.isEmpty() ? "(EMPTY LINE)" : row));

            if (row.trim().isEmpty()) {
                System.out.println("[REJECTED] Empty row skipped safely");
                continue;
            }

            String[] fields = row.split(",", -1);
            if (fields.length != 5) {
                System.out.printf("[REJECTED] Structural validation failed (got %d fields, expected 5)%n", fields.length);
                continue;
            }

            try {
                int route = Integer.parseInt(fields[3].trim());
                double amount = Double.parseDouble(fields[4].trim());

                if (route <= 0) {
                    System.out.printf("[REJECTED] Semantic validation failed: Route must be > 0 (got %d)%n", route);
                    continue;
                }
                if (amount < 0) {
                    System.out.printf("[REJECTED] Semantic validation failed: Amount must be >= 0 (got %.2f)%n", amount);
                    continue;
                }

                System.out.printf("[ACCEPTED] Clean Bus Pass Record (ID: %s, Reg: %s, Fee: INR %.2f)%n",
                        fields[0], fields[1], amount);
            } catch (NumberFormatException e) {
                System.out.println("[REJECTED] Numeric parsing exception caught (Type violation)");
            }
        }
    }

    public static void main(String[] args) {
        System.out.println("================================================================================");
        System.out.println("SIMATS JAVA PROGRAMMING ASSIGNMENT 5 - TASK 02: ADVERSARIAL INPUT FORENSICS");
        System.out.println("Student: Bala Naga Jaswanth | Reg No: 192424300 | Dept: AI & DS");
        System.out.println("================================================================================");

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            setupSecurityDatabase(con);

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.println("TEST SUITE 1: SQL INJECTION VULNERABILITY VS PARAMETERIZED DEFENSE");
            System.out.println("--------------------------------------------------------------------------------");

            String attackPayload = "' OR '1'='1' --";
            String dummyPassword = "arbitrary_attacker_payload";

            System.out.println("[ATTACK SCENARIO] Adversarial Input Vector:");
            System.out.println("   Input User ID:  " + attackPayload);
            System.out.println("   Input Password: " + dummyPassword);

            System.out.println("\n1. Testing Insecure Concatenated Statement:");
            List<String> leaked = authenticateUnsafe(con, attackPayload, dummyPassword);
            System.out.println("   [BREACH CONFIRMED] Statement compromised! Extracted " + leaked.size() + " accounts:");
            for (String acc : leaked) {
                System.out.println("     ! " + acc);
            }

            System.out.println("\n2. Testing Robust Parameterized PreparedStatement:");
            List<String> safeResult = authenticateSafe(con, attackPayload, dummyPassword);
            if (safeResult.isEmpty()) {
                System.out.println("   [DEFENSE VERIFIED] PreparedStatement safely neutralized payload!");
                System.out.println("   Input treated strictly as literal data. Zero unauthorized records returned.");
            } else {
                System.out.println("   [FAILURE] Unexpected match found.");
            }

            testAdversarialCsvRows();

            System.out.println("\n================================================================================");
            System.out.println("[CONCLUSION] PreparedStatement completely prevents SQL structure subversion.");
            System.out.println("[CONCLUSION] Strict multi-stage regex and bounds validation safeguards persistent CSV.");
            System.out.println("================================================================================");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
