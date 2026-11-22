package task06;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Concrete JDBC DAO implementation for Student entities.
 */
public class StudentDao implements Dao<Student, Integer> {
    private final Connection con;

    public StudentDao(Connection con) {
        this.con = con;
    }

    @Override
    public void insert(Student s) throws SQLException {
        String sql = "INSERT INTO student(student_id, register_no, name, department) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, s.getStudentId());
            ps.setString(2, s.getRegisterNo());
            ps.setString(3, s.getName());
            ps.setString(4, s.getDepartment());
            ps.executeUpdate();
        }
    }

    @Override
    public Student findById(Integer id) throws SQLException {
        String sql = "SELECT student_id, register_no, name, department FROM student WHERE student_id = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                return new Student(
                        rs.getInt("student_id"),
                        rs.getString("register_no"),
                        rs.getString("name"),
                        rs.getString("department")
                );
            }
        }
    }

    @Override
    public List<Student> findAll() throws SQLException {
        List<Student> list = new ArrayList<>();
        String sql = "SELECT student_id, register_no, name, department FROM student ORDER BY student_id";
        try (Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Student(
                        rs.getInt("student_id"),
                        rs.getString("register_no"),
                        rs.getString("name"),
                        rs.getString("department")
                ));
            }
        }
        return list;
    }

    @Override
    public void update(Student s) throws SQLException {
        String sql = "UPDATE student SET register_no=?, name=?, department=? WHERE student_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, s.getRegisterNo());
            ps.setString(2, s.getName());
            ps.setString(3, s.getDepartment());
            ps.setInt(4, s.getStudentId());
            ps.executeUpdate();
        }
    }

    @Override
    public void delete(Integer id) throws SQLException {
        String sql = "DELETE FROM student WHERE student_id=?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }
}
