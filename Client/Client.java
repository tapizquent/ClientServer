
import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Client {
    private final int BUFFER_SIZE = 4096;
    private Socket connection;
    private DataInputStream socketIn;
    private DataOutputStream socketOut;
    private int bytes;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private File fileToWrite;

    public Client(String host, int port, String filename) {
        try {

            connection = new Socket(host, port);

            socketIn = new DataInputStream(connection.getInputStream()); // Read data from server
            socketOut = new DataOutputStream(connection.getOutputStream()); // Write data to server

            socketOut.writeUTF(filename); // Write filename to server

            fileToWrite = new File("Files/" + filename);
            OutputStream os = new FileOutputStream(fileToWrite);

            // Read file contents from server
            while (true) {
                bytes = socketIn.read(buffer, 0, BUFFER_SIZE); // Read from socket
                if (bytes <= 0)
                    break; // Check for end of file

                // Write
                os.write(buffer, 0, bytes);
            }

            os.close();
            connection.close();
        } catch (Exception ex) {
            System.out.println("Error: " + ex);
        }
    }

    public static void main(String[] args) {
        Client client = new Client("127.0.0.1", 5000, args[0]);
    }
}
