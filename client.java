import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class client {
    private final String serverAddress;
    private final int clientPort;
    private final int serverPort;
    private final String protocol;
    private final Transport transport;

    public client(int clientPort, String serverAddress, int serverPort, String protocol) throws IOException {
        this.clientPort = clientPort;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.protocol = protocol.toLowerCase();

        this.transport = initializeTransport();
        System.out.println("Client started using protocol: " + protocol.toUpperCase());
    }

    private Transport initializeTransport() throws IOException {
        switch (protocol) {
            case "tcp":
                Socket socket = initializeSocket();
                return new tcp_transport(socket);
            case "snw":
                InetAddress serverInetAddress = InetAddress.getByName(serverAddress);
                return new snw_transport(serverInetAddress, serverPort, clientPort);
            default:
                throw new IllegalArgumentException("Unknown protocol: " + protocol);
        }
    }

    private Socket initializeSocket() throws IOException {
        Socket socket = new Socket();
        if (clientPort > 0) {
            socket.bind(new InetSocketAddress(clientPort));
        }
        socket.connect(new InetSocketAddress(serverAddress, serverPort));
        return socket;
    }

    public void start() {
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            String command;
            System.out.println("Enter commands (put filename / get filename / quit):");
            while ((command = console.readLine()) != null) {
                if (command.equalsIgnoreCase("quit")) {
                    System.out.println("Exiting client.");
                    transport.send("quit");
                    break;
                } else if (command.startsWith("put ")) {
                    handlePut(command.substring(4).trim());
                } else if (command.startsWith("get ")) {
                    handleGet(command.substring(4).trim());
                } else {
                    transport.send(command);
                    String response = transport.receive();
                    System.out.println(response);
                }
                System.out.println("Enter next command (put filename / get filename / quit):");
            }
        } catch (IOException e) {
            System.err.println("Error during client operation: " + e.getMessage());
        } finally {
            try {
                transport.close();
            } catch (IOException e) {
                System.err.println("Error closing transport.");
            }
        }
    }

    private void handlePut(String filename) {
        Path filePath = Paths.get("client_files", filename);
        if (!Files.exists(filePath)) {
            System.out.println("File does not exist.");
            return;
        }

        try {
            transport.send("put " + filename);
            String response = transport.receive();
            if ("READY".equalsIgnoreCase(response)) {
                byte[] data = Files.readAllBytes(filePath);
                transport.send(String.valueOf(data.length));
                response = transport.receive();
                if (!"SIZE_RECEIVED".equalsIgnoreCase(response)) {
                    System.out.println("Server did not acknowledge file size.");
                    return;
                }
                transport.sendFile(data);
                String serverResponse = transport.receive();
                if ("UPLOAD_SUCCESS".equalsIgnoreCase(serverResponse)) {
                    System.out.println("File uploaded successfully.");
                } else {
                    System.out.println("Server response: " + serverResponse);
                }
            } else {
                System.out.println("Server response: " + response);
            }
        } catch (IOException e) {
            System.err.println("Error during file upload: " + e.getMessage());
        }
    }

    private void handleGet(String filename) {
        try {
            transport.send("get " + filename);
            String response = transport.receive();
            if ("READY".equalsIgnoreCase(response)) {
                String deliverySource = transport.receive();
                byte[] data = transport.receiveFile();
                Path destination = Paths.get("client_files", filename);
                Files.createDirectories(destination.getParent());
                Files.write(destination, data);
                switch (deliverySource.toLowerCase()) {
                    case "server":
                        System.out.println("File delivered from server.");
                        break;
                    case "cache":
                        System.out.println("File delivered from cache.");
                        break;
                    default:
                        System.out.println("File delivered from unknown source.");
                        break;
                }
            } else if (response.startsWith("ERROR:")) {
                System.out.println("File not found.");
            } else {
                System.out.println("Unexpected server response.");
            }
        } catch (IOException e) {
            System.err.println("Error during file retrieval: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        try {
            client clientInstance = getClientInstance(args);
            clientInstance.start();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.out.println("Usage: java client [server ip] [server port] [client port] [protocol]");
        } catch (IOException e) {
            System.err.println("IO Exception occurred while starting the client: " + e.getMessage());
            System.out.println("Usage: java client [server ip] [server port] [client port] [protocol]");
        }
    }

    private static client getClientInstance(String[] args) throws IOException {
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
        return new client(clientPort, serverIp, serverPort, protocol);
    }
}
