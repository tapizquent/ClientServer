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
    private static String filename = "";
    private static ClientMode clientMode = ClientMode.read;
    private static long startByteIndex = -1;
    private static long endByteIndex = -1;

    public Client(String host, int port, String filename) {
        try {
            connection = new Socket(host, port);

            socketIn = new DataInputStream(connection.getInputStream()); // Read data from server
            socketOut = new DataOutputStream(connection.getOutputStream()); // Write data to server

            if (clientMode == ClientMode.write) {
                // Write flag to let server know whether to upload or download
                socketOut.writeInt(1);
                socketOut.writeUTF(filename); // Write filename to server
                fileToWrite = new File("Files/" + filename);
                processUpload();
            } else {
                // Write flag to let server know whether to upload or download
                socketOut.writeInt(0);
                socketOut.writeUTF(filename); // Write filename to server
                fileToWrite = new File("Files/" + filename);
                processDownload();
            }

            connection.close();
        } catch (Exception ex) {
            System.out.println("Error: " + ex);
        }
    }

    public void processDownload() {
        try {

            long skipItems = startByteIndex < 0 ? 0 : startByteIndex - 1;

            socketOut.writeLong(skipItems);
            socketOut.writeLong(endByteIndex);

            String downloadSignal = socketIn.readUTF();

            if (downloadSignal.compareTo("ErrInvalidByteRange") == 0) {
                System.out.println("** Invalid byte range specified. More bytes requested than available in file");
                return;
            }

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
        } catch (Exception e) {
            System.out.println("ERROR: Could not read file");
        }
    }

    public void processUpload() {
        try {
            String uploadSignal = socketIn.readUTF();

            if (uploadSignal.compareTo("ErrFileExists") == 0) {
                System.out.println("** File already in server. Upload permission denied.");
                return;
            }

            FileInputStream fileInputStream = new FileInputStream("Files/" + filename);
            int totalFileByteSize = fileInputStream.available();
            int bufferSize = totalFileByteSize > BUFFER_SIZE ? BUFFER_SIZE : totalFileByteSize;
            buffer = new byte[bufferSize];

            while (true) {
                bytes = fileInputStream.read(buffer, 0, bufferSize); // Read from file
                if (bytes <= 0)
                    break; // Check for end of file
                socketOut.write(buffer, 0, bytes); // Write bytes to socket
                // printTransferProgress(totalFileByteSize, fileInputStream.available());
            }

            fileInputStream.close();
        } catch (Exception e) {
            System.out.println("ERROR: Could not read file");
        }
    }

    public static void main(String[] args) {
        try {
            parseCommandLineArguments(args);
            Client client = new Client("127.0.0.1", 5000, filename);
        } catch (InvalidArgumentException e) {
            printConsoleHelp(e.getCode());
        }
    }

    public static void parseCommandLineArguments(String[] args) throws InvalidArgumentException {
        if (args.length <= 0) {
            throw new InvalidArgumentException(ArgumentErrorCode.missingAll);
        }

        // We are trying to access Client in Write mode
        if (args[0].compareTo("-w") == 0) {
            if (args.length < 2) {
                throw new InvalidArgumentException(ArgumentErrorCode.filename);
            }

            filename = args[1];
            clientMode = ClientMode.write;
        } else {
            filename = args[0];

            if (args.length < 2) {
                return;
            }

            if (args[1].compareTo("-s") == 0) {
                if (args.length < 3) {
                    throw new InvalidArgumentException(ArgumentErrorCode.startByte);
                }

                startByteIndex = Long.parseLong(args[2]);
            }

            if (args.length < 4) {
                return;
            }

            if (args[3].compareTo("-e") == 0) {
                if (args.length < 5) {
                    throw new InvalidArgumentException(ArgumentErrorCode.endByte);
                }

                endByteIndex = Long.parseLong(args[4]);

                if (startByteIndex > endByteIndex) {
                    throw new InvalidArgumentException(ArgumentErrorCode.startIndexAfterEnd);
                }
            }
        }
    }

    public static void printConsoleHelp(ArgumentErrorCode code) {
        switch (code) {
            case filename:
                System.out.println("ERROR: FILE NAME MISSING");
                break;
            case missingAll:
                System.out.println("ERROR: MISSING ARGUMENTS");
                break;
            case startByte:
                System.out.println("ERROR: START BYTE VALUE MISSING");
                break;
            case endByte:
                System.out.println("ERROR: END BYTE VALUE MISSING");
                break;
            case startIndexAfterEnd:
                System.out.println("ERROR: END BYTE INDEX CANNOT BE LESS THAN START BYTE INDEX");
                break;
            default:
                System.out.println("ERROR: INVALID ARGUMENT");
                break;
        }

        System.out.println();
        System.out.println("Please use as follows:");
        System.out.println();
        System.out.println("  java Client [-w] <filename> [-s <startByte>] [-e <endByte>]");
        System.out.println();
        System.out.println("  <filename> : Required whether reading or writting file");
        System.out.println("  -w : Sets Client in write mode. File with <filename> will upload to Server");
        System.out.println("  -s : Sets the start byte to copy file (if whole file is not needed)");
        System.out.println("  -e : Sets the end byte to copy file (if whole file is not needed)");
    }
}

class InvalidArgumentException extends Exception {
    private ArgumentErrorCode code;
    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(ArgumentErrorCode code) {
        this.code = code;
    }

    public ArgumentErrorCode getCode() {
        return code;
    }
}

enum ArgumentErrorCode {
    filename, startByte, endByte, missingAll, startIndexAfterEnd
}

enum ClientMode {
    read, write
}