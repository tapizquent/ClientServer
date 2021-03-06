import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;

public class Server {
    private final int BUFFER_SIZE = 4096;
    private Socket connection;
    private ServerSocket socket;
    private DataInputStream socketIn;
    private DataOutputStream socketOut;
    private FileInputStream fileIn;
    private FileOutputStream fileOut;
    private String filename;
    private int bytes;
    private byte[] buffer = new byte[BUFFER_SIZE];
    private int debugFlag = 0;
    private long percentageTransferred = 0;
    // Client mode 0 = READ
    // Client mode 1 = WRITE
    private int clientMode = 0;

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

                    clientMode = socketIn.readInt(); // Read clientMode from Client
                    filename = socketIn.readUTF(); // Read filename from client

                    if (debugFlag == 1) {
                        if (clientMode == 0) {
                            System.out.println("Sending " + filename + " to " + connection.getInetAddress());
                        } else {
                            System.out.println("Receiving " + filename + " from " + connection.getInetAddress());
                        }
                    }

                    if (clientMode == 0) {
                        processClientDownload();
                    } else {
                        processClientUpload();
                    }

                } catch (Exception ex) {
                    System.out.println("Error: " + ex);
                    System.out.println("filename: " + filename);
                } finally {
                    // Clean up socket and file streams
                    if (connection != null) {
                        connection.close();
                    }

                    percentageTransferred = 0;

                    System.out.println();
                }
            }
        } catch (IOException i) {
            System.out.println("Error: " + i);
        }
    }

    private long totalBytesTransferred = 0;

    private void processClientDownload() throws IOException, FileNotFoundException {
        long skipItems = socketIn.readLong();
        long endByteIndex = socketIn.readLong();
        long lengthOfBytesToRead = endByteIndex - skipItems;
        fileIn = new FileInputStream("Files/" + filename);
        fileIn.skip(skipItems);
        long totalFileByteSize = fileIn.available();

        if (endByteIndex > 0) {
            if (totalFileByteSize > lengthOfBytesToRead) {
                totalFileByteSize = lengthOfBytesToRead;
            }
        }

        int bufferSize = totalFileByteSize > BUFFER_SIZE ? BUFFER_SIZE : (int) totalFileByteSize;

        buffer = new byte[bufferSize];

        while (true) {
            bytes = fileIn.read(buffer, 0, bufferSize); // Read from file
            totalBytesTransferred += bytes;

            if (bytes <= 0)
                break; // Check for end of file

            socketOut.write(buffer, 0, bytes); // Write bytes to socket
            printTransferProgress(totalFileByteSize, totalFileByteSize - totalBytesTransferred);

            if (totalBytesTransferred >= totalFileByteSize) {
                totalBytesTransferred = 0;
                break;
            }
        }

        fileIn.close();

        if (debugFlag == 1) {
            System.out.println("Finished sending " + filename + " to " + connection.getInetAddress());
        }
    }

    private void processClientUpload() throws FileNotFoundException, IOException {
        try {
            File fileToWrite = new File("Files/" + filename);

            if (fileToWrite.exists()) {
                if (debugFlag == 1) {
                    System.out.println("** File " + filename + " already exists. Aborting upload.");
                }

                socketOut.writeUTF("ErrFileExists");
                return;
            } else {
                socketOut.writeUTF("OkToWrite");
            }

            fileOut = new FileOutputStream(fileToWrite);

            // Read file contents from server
            while (true) {
                bytes = socketIn.read(buffer, 0, BUFFER_SIZE); // Read from socket
                if (bytes <= 0)
                    break; // Check for end of file

                // Write
                fileOut.write(buffer, 0, bytes);
            }

            fileOut.close();

            if (debugFlag == 1) {
                System.out.println("Finished receiving " + filename + " from " + connection.getInetAddress());
            }
        } catch (Exception e) {
            System.out.println("ERROR: Could not read file. " + e.getLocalizedMessage());
        }
    }

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
        try {
            int debugFlag = parseCommandLineArgument(args);
            Server server = new Server(5000, debugFlag);
        } catch (InvalidArgumentException e) {
            System.out.println("Invalid argument. Please run as:");
            System.out.println();
            System.out.println("  `java Server` or `java Server DEBUG=1`");
        }
    }

    public static int parseCommandLineArgument(String[] args) throws InvalidArgumentException {
        if (args.length <= 0)
            return 0;

        String[] splitByEqual = args[0].split("=");

        if (splitByEqual.length < 2) {
            throw new InvalidArgumentException();
        }

        if (splitByEqual[0].compareTo("DEBUG") != 0 && splitByEqual[0].compareTo("debug") != 0) {
            throw new InvalidArgumentException();
        }

        int debugFlag = args.length > 0 ? Integer.parseInt(splitByEqual[1]) : 0;
        return debugFlag;
    }
}

class InvalidArgumentException extends Exception {
    private static final long serialVersionUID = 1L;
}
