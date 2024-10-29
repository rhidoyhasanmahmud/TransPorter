import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class client {
    private final String remoteServerIp;
    private final int localClientPort;
    private final int remoteServerPort;
    private final String connectionProtocol;
    private final DataTransport transportLayer;

    public client(int localClientPort, String remoteServerIp, int remoteServerPort, String connectionProtocol) throws IOException {
        this.localClientPort = localClientPort;
        this.remoteServerIp = remoteServerIp;
        this.remoteServerPort = remoteServerPort;
        this.connectionProtocol = connectionProtocol.toLowerCase();

        this.transportLayer = setupTransport();
        System.out.println("Client initiated using protocol: " + connectionProtocol.toUpperCase());
    }

    public static void main(String[] args) {
        try {
            String serverIp = "localhost";
            int serverPort = 10000;
            String cacheIp = "localhost";
            int cachePort = 20000;

            int clientPort = 20001;
            String protocol = "tcp";

            if (args.length >= 1) {
                serverIp = args[0];
            }
            if (args.length >= 2) {
                serverPort = Integer.parseInt(args[1]);
            }
            if (args.length >= 3) {
                cacheIp = args[2];
            }
            if (args.length >= 4) {
                cachePort = Integer.parseInt(args[3]);
            }
            if (args.length >= 5) {
                protocol = args[4];
            }
            client clientInstance = new client(clientPort, serverIp, serverPort, protocol);
            clientInstance.startClient();
        } catch (IllegalArgumentException | IOException e) {
            System.out.println("Usage: java Client [server ip] [server port] [client port] [protocol]");
        }
    }

    private Socket setupSocket() throws IOException {
        if ("tcp".equalsIgnoreCase(connectionProtocol)) {
            Socket socket = new Socket();
            if (localClientPort > 0) {
                socket.bind(new InetSocketAddress(localClientPort));
            }
            socket.connect(new InetSocketAddress(remoteServerIp, remoteServerPort));
            return socket;
        }
        return null;
    }

    private DataTransport setupTransport() throws IOException {
        switch (connectionProtocol) {
            case "tcp":
                Socket socket = setupSocket();
                return new tcp_transport(socket);
            case "snw":
                InetAddress serverAddress = InetAddress.getByName(remoteServerIp);
                return new snw_transport(serverAddress, remoteServerPort, localClientPort);
            default:
                throw new IllegalArgumentException("Unsupported protocol: " + connectionProtocol);
        }
    }


    public void startClient() {
        try (BufferedReader consoleInput = new BufferedReader(new InputStreamReader(System.in))) {
            String userCommand;
            System.out.println("Enter commands (put filename / get filename / quit):");
            while ((userCommand = consoleInput.readLine()) != null) {
                if (userCommand.equalsIgnoreCase("quit")) {
                    System.out.println("Client shutting down.");
                    transportLayer.transmitMessage("quit");
                    break;
                } else if (userCommand.startsWith("put ")) {
                    executeUpload(userCommand.substring(4).trim());
                } else if (userCommand.startsWith("get ")) {
                    executeDownload(userCommand.substring(4).trim());
                } else {
                    transportLayer.transmitMessage(userCommand);
                    String serverResponse = transportLayer.receiveMessage();
                    System.out.println(serverResponse);
                }
                System.out.println("Enter commands (put filename / get filename / quit):");
            }
        } catch (IOException e) {
            System.err.println("Error during client operation: " + e.getMessage());
        } finally {
            try {
                transportLayer.close();
            } catch (IOException e) {
                System.err.println("Error closing transport.");
            }
        }
    }

    private void executeUpload(String fileName) {
        Path localFilePath = Paths.get("client_storage", fileName);
        if (!Files.exists(localFilePath)) {
            System.out.println("File does not exist.");
            return;
        }

        try {
            // Send upload command to server
            transportLayer.transmitMessage("put " + fileName);
            String serverResponse = transportLayer.receiveMessage();

            // Confirm server is ready to receive
            if ("READY".equalsIgnoreCase(serverResponse)) {
                byte[] fileData = Files.readAllBytes(localFilePath);
                transportLayer.transmitMessage(String.valueOf(fileData.length));
                serverResponse = transportLayer.receiveMessage();

                if (!"SIZE_CONFIRMED".equalsIgnoreCase(serverResponse)) {
                    System.err.println("Server did not confirm file size.");
                    return;
                }

                // Transmit the file data
                transportLayer.transmitFile(fileData);
                String uploadResponse = transportLayer.receiveMessage();
                if ("UPLOAD_SUCCESSFUL".equalsIgnoreCase(uploadResponse)) {
                    System.out.println("File uploaded successfully.");
                } else {
                    System.err.println("File upload failed with server response: " + uploadResponse);
                }
            } else {
                System.err.println("Server not ready for file upload. Response: " + serverResponse);
            }
        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
        }
    }

    private void executeDownload(String fileName) {
        try {
            transportLayer.transmitMessage("get " + fileName);
            String serverResponse = transportLayer.receiveMessage();
            if ("READY".equalsIgnoreCase(serverResponse)) {
                String source = transportLayer.receiveMessage();
                byte[] fileData = transportLayer.receiveFileData();
                Path destinationPath = Paths.get("client_storage", fileName);
                Files.createDirectories(destinationPath.getParent());
                Files.write(destinationPath, fileData);
                switch (source.toLowerCase()) {
                    case "server":
                        System.out.println("File downloaded from server.");
                        break;
                    case "cache":
                        System.out.println("File downloaded from cache.");
                        break;
                    default:
                        System.out.println("File source unknown.");
                        break;
                }
            } else if (serverResponse.startsWith("ERROR:")) {
                System.out.println("File not found.");
            } else {
                System.out.println("Unexpected response from server.");
            }
        } catch (IOException e) {
            System.err.println("Error during file download.");
        }
    }
}
