import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class CacheService {
    private int port;
    private String serverIp;
    private int serverPort; 
    private String protocol;

    public CacheService(int port, String protocol) {
        this.port = port;
        this.protocol = protocol;
    }

    public void start() throws IOException {
        Files.createDirectories(Paths.get("cache_files"));
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Cache service started on port " + port + " using protocol: " + protocol.toUpperCase());

        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newCachedThreadPool();

        while (true) {
            Socket clientSocket = serverSocket.accept();
            executor.execute(() -> {
                try {
                    handleClient(clientSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
        }
    }

    private void handleClient(Socket socket) throws IOException {
        System.out.println("Handling new client connection");
        try (
                DataInputStream dataIn = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
                DataOutputStream dataOut = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        ) {
            String command = dataIn.readUTF();
            System.out.println("Received command: " + command);
            if (command.startsWith("GET ")) {
                String filename = command.substring(4).trim();
                Path filePath = Paths.get("cache_files", filename);
                if (Files.exists(filePath)) {
                    dataOut.writeUTF("FOUND");
                    dataOut.flush();
                    byte[] data = Files.readAllBytes(filePath);
                    dataOut.writeInt(data.length);
                    dataOut.flush();
                    dataOut.write(data);
                    dataOut.flush();
                    System.out.println("Served file '" + filename + "' from cache");
                } else {
                    dataOut.writeUTF("NOT_FOUND");
                    dataOut.flush();
                    System.out.println("File '" + filename + "' not found in cache");
                }
            } else if (command.startsWith("STORE ")) {
                String filename = command.substring(6).trim();
                int size = dataIn.readInt();
                System.out.println("Storing file '" + filename + "' of size " + size);
                byte[] data = new byte[size];
                dataIn.readFully(data);
                Path filePath = Paths.get("cache_files", filename);
                Files.write(filePath, data);
                dataOut.writeUTF("STORED");
                dataOut.flush();
                System.out.println("Stored file '" + filename + "' in cache");
            } else {
                dataOut.writeUTF("INVALID_COMMAND");
                dataOut.flush();
                System.out.println("Received invalid command");
            }
        } catch (Exception e) {
            System.out.println("Exception in cache service: " + e.getMessage());
            e.printStackTrace();
        } finally {
            socket.close();
        }
    }

    public static void main(String[] args) {
        try {
            int port = 5050; // Default port
            String protocol = "tcp"; // Default protocol

            if (args.length >= 1) {
                port = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                protocol = args[1];
            }

            CacheService cacheService = new CacheService(port, protocol);
            cacheService.start();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NumberFormatException e) {
            System.out.println("Invalid port number.");
            System.out.println("Usage: java CacheService [port] [tcp/snw]");
        }
    }
}