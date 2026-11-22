package task06;

/**
 * Domain entity model for Student.
 */
public class Student {
    private int studentId;
    private String registerNo;
    private String name;
    private String department;

    public Student(int studentId, String registerNo, String name, String department) {
        this.studentId = studentId;
        this.registerNo = registerNo;
        this.name = name;
        this.department = department;
    }

    public int getStudentId() { return studentId; }
    public String getRegisterNo() { return registerNo; }
    public String getName() { return name; }
    public String getDepartment() { return department; }

    @Override
    public String toString() {
        return "Student[ID=" + studentId + ", RegNo=" + registerNo + ", Name=" + name + ", Dept=" + department + "]";
    }
}
