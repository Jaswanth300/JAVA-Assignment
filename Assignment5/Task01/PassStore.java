package task01;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

/**
 * SIMATS Java Programming Assignment 5 - Task 01
 * Data Persistence Engineering: File Store and Resilient CSV Parsing
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class PassStore {
    private final Path file;

    public PassStore(Path file) {
        this.file = file;
    }

    /**
     * Appends a new bus pass record to the persistent delimited text store.
     */
    public void add(String passId, String regNo, String name, int route, double amount) throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(String.join(",", passId, regNo, name,
                    String.valueOf(route), String.valueOf(amount)));
            w.newLine();
        }
    }

    /**
     * Sequential lookup of a bus pass record by unique pass identifier.
     * Implements forensic defensive parsing: malformed rows, wrong field count,
     * and corrupted numeric fields are gracefully skipped without terminating search.
     *
     * @param passId unique identifier to search
     * @return array of 5 fields or null if not found
     */
    public String[] find(String passId) throws IOException {
        if (!Files.exists(file)) return null;

        try (BufferedReader reader = Files.newBufferedReader(file)) {
            String line;
            long lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }
                String[] fields = line.split(",", -1);
                if (fields.length != 5) {
                    // Damaged row: wrong field count
                    continue;
                }
                try {
                    Integer.parseInt(fields[3].trim());
                    Double.parseDouble(fields[4].trim());
                } catch (NumberFormatException ex) {
                    // Damaged row: corrupted numeric field
                    continue;
                }
                if (fields[0].trim().equals(passId)) {
                    return fields;
                }
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        System.out.println("================================================================================");
        System.out.println("SIMATS JAVA PROGRAMMING ASSIGNMENT 5 - TASK 01: FILE STORE & PARSING RESILIENCE");
        System.out.println("Student: Bala Naga Jaswanth | Reg No: 192424300 | Dept: AI & DS");
        System.out.println("================================================================================");

        Path tempFile = Files.createTempFile("bus_passes_test", ".csv");
        PassStore store = new PassStore(tempFile);

        System.out.println("\n[PHASE 1] Populating Pass Store with Standard and Adversarial Records...");
        store.add("P1001", "192311001", "Arun Kumar", 12, 4500.0);
        Files.writeString(tempFile, "P1002,DAMAGED LINE MISSING FIELDS\n", StandardOpenOption.APPEND);
        Files.writeString(tempFile, "\n", StandardOpenOption.APPEND); // empty line
        Files.writeString(tempFile, "P100X,192311099,Corrupt Numeric,INVALID_ROUTE,not-a-number\n", StandardOpenOption.APPEND);
        store.add("P1003", "192311003", "Divya Lakshmi", 7, 3800.0);
        store.add("P1004", "192424300", "Bala Naga Jaswanth", 15, 5200.0);

        System.out.println("File Size: " + Files.size(tempFile) + " bytes");

        System.out.println("\n[PHASE 2] Executing Defensive Search Queries:");
        System.out.println("Search 'P1003' (Valid record after damaged rows):");
        String[] resP1003 = store.find("P1003");
        System.out.println(" -> Result: " + Arrays.toString(resP1003));

        System.out.println("\nSearch 'P1002' (Damaged row with 2 fields):");
        String[] resP1002 = store.find("P1002");
        System.out.println(" -> Result: " + (resP1002 == null ? "null (Cleanly skipped, search unharmed)" : Arrays.toString(resP1002)));

        System.out.println("\nSearch 'P1004' (Student Author record):");
        String[] resP1004 = store.find("P1004");
        System.out.println(" -> Result: " + Arrays.toString(resP1004));

        System.out.println("\nSearch 'P9999' (Non-existent pass ID):");
        String[] resP9999 = store.find("P9999");
        System.out.println(" -> Result: " + (resP9999 == null ? "null (Expected: Not found)" : Arrays.toString(resP9999)));

        Files.deleteIfExists(tempFile);
        System.out.println("\n[STATUS] Task 01 PassStore Verification Completed Successfully.");
    }
}
