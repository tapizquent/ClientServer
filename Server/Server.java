import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int BUFFER_SIZE = 4096;
    private Socket connection;
    private ServerSocket socket;
    private DataInputStream socketIn;
    private DataOutputStream socketOut;
    private FileInputStream fileIn;
    private String filename;
    private int bytes;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int debugFlag = 0;
    private long percentageTransferred = 0;

    public Server(int port, int debugFlag) {
        this.debugFlag = debugFlag;
        try {
            socket = new ServerSocket(port);
            // Wait for connection and process it
            while (true) {
                try {
                    connection = socket.accept(); // Block for connection request

                    socketIn = new DataInputStream(connection.getInputStream()); // Read data from client
                    socketOut = new DataOutputStream(connection.getOutputStream()); // Write data to client

                    filename = socketIn.readUTF(); // Read filename from client

                    fileIn = new FileInputStream("Files/" + filename);

                    if (debugFlag == 1) {
                        System.out.println("Sending " + filename + " to " + connection.getInetAddress());
                    }

                    int totalFileByteSize = fileIn.available();
                    int bufferSize = totalFileByteSize > BUFFER_SIZE ? BUFFER_SIZE : totalFileByteSize;
                    buffer = new byte[bufferSize];

                    System.out.println("Size of file being sent: " + totalFileByteSize);

                    // Write file contents to client
                    while (true) {
                        bytes = fileIn.read(buffer, 0, bufferSize); // Read from file
                        if (bytes <= 0)
                            break; // Check for end of file
                        socketOut.write(buffer, 0, bytes); // Write bytes to socket
                        printTransferProgress(totalFileByteSize, fileIn.available());
                        // Log.i("TOTAL", Long.toString(total));
                        // Log.i("Upload progress", "" + (int) ((total * 100) / buffer.length));
                    }
                } catch (Exception ex) {
                    System.out.println("Error: " + ex);
                    System.out.println("filename: " + filename);
                } finally {
                    // Clean up socket and file streams
                    if (connection != null) {
                        connection.close();
                    }

                    if (fileIn != null) {
                        fileIn.close();
                    }

                    percentageTransferred = 0;

                    if (debugFlag == 1) {
                        System.out.println("Finished sending " + filename + " to " + connection.getInetAddress());
                    }

                    System.out.println();
                }
            }
        } catch (IOException i) {
            System.out.println("Error: " + i);
        }
    }

    // if bytesLeft == 0
    // 38 X

    private void printTransferProgress(long totalBytes, long bytesLeft) {
        if (debugFlag == 0)
            return;

        double percentage = 100 - ((bytesLeft * 100) / totalBytes);
        long roundedPercentage = Math.round(percentage / 10.0) * 10;

        for (long i = percentageTransferred + 10; i <= roundedPercentage; i += 10) {
            System.out.println("Sent " + i + "% of " + filename);
        }

        percentageTransferred = roundedPercentage;

    }

    public static void main(String[] args) {
        int debugFlag = args.length > 0 ? Integer.parseInt(args[0]) : 0;
        Server server = new Server(5000, debugFlag);
    }
}
