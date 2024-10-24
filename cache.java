import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class cache {
    private final int port;
    private final String protocol;
    private final String serverIp;
    private final int serverPort;
    private final CacheManager cacheManager;
    private final ExecutorService executor;

    public cache(int port, String protocol, String serverIp, int serverPort) throws IOException {
        this.port = port;
        this.protocol = protocol;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
        this.cacheManager = new CacheManager("cache_files");
        this.executor = Executors.newCachedThreadPool();
    }

    public void start() throws IOException {
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("Cache service started on port " + port + " using protocol: " + protocol.toUpperCase());

        while (true) {
            try {
                Socket clientSocket = serverSocket.accept();
                executor.execute(new ClientHandler(clientSocket, cacheManager));
            } catch (IOException e) {
                System.err.println("Failed to accept client connection.");
            }
        }
    }

    public static void main(String[] args) {
        try {
            cache cacheService = getCacheInstance(args);
            cacheService.start();
        } catch (IOException e) {
            System.err.println("IO Exception occurred while starting the cache service.");
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number provided.");
            System.out.println("Usage: java CacheService [port] [tcp/snw] [server ip] [server port]");
        }
    }

    private static cache getCacheInstance(String[] args) throws IOException {
        int port = 5050;
        String cacheIp = "localhost";
        String protocol = "tcp";
        String serverIp = "localhost";
        int serverPort = 4040;

        if (args.length >= 1) {
            port = Integer.parseInt(args[0]);
        }
        if (args.length >= 2) {
            cacheIp = args[1];
        }
        if (args.length >= 3) {
            protocol = args[2];
        }
        if (args.length >= 4) {
            serverIp = args[3];
        }
        if (args.length >= 5) {
            serverPort = Integer.parseInt(args[4]);
        }

        return new cache(port, protocol, serverIp, serverPort);
    }
}
