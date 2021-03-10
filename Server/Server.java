import java.io.*;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.net.Socket;
import java.util.concurrent.*;

// PLEASE ADJUST PORT NUMBER HERE AS NEEDED
class Port {
    public static int port = 2296;
}

public class Server {
    private Socket connection;
    private ServerSocket socket;
    private ExecutorService executorService;

    public Server() {
    }

    public void start(int port, int debugFlag) {
        if (debugFlag == 1) {
            System.out.println("Server running on port: " + port);
        }

        executorService = Executors.newCachedThreadPool();

        try {
            socket = new ServerSocket(port);
            // Wait for connection and process it
            while (true) {
                try {
                    connection = socket.accept(); // Block for connection request
                    executorService.execute(new ConnectionManager(connection, debugFlag));

                } catch (Exception ex) {
                    System.out.println("Error: " + ex);
                }
            }
        } catch (IOException i) {
            System.out.println("Error: " + i);
        }

        executorService.shutdown();
    }

    public static void main(String[] args) {
        try {
            int debugFlag = parseCommandLineArgument(args);
            Server server = new Server();
            server.start(Port.port, debugFlag);
        } catch (InvalidArgumentException e) {
            System.out.println(e.getMessage().isEmpty() ? "ERROR: INVALID ARGUMENTS" : e.getMessage());
            System.out.println();
            System.out.println("Please run as:");
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

        int debugFlag = 0;

        try {
            int parsedDebugFlag = Integer.parseInt(splitByEqual[1]);

            if (parsedDebugFlag != 0 && parsedDebugFlag != 1) {
                throw new NumberFormatException();
            }

            debugFlag = parsedDebugFlag;
        } catch (NumberFormatException e) {
            throw new InvalidArgumentException(
                    "ERROR: CANNOT PARSE DEBUG ARGUMENT. IS NOT EQUAL TO 0 OR 1, OR IT'S NOT A NUMBER");
        }

        return debugFlag;
    }
}

class InvalidArgumentException extends Exception {
    private String message;

    public InvalidArgumentException() {
        this("");
    }

    public InvalidArgumentException(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    private static final long serialVersionUID = 1L;
}

class ConnectionManager implements Runnable {
    private Socket connection;
    private final int BUFFER_SIZE = 4096;
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

    public ConnectionManager(Socket serverSocket, int debugFlag) {
        connection = serverSocket;
        this.debugFlag = debugFlag;
    }

    public void run() {
        try {
            try {
                socketIn = new DataInputStream(connection.getInputStream()); // Read data from client

                @SuppressWarnings("deprecation")
                String request = socketIn.readLine(); // Now you get GET index.html HTTP/1.1

                if (request.trim().length() == 1) {
                    clientMode = request.trim().equals("1") ? 1 : 0;
                    socketOut = new DataOutputStream(connection.getOutputStream()); // Write data to client
                    processClientRequest(debugFlag);
                    System.out.println();
                } else {
                    String[] requestParam = request.split(" ");
                    String path = requestParam[1];
                    processHTTPGetRequest(path);
                    System.out.println();
                }
            } catch (Exception e) {
                System.out.println("ERROR: Error in ConnectionManager. " + e);
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            System.out.println("ERROR: Could not access socket.");
        }
    }

    private void processClientRequest(int debugFlag) throws IOException, FileNotFoundException {
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
    }

    private long totalBytesTransferred = 0;

    private void processClientDownload() throws IOException, FileNotFoundException {
        File fileInFiles = new File("Files/" + filename);

        if (!fileInFiles.exists()) {
            if (debugFlag == 1) {
                System.out.println("** File with name: " + filename + " does not exist.");
                System.out.println("   Please ensure file is in Files/ directory of Server");
            }
            socketOut.writeUTF("ErrFileDoesNotExist");
            return;
        } else {
            socketOut.writeUTF("OkToRead");
        }

        long skipItems = socketIn.readLong();
        long endByteIndex = socketIn.readLong();
        long lengthOfBytesToRead = endByteIndex - skipItems;
        fileIn = new FileInputStream(fileInFiles);
        fileIn.skip(skipItems);
        long totalFileByteSize = fileInFiles.length();

        if (endByteIndex > 0) {
            if (totalFileByteSize >= lengthOfBytesToRead) {
                totalFileByteSize = lengthOfBytesToRead;
                socketOut.writeUTF("OkToRead");
            } else {
                if (debugFlag == 1) {
                    System.out.println("** Invalid byte range specified");
                }
                socketOut.writeUTF("ErrInvalidByteRange");
                return;
            }
        }

        int bufferSize = totalFileByteSize > BUFFER_SIZE ? BUFFER_SIZE : (int) totalFileByteSize;

        buffer = new byte[bufferSize];

        while (true) {
            if (bufferSize > (totalFileByteSize - totalBytesTransferred)) {
                bufferSize = (int) (totalFileByteSize - totalBytesTransferred);
            }

            bytes = fileIn.read(buffer, 0, bufferSize); // Read from file

            if (bytes <= 0)
                break; // Check for end of file

            totalBytesTransferred += bytes;

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
            System.out.println("ERROR: Could not read file. " + e);
            System.out.println("ERROR: Could not read file. " + e.getLocalizedMessage());
        }
    }

    private void processHTTPGetRequest(String path) {
        try {
            PrintStream out = new PrintStream(connection.getOutputStream(), true);

            if (path.equals("/")) {
                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                return;
            }

            File file = new File("Files/" + path);

            if (!file.exists()) {
                if (debugFlag == 1) {
                    System.out.println("File not in server: " + path);
                }

                out.println("HTTP/1.1 404 Not Found");
                out.println("Content-Type: text/html");
                out.println();
                return; // the file does not exist
            }

            FileReader fr = new FileReader(file);
            BufferedReader bfr = new BufferedReader(fr);
            String line;
            out.println("HTTP/1.1 200 OK\r");
            out.println("Content-Type: text/html\r");
            out.println("Content-Length: " + file.length() + "\r");
            out.println("Connection: Closed\r");
            out.println();
            out.println();
            while ((line = bfr.readLine()) != null) {
                out.println(line + "\r");
            }

            bfr.close();
            out.close();
        } catch (Exception e) {
            System.out.print("** ERROR: Can't access outputStream");
            System.out.print("   " + e);
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
}