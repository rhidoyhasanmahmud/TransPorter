import java.io.*;
import java.net.*;
import java.nio.file.*;

public class Server {
    private int port;
    private int cachePort;
    private String cacheIp;
    private String protocol;

    public Server(int port, String protocol, String cacheIp, int cachePort) {
        this.port = port;
        this.protocol = protocol;
        this.cacheIp = cacheIp;
        this.cachePort = cachePort;
    }

    public void start() throws IOException {
        Files.createDirectories(Paths.get("server_files"));
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port + " using protocol: " + protocol.toUpperCase());
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(clientSocket);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    private void handleClient(Socket socket) throws IOException {
        try (Transport transport = createTransport(socket)) {
            String command;
            while ((command = transport.receive()) != null) {
                if (command.equalsIgnoreCase("quit")) {
                    System.out.println("Client has disconnected.");
                    break;
                } else if (command.startsWith("put ")) {
                    handlePut(command.substring(4).trim(), transport);
                } else if (command.startsWith("get ")) {
                    handleGet(command.substring(4).trim(), transport);
                } else {
                    transport.send("Unknown command");
                    System.out.println("Received unknown command: " + command);
                }
            }
        }
    }

    private void handlePut(String filename, Transport transport) throws IOException {
        System.out.println("Received PUT request for: " + filename);
        transport.send("READY");
        String sizeStr = transport.receive();
        long fileSize = Long.parseLong(sizeStr);
        transport.send("SIZE_RECEIVED");
        byte[] data = transport.receiveFile();
        Path filePath = Paths.get("server_files", filename);
        Files.write(filePath, data);
        transport.send("UPLOAD_SUCCESS");
        System.out.println("File '" + filename + "' received and saved.");
    }

    private void handleGet(String filename, Transport transport) throws IOException {
        System.out.println("Received GET request for: " + filename);
        byte[] data = null;
        String deliverySource = null;

        data = getFileFromCache(filename);

        if (data != null) {
            deliverySource = "cache";
            System.out.println("File delivered from cache.");
        } else {
            Path serverFilePath = Paths.get("server_files", filename);
            if (Files.exists(serverFilePath)) {
                data = Files.readAllBytes(serverFilePath);
                deliverySource = "server";
                System.out.println("File delivered from server.");
                storeFileInCache(filename, data);
            } else {
                System.out.println("File not found on server: " + filename);
            }
        }

        if (data != null) {
            transport.send("READY");
            transport.send(deliverySource);
            transport.sendFile(data);
        } else {
            transport.send("ERROR: File '" + filename + "' not found on server.");
        }
    }

    private byte[] getFileFromCache(String filename) {
        System.out.println("Attempting to retrieve file from cache: " + filename);
        try (Socket cacheSocket = new Socket(cacheIp, cachePort);
             DataInputStream dataIn = new DataInputStream(new BufferedInputStream(cacheSocket.getInputStream()));
             DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(cacheSocket.getOutputStream()))) {

            dataOut.writeUTF("GET " + filename);
            dataOut.flush();
            String response = dataIn.readUTF();
            if ("FOUND".equals(response)) {
                int size = dataIn.readInt();
                byte[] data = new byte[size];
                dataIn.readFully(data);
                return data;
            } else {
                System.out.println("File not found in cache: " + filename);
                return null;
            }
        } catch (IOException e) {
            System.out.println("Error communicating with cache service: " + e.getMessage());
            return null;
        }
    }

    private void storeFileInCache(String filename, byte[] data) {
        System.out.println("Storing file in cache: " + filename);
        try (Socket cacheSocket = new Socket(cacheIp, cachePort);
             DataInputStream dataIn = new DataInputStream(new BufferedInputStream(cacheSocket.getInputStream()));
             DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(cacheSocket.getOutputStream()))) {

            dataOut.writeUTF("STORE " + filename);
            dataOut.flush();
            dataOut.writeInt(data.length);
            dataOut.flush();
            dataOut.write(data);
            dataOut.flush(); 
            String response = dataIn.readUTF();
            if ("STORED".equals(response)) {
                System.out.println("File '" + filename + "' stored in cache");
            } else {
                System.out.println("Failed to store file '" + filename + "' in cache");
            }
        } catch (IOException e) {
            System.out.println("Error communicating with cache service: " + e.getMessage());
        }
    }

    private Transport createTransport(Socket socket) throws IOException {
        if (protocol.equalsIgnoreCase("tcp")) {
            return new TcpTransport(socket);
        } else if (protocol.equalsIgnoreCase("snw")) {
            return new SnwTransport(socket);
        } else {
            throw new IllegalArgumentException("Unknown protocol: " + protocol);
        }
    }

    public static void main(String[] args) {
        try {
            int port = 4040; // Default port
            String protocol = "tcp"; // Default protocol
            String cacheIp = "localhost"; // Default cache IP
            int cachePort = 5050; // Default cache port

            if (args.length >= 1) {
                port = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                protocol = args[1];
            }
            if (args.length >= 3) {
                cacheIp = args[2];
            }
            if (args.length >= 4) {
                cachePort = Integer.parseInt(args[3]);
            }

            Server server = new Server(port, protocol, cacheIp, cachePort);
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            System.out.println("Usage: java Server [port] [tcp/snw] [cache ip] [cache port]");
        }
    }
}