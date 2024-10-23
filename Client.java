import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Client {
    private String serverAddress;
    private int clientPort; 
    private int serverPort; 
    private String protocol;
    private Transport transport;

    public Client(int clientPort, String serverAddress, int serverPort, String protocol) throws IOException {
        this.clientPort = clientPort;
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.protocol = protocol;

        Socket socket;
        if (clientPort > 0) {
            socket = new Socket();
            socket.bind(new InetSocketAddress(clientPort));
            socket.connect(new InetSocketAddress(serverAddress, serverPort));
        } else {
            socket = new Socket(serverAddress, serverPort);
        }

        if (protocol.equalsIgnoreCase("tcp")) {
            transport = new TcpTransport(socket);
        } else if (protocol.equalsIgnoreCase("snw")) {
            transport = new SnwTransport(socket);
        } else {
            System.out.println("Unknown protocol: " + protocol);
            socket.close();
            throw new IllegalArgumentException("Unknown protocol: " + protocol);
        }
        System.out.println("Client started using protocol: " + protocol.toUpperCase());
    }

    public void start() throws IOException {
        BufferedReader console = new BufferedReader(
                new InputStreamReader(System.in));
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
                System.out.println(transport.receive());
            }
            System.out.println("Enter next command:");
        }
        transport.close();
    }

    private void handlePut(String filename) throws IOException {
        Path filePath = Paths.get("client_files", filename);
        if (!Files.exists(filePath)) {
            System.out.println("File '" + filename +
                    "' does not exist in client_files directory.");
            return;
        }
        System.out.println("Sending PUT request for: " + filename);
        transport.send("put " + filename);
        String response = transport.receive();
        System.out.println("Server response: " + response);
        if ("READY".equals(response)) {
            byte[] data = Files.readAllBytes(filePath);
            transport.send(String.valueOf(data.length));
            response = transport.receive();
            System.out.println("Server response: " + response);
            if (!"SIZE_RECEIVED".equals(response)) {
                System.out.println("Server did not acknowledge file size.");
                return;
            }
            transport.sendFile(data);
            String serverResponse = transport.receive();
            System.out.println("Server response: " + serverResponse);
            if ("UPLOAD_SUCCESS".equals(serverResponse)) {
                Files.delete(filePath);
                System.out.println("File '" + filename + "' uploaded and moved to server successfully.");
            } else {
                System.out.println("Server response: " + serverResponse);
            }
        } else {
            System.out.println("Server response: " + response);
        }
    }

    private void handleGet(String filename) throws IOException {
        System.out.println("Sending GET request for: " + filename);
        transport.send("get " + filename);
        String response = transport.receive();
        System.out.println("Server response: " + response);
        if ("READY".equals(response)) {
            String deliverySource = transport.receive();
            System.out.println("Delivery source: " + deliverySource);
            byte[] data = transport.receiveFile();
            System.out.println("Received file data");
            Files.write(Paths.get("client_files", filename), data);
            System.out.println("File '" + filename + "' downloaded successfully from " + deliverySource);
        } else if (response.startsWith("ERROR:")) {
            System.out.println(response);
        } else {
            System.out.println("Unexpected server response: " + response);
        }
    }

    public static void main(String[] args) {
        try {
            int clientPort = 0; // Default client port 
            String serverAddress = "localhost";
            int serverPort = 4040; // Default server port
            String protocol = "tcp"; // Default protocol

            if (args.length >= 1) {
                clientPort = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                serverAddress = args[1];
            }
            if (args.length >= 3) {
                serverPort = Integer.parseInt(args[2]);
            }
            if (args.length >= 4) {
                protocol = args[3];
            }

            Client client = new Client(clientPort, serverAddress, serverPort, protocol);
            client.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            System.out.println("Usage: java Client [client port] [server ip] [server port] [tcp/snw]");
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
        }
    }
}