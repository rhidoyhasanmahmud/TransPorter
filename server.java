import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class server {
    private final int serverPort;
    private final int cacheServerPort;
    private final String cacheServerIp;
    private final String communicationProtocol;
    private final ExecutorService clientExecutor;
    private final CacheStorageManager cacheManager;

    public server(int serverPort, String communicationProtocol, String cacheServerIp, int cacheServerPort) throws IOException {
        this.serverPort = serverPort;
        this.communicationProtocol = communicationProtocol.toLowerCase();
        this.cacheServerIp = cacheServerIp;
        this.cacheServerPort = cacheServerPort;
        this.cacheManager = new CacheStorageManager("server_storage");
        this.clientExecutor = Executors.newCachedThreadPool();
    }

    public static void main(String[] args) {
        try {
            int serverPort = 10000;
            String protocol = "tcp";
            String cacheServerIp = "localhost";
            int cacheServerPort = 20000;

            if (args.length >= 1) {
                serverPort = Integer.parseInt(args[0]);
            }
            if (args.length >= 2) {
                protocol = args[1];
            }
            if (args.length >= 3) {
                cacheServerIp = args[2];
            }
            if (args.length >= 4) {
                cacheServerPort = Integer.parseInt(args[3]);
            }
            server serverInstance = new server(serverPort, protocol, cacheServerIp, cacheServerPort);
            serverInstance.initialize();
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        } catch (IOException e) {
            System.out.println("Usage: java FileServer [port] [protocol] [cache ip] [cache port]");
        }
    }

    public void initialize() throws IOException {
        Files.createDirectories(Paths.get("server_storage"));
        if ("tcp".equalsIgnoreCase(communicationProtocol)) {
            try (ServerSocket serverSocket = new ServerSocket(serverPort)) {
                System.out.println("Server active on port " + serverPort + " with protocol: " + communicationProtocol.toUpperCase());
                while (true) {
                    try {
                        Socket clientSocket = serverSocket.accept();
                        clientExecutor.execute(() -> processClientRequest(clientSocket));
                    } catch (IOException e) {
                        System.err.println("Client connection failed.");
                    }
                }
            }
        } else if ("snw".equalsIgnoreCase(communicationProtocol)) {
            System.out.println("Server active on port " + serverPort + " with protocol: " + communicationProtocol.toUpperCase());
            clientExecutor.execute(() -> processSNWRequests());
        } else {
            throw new IllegalArgumentException("Unsupported protocol: " + communicationProtocol);
        }
    }

    private DataTransport initializeTransport(Socket clientSocket) throws IOException {
        switch (communicationProtocol) {
            case "tcp":
                return new tcp_transport(clientSocket);
            case "snw":
                // This method won't be called for SNW
                throw new UnsupportedOperationException("SNW protocol does not use Socket in this context");
            default:
                clientSocket.close();
                throw new IllegalArgumentException("Unsupported protocol: " + communicationProtocol);
        }
    }

    private void processClientRequest(Socket clientSocket) {
        try (DataTransport transportLayer = initializeTransport(clientSocket)) {
            String clientCommand;
            while ((clientCommand = transportLayer.receiveMessage()) != null) {
                if ("quit".equalsIgnoreCase(clientCommand)) {
                    System.out.println("Client disconnected.");
                    break;
                } else if (clientCommand.startsWith("put ")) {
                    executePut(clientCommand.substring(4).trim(), transportLayer);
                } else if (clientCommand.startsWith("get ")) {
                    executeGet(clientCommand.substring(4).trim(), transportLayer);
                } else {
                    transportLayer.transmitMessage("Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing client request.");
        }
    }

    private void processSNWRequests() {
        try (snw_transport transportLayer = new snw_transport(serverPort)) {
            String clientCommand;
            while (true) {
                clientCommand = transportLayer.receiveMessage();
                if (clientCommand == null) {
                    break;
                }
                if ("quit".equalsIgnoreCase(clientCommand)) {
                    System.out.println("Client disconnected.");
                    break;
                } else if (clientCommand.startsWith("put ")) {
                    executePut(clientCommand.substring(4).trim(), transportLayer);
                } else if (clientCommand.startsWith("get ")) {
                    executeGet(clientCommand.substring(4).trim(), transportLayer);
                } else {
                    transportLayer.transmitMessage("Unknown command");
                }
            }
        } catch (IOException e) {
            System.err.println("Error processing SNW client request: " + e.getMessage());
        }
    }

    private void executePut(String fileName, DataTransport transportLayer) {
        try {
            // Prepare server for file reception
            transportLayer.transmitMessage("READY");
            String fileSizeStr = transportLayer.receiveMessage();
            long fileSize = Long.parseLong(fileSizeStr);
            transportLayer.transmitMessage("SIZE_CONFIRMED");

            // Receive file data
            byte[] fileData = transportLayer.receiveFileData();
            Path filePath = Paths.get("server_storage", fileName);
            Files.write(filePath, fileData);

            // Confirm upload success
            transportLayer.transmitMessage("UPLOAD_SUCCESSFUL");
            System.out.println("File '" + fileName + "' received and stored.");
        } catch (IOException | NumberFormatException e) {
            System.err.println("File upload encountered an error: " + e.getMessage());
            try {
                transportLayer.transmitMessage("ERROR: File upload failed.");
            } catch (IOException ignored) {}
        }
    }


    private void executeGet(String fileName, DataTransport transportLayer) {
        byte[] fileData = fetchFileFromCache(fileName);
        String sourceLocation = fileData != null ? "cache" : null;

        if (fileData != null) {
            System.out.println("File retrieved from cache.");
        } else {
            Path serverFilePath = Paths.get("server_storage", fileName);
            if (Files.exists(serverFilePath)) {
                try {
                    fileData = Files.readAllBytes(serverFilePath);
                    sourceLocation = "server";
                    System.out.println("File retrieved from server.");
                    storeInCache(fileName, fileData);
                } catch (IOException e) {
                    System.err.println("Error reading file from server storage.");
                }
            } else {
                System.out.println("File not found on server: " + fileName);
            }
        }

        try {
            if (fileData != null) {
                transportLayer.transmitMessage("READY");
                transportLayer.transmitMessage(sourceLocation);
                transportLayer.transmitFile(fileData);
            } else {
                transportLayer.transmitMessage("ERROR: File '" + fileName + "' not found.");
            }
        } catch (IOException e) {
            System.err.println("Error during file transmission.");
        }
    }

    private byte[] fetchFileFromCache(String fileName) {
        try (Socket cacheSocket = new Socket(cacheServerIp, cacheServerPort);
             DataInputStream cacheDataIn = new DataInputStream(new BufferedInputStream(cacheSocket.getInputStream()));
             DataOutputStream cacheDataOut = new DataOutputStream(new BufferedOutputStream(cacheSocket.getOutputStream()))) {

            cacheDataOut.writeUTF("GET " + fileName);
            cacheDataOut.flush();
            String cacheResponse = cacheDataIn.readUTF();
            if ("FOUND".equals(cacheResponse)) {
                int dataSize = cacheDataIn.readInt();
                byte[] fileData = new byte[dataSize];
                cacheDataIn.readFully(fileData);
                return fileData;
            } else {
                System.out.println("File not located in cache: " + fileName);
                return null;
            }
        } catch (IOException e) {
            System.err.println("Cache server communication error.");
            return null;
        }
    }

    private void storeInCache(String fileName, byte[] fileData) {
        System.out.println("Saving file to cache: " + fileName);
        try (Socket cacheSocket = new Socket(cacheServerIp, cacheServerPort);
             DataInputStream cacheDataIn = new DataInputStream(new BufferedInputStream(cacheSocket.getInputStream()));
             DataOutputStream cacheDataOut = new DataOutputStream(new BufferedOutputStream(cacheSocket.getOutputStream()))) {

            cacheDataOut.writeUTF("STORE " + fileName);
            cacheDataOut.flush();
            cacheDataOut.writeInt(fileData.length);
            cacheDataOut.flush();
            cacheDataOut.write(fileData);
            cacheDataOut.flush();
            String cacheResponse = cacheDataIn.readUTF();
            if ("STORED".equals(cacheResponse)) {
                System.out.println("File '" + fileName + "' successfully stored in cache.");
            } else {
                System.err.println("Failed to store file '" + fileName + "' in cache.");
            }
        } catch (IOException e) {
            System.err.println("Error communicating with cache server.");
        }
    }
}
