package task05;

import java.sql.*;

/**
 * SIMATS Java Programming Assignment 5 - Task 05
 * Database Metadata & Schema Forensics: Automated Portability and Environment Inspection
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class DatabaseInspector {

    private static final String DB_URL = "jdbc:h2:mem:inspection_db;MODE=MySQL;DB_CLOSE_DELAY=-1";
    private static final String USER = "sa";
    private static final String PASSWORD = "";

    public static void createSampleSchema(Connection con) throws SQLException {
        try (Statement st = con.createStatement()) {
            st.execute("CREATE TABLE bus_pass (" +
                    "pass_id VARCHAR(20) PRIMARY KEY, " +
                    "reg_no VARCHAR(20) NOT NULL, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "route_no INT NOT NULL, " +
                    "boarding_stop VARCHAR(100), " +
                    "validity_date DATE, " +
                    "amount DOUBLE NOT NULL)");

            st.execute("CREATE TABLE student (" +
                    "student_id INT PRIMARY KEY, " +
                    "register_no VARCHAR(20) UNIQUE NOT NULL, " +
                    "name VARCHAR(100) NOT NULL, " +
                    "department VARCHAR(50) NOT NULL)");

            st.execute("CREATE TABLE wallet (" +
                    "wallet_id INT PRIMARY KEY, " +
                    "student_id INT NOT NULL, " +
                    "balance DOUBLE NOT NULL, " +
                    "FOREIGN KEY (student_id) REFERENCES student(student_id))");
        }
    }

    public static void inspectEnvironment(Connection con) throws SQLException {
        DatabaseMetaData meta = con.getMetaData();

        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("DATABASE PLATFORM & DRIVER METRICS");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-30s : %s%n", "Database Product Name", meta.getDatabaseProductName());
        System.out.printf("%-30s : %s%n", "Database Product Version", meta.getDatabaseProductVersion());
        System.out.printf("%-30s : %s%n", "Database Major.Minor Version", meta.getDatabaseMajorVersion() + "." + meta.getDatabaseMinorVersion());
        System.out.printf("%-30s : %s%n", "JDBC Driver Name", meta.getDriverName());
        System.out.printf("%-30s : %s%n", "JDBC Driver Version", meta.getDriverVersion());
        System.out.printf("%-30s : %s%n", "Read Only Mode", meta.isReadOnly());
        System.out.printf("%-30s : %s%n", "Batch Updates Supported", meta.supportsBatchUpdates());
        System.out.printf("%-30s : %s%n", "Transactions Supported", meta.supportsTransactions());
        System.out.printf("%-30s : %s%n", "Savepoints Supported", meta.supportsSavepoints());
        System.out.printf("%-30s : %s%n", "Default Transaction Isolation", getIsolationLevelName(meta.getDefaultTransactionIsolation()));

        System.out.println("\n--------------------------------------------------------------------------------");
        System.out.println("CATALOG TABLE AUDIT & COLUMN SCHEMA SPECIFICATION");
        System.out.println("--------------------------------------------------------------------------------");

        try (ResultSet tables = meta.getTables(null, "PUBLIC", "%", new String[]{"TABLE"})) {
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println("\n[TABLE]: " + tableName);
                System.out.printf("   %-20s | %-16s | %-10s | %-10s%n",
                        "Column Name", "Data Type", "Size", "Nullable");
                System.out.println("   ---------------------------------------------------------------------");

                try (ResultSet cols = meta.getColumns(null, "PUBLIC", tableName, "%")) {
                    while (cols.next()) {
                        System.out.printf("   %-20s | %-16s | %-10d | %-10s%n",
                                cols.getString("COLUMN_NAME"),
                                cols.getString("TYPE_NAME"),
                                cols.getInt("COLUMN_SIZE"),
                                (cols.getInt("NULLABLE") == DatabaseMetaData.columnNullable ? "YES" : "NO"));
                    }
                }
            }
        }
    }

    private static String getIsolationLevelName(int level) {
        return switch (level) {
            case Connection.TRANSACTION_NONE -> "TRANSACTION_NONE";
            case Connection.TRANSACTION_READ_UNCOMMITTED -> "READ_UNCOMMITTED";
            case Connection.TRANSACTION_READ_COMMITTED -> "READ_COMMITTED";
            case Connection.TRANSACTION_REPEATABLE_READ -> "REPEATABLE_READ";
            case Connection.TRANSACTION_SERIALIZABLE -> "SERIALIZABLE";
            default -> "UNKNOWN (" + level + ")";
        };
    }

    public static void main(String[] args) throws SQLException {
        System.out.println("================================================================================");
        System.out.println("SIMATS JAVA PROGRAMMING ASSIGNMENT 5 - TASK 05: METADATA & SCHEMA INSPECTOR");
        System.out.println("Student: Bala Naga Jaswanth | Reg No: 192424300 | Dept: AI & DS");
        System.out.println("================================================================================\n");

        try (Connection con = DriverManager.getConnection(DB_URL, USER, PASSWORD)) {
            createSampleSchema(con);
            inspectEnvironment(con);

            System.out.println("\n================================================================================");
            System.out.println("[CONCLUSION] DatabaseMetaData provides automated portability diagnostics.");
            System.out.println("[CONCLUSION] Enables schema verification and cross-database validation in enterprise ETL.");
            System.out.println("================================================================================");
        }
    }
}
