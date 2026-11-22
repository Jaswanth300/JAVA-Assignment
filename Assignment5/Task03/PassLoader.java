package task03;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * SIMATS Java Programming Assignment 5 - Task 03
 * High-Throughput Batch Persistence with Transaction Safety (TODO 1 to TODO 3 Completed)
 * Student: Bala Naga Jaswanth (Reg: 192424300)
 */
public class PassLoader {

    /**
     * Inserts a list of bus pass records using PreparedStatement batching
     * wrapped within a strict single transaction.
     *
     * @param con active database connection
     * @param passes collection of pass field arrays
     * @return count of inserted records
     * @throws SQLException on database error or transaction failure
     */
    public static int loadAll(Connection con, List<String[]> passes) throws SQLException {
        String sql = "INSERT INTO bus_pass(pass_id, reg_no, name, route_no, amount) VALUES (?, ?, ?, ?, ?)";
        boolean oldAutoCommit = con.getAutoCommit();
        con.setAutoCommit(false); // TODO 1: Disable auto-commit for transactional atomicity

        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int n = 0;
            for (String[] p : passes) {
                ps.setString(1, p[0]);
                ps.setString(2, p[1]);
                ps.setString(3, p[2]);
                ps.setInt(4, Integer.parseInt(p[3]));
                ps.setDouble(5, Double.parseDouble(p[4]));
                ps.addBatch(); // TODO 2: Buffer parameter values into JDBC batch queue
                n++;
                if (n % 1000 == 0) {
                    ps.executeBatch(); // Flush chunk to database
                }
            }
            ps.executeBatch(); // Flush remaining records
            con.commit();      // TODO 3: Atomically commit the entire import batch
            return n;
        } catch (SQLException | RuntimeException e) {
            con.rollback();    // Atomicity guarantee: rollback on any conversion or SQL error
            throw e;
        } finally {
            con.setAutoCommit(oldAutoCommit); // Restore original connection state
        }
    }
}
