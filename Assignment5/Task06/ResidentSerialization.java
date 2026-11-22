package task06;

import java.io.*;

/**
 * Task A4 Demonstration: Java Object Serialization
 * Illustrates serialVersionUID evolution and transient protection for passwords and Connection handles.
 */
public class ResidentSerialization implements Serializable {
    private static final long serialVersionUID = 1L;

    private String name;
    private String registerNumber;
    private transient String password; // Not persisted to prevent secret leakage
    private String phoneNumber;
    private transient Object connectionHandle; // Runtime resource handle not serializable

    public ResidentSerialization(String name, String registerNumber, String password, String phoneNumber) {
        this.name = name;
        this.registerNumber = registerNumber;
        this.password = password;
        this.phoneNumber = phoneNumber;
        this.connectionHandle = "MOCK_DB_SOCKET_HANDLE";
    }

    public String getName() { return name; }
    public String getRegisterNumber() { return registerNumber; }
    public String getPassword() { return password; }
    public String getPhoneNumber() { return phoneNumber; }
    public Object getConnectionHandle() { return connectionHandle; }

    public static byte[] serialize(ResidentSerialization resident) throws IOException {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream oos = new ObjectOutputStream(baos)) {
            oos.writeObject(resident);
            return baos.toByteArray();
        }
    }

    public static ResidentSerialization deserialize(byte[] data) throws IOException, ClassNotFoundException {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(data);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            return (ResidentSerialization) ois.readObject();
        }
    }
}
